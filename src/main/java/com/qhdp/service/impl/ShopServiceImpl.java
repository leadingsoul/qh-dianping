package com.qhdp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qhdp.constant.RedisConstants;
import com.qhdp.constant.RedisKeyManage;
import com.qhdp.entity.Shop;
import com.qhdp.factory.BloomFilterHandlerFactory;
import com.qhdp.service.ShopService;
import com.qhdp.mapper.ShopMapper;
import com.qhdp.servicelocker.LockType;
import com.qhdp.toolkit.SnowflakeIdGenerator;
import com.qhdp.utils.RedisKeyBuild;
import com.qhdp.utils.RedisUtils;
import com.qhdp.utils.ServiceLockTool;
import com.qhdp.utils.SystemConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.qhdp.constant.Constant.BLOOM_FILTER_HANDLER_SHOP;
import static com.qhdp.constant.RedisConstants.SHOP_GEO_KEY;
import static com.qhdp.constant.RedisConstants.*;

/**
* @author phoenix
* @description 针对表【tb_shop】的数据库操作Service实现
* @createDate 2026-03-11 14:32:16
*/
@RequiredArgsConstructor
@Service
@Slf4j
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop>
    implements ShopService{

    private final RedisUtils redisUtils;

    private final BloomFilterHandlerFactory bloomFilterHandlerFactory;

    private final ServiceLockTool serviceLockTool;

    private final SnowflakeIdGenerator snowflakeIdGenerator;


    @Override
    public Shop queryShopById(Long id) {
        Shop shop = queryShopByIdPlus(id);
        if (Objects.isNull(shop)) {
            throw new RuntimeException("查询商铺不存在");
        }
        return shop;
    }

    @Override
    public Long saveShop(Shop shop) {
        // 写入数据库
        shop.setId(snowflakeIdGenerator.nextId());
        save(shop);
        // 写入布隆过滤器（商铺业务）
        bloomFilterHandlerFactory.get(BLOOM_FILTER_HANDLER_SHOP).add(String.valueOf(shop.getId()));
        // 返回店铺id
        return shop.getId();
    }

    @Override
    public void updateShopById(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            throw new RuntimeException("店铺id不能为空");
        }
        // 1.更新数据库
        updateById(shop);
        // 2.删除缓存
        redisUtils.delete(RedisKeyBuild.createRedisKey(RedisKeyManage.CACHE_SHOP_KEY, id));
    }

    @Override
    public Page<Shop> queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        // 1.判断是否需要根据坐标查询
        x = null;
        y = null;
        if (x == null || y == null) {
            // 不需要坐标查询，按数据库查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return page;
        }

        // 2.计算分页参数
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;

        // 3.查询redis、按照距离排序、分页。结果：shopId、distance
        String key = SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<Object>> results = redisUtils.geoSearch(key, x, y, 5000, end);
        // 4.解析出id
        if (results == null) {
            Page<Shop> emptyPage = new Page<>();
            emptyPage.setRecords(Collections.emptyList());
            emptyPage.setCurrent(current);
            emptyPage.setSize(SystemConstants.DEFAULT_PAGE_SIZE);
            emptyPage.setTotal(0);
            return emptyPage;
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<Object>>> list = results.getContent();
        if (list.size() <= from) {
            // 没有下一页了，结束
            Page<Shop> emptyPage = new Page<>();
            emptyPage.setRecords(Collections.emptyList());
            emptyPage.setCurrent(current);
            emptyPage.setSize(SystemConstants.DEFAULT_PAGE_SIZE);
            emptyPage.setTotal(0);
            return emptyPage;
        }
        // 4.1.截取 from ~ end的部分
        List<Long> ids = new ArrayList<>(list.size());
        Map<String, Distance> distanceMap = new HashMap<>(list.size());
        list.stream().skip(from).forEach(result -> {
            // 4.2.获取店铺id
            String shopIdStr = result.getContent().getName().toString();
            ids.add(Long.valueOf(shopIdStr));
            // 4.3.获取距离
            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr, distance);
        });
        // 5.根据id查询Shop
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        // 6.构造并返回Page对象
        Page<Shop> page = new Page<>();
        page.setRecords(shops);
        page.setCurrent(current);
        page.setSize(SystemConstants.DEFAULT_PAGE_SIZE);
        // For geo-search, we estimate total based on available results
        page.setTotal((long) list.size()); // Set estimated total
        return page;
    }

    private Shop queryShopByIdPlus(Long id) {
        Shop shop =
                redisUtils.get(RedisKeyBuild.createRedisKey(RedisKeyManage.CACHE_SHOP_KEY, id), Shop.class);
        if (Objects.nonNull(shop)) {
            return shop;
        }
        log.info("查询商铺 从Redis缓存没有查询到 商铺id : {}",id);
        if (!bloomFilterHandlerFactory.get(BLOOM_FILTER_HANDLER_SHOP).contains(String.valueOf(id))) {
            log.info("查询商铺 布隆过滤器判断不存在 商铺id : {}",id);
            throw new RuntimeException("查询商铺不存在");
        }
        Boolean existResult = redisUtils.hasKey(RedisKeyBuild.createRedisKey(RedisKeyManage.CACHE_SHOP_KEY_NULL, id));
        if (existResult){
            throw new RuntimeException("查询商铺不存在");
        }
        RLock lock = serviceLockTool.getLock(LockType.Reentrant, LOCK_SHOP_KEY, new String[]{String.valueOf(id)});
        lock.lock();
        try {
            existResult = redisUtils.hasKey(RedisKeyBuild.createRedisKey(RedisKeyManage.CACHE_SHOP_KEY_NULL, id));
            if (existResult){
                throw new RuntimeException("查询商铺不存在");
            }
            shop = redisUtils.get(RedisKeyBuild.createRedisKey(RedisKeyManage.CACHE_SHOP_KEY, id), Shop.class);
            if (Objects.nonNull(shop)) {
                return shop;
            }
            shop = getById(id);
            if (Objects.isNull(shop)) {
                redisUtils.set(RedisKeyBuild.createRedisKey(RedisKeyManage.CACHE_SHOP_KEY_NULL, id),
                        "这是一个空值",
                        CACHE_SHOP_TTL,
                        TimeUnit.MINUTES);
                throw new RuntimeException("查询商铺不存在");
            }
            redisUtils.set(RedisKeyBuild.createRedisKey(RedisKeyManage.CACHE_SHOP_KEY, id),shop,
                    CACHE_SHOP_TTL,
                    TimeUnit.MINUTES);
            return shop;
        }finally {
            lock.unlock();
        }
    }
}