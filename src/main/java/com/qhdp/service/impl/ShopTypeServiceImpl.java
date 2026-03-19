package com.qhdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qhdp.constant.Constant;
import com.qhdp.constant.RedisKeyManage;
import com.qhdp.entity.ShopType;
import com.qhdp.service.ShopTypeService;
import com.qhdp.mapper.ShopTypeMapper;
import com.qhdp.servicelocker.LockType;
import com.qhdp.utils.RedisKeyBuild;
import com.qhdp.utils.RedisUtils;
import com.qhdp.utils.ServiceLockTool;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.qhdp.constant.RedisConstants.LOCK_SHOP_KEY;
import static com.qhdp.constant.RedisConstants.LOCK_SHOP_TYPE_LIST_KEY;

/**
* @author phoenix
* @description 针对表【tb_shop_type】的数据库操作Service实现
* @createDate 2026-03-11 14:32:28
*/
@RequiredArgsConstructor
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType>
    implements ShopTypeService{

    private final RedisUtils redisUtils;

    private final ServiceLockTool serviceLockTool;

    @Override
    public List<ShopType> queryAllTypeList() {
        return queryAllTypeListPlus();
    }

    public List<ShopType> queryAllTypeListPlus() {
        List<ShopType> typeList = redisUtils.getList(RedisKeyBuild.createRedisKey(RedisKeyManage.CACHE_SHOP_TYPE_KEY), ShopType.class);
        RLock lock = serviceLockTool.getLock(LockType.Reentrant, LOCK_SHOP_TYPE_LIST_KEY, new String[]{"shopTypeList"});
        lock.lock();
        try{
            if (typeList == null || typeList.isEmpty()) {
                refreshShopTypeCache();
                typeList = redisUtils.getList(RedisKeyBuild.createRedisKey(RedisKeyManage.CACHE_SHOP_TYPE_KEY), ShopType.class);
            }
            return typeList;
        }finally {
            lock.unlock();
        }
    }
    /**
     * 定时任务：自动刷新 Redis（防过期、防雪崩）
     */
    @Scheduled(cron = Constant.SCHEDULE_TASK_TIME)
    public void autoRefreshCache() {
        refreshShopTypeCache();
    }
    /**
     * 手动刷新：修改店铺类型后，调用此方法实时更新缓存
     */
    @Override
    public void refreshShopTypeCache() {
        // 查数据库
        List<ShopType> typeList = list(new LambdaQueryWrapper<ShopType>()
                .orderByAsc(ShopType::getSort));
        redisUtils.set(RedisKeyBuild.createRedisKey(RedisKeyManage.CACHE_SHOP_TYPE_KEY), typeList);
    }

}




