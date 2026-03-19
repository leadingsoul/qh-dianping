package com.qhdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qhdp.constant.RedisKeyManage;
import com.qhdp.entity.Shop;
import com.qhdp.factory.BloomFilterHandlerFactory;
import com.qhdp.service.ShopService;
import com.qhdp.mapper.ShopMapper;
import com.qhdp.servicelocker.LockType;
import com.qhdp.utils.RedisKeyBuild;
import com.qhdp.utils.RedisUtils;
import com.qhdp.utils.ServiceLockTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.qhdp.constant.Constant.BLOOM_FILTER_HANDLER_SHOP;
import static com.qhdp.utils.RedisConstants.*;

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


    @Override
    public Shop queryShopById(Long id) {
        Shop shop = queryShopByIdPlus(id);
        if (Objects.isNull(shop)) {
            throw new RuntimeException("查询商铺不存在");
        }
        return shop;
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




