package com.qhdp.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qhdp.enums.RedisKeyManage;
import com.qhdp.entity.SeckillVoucher;
import com.qhdp.entity.Voucher;
import com.qhdp.factory.BloomFilterHandlerFactory;
import com.qhdp.mapper.VoucherMapper;
import com.qhdp.service.SeckillVoucherService;
import com.qhdp.mapper.SeckillVoucherMapper;
import com.qhdp.servicelocker.LockType;
import com.qhdp.servicelocker.annotation.ServiceLock;
import com.qhdp.utils.RedisKeyBuild;
import com.qhdp.utils.RedisUtils;
import com.qhdp.utils.SeckillVoucherCaffeineUtils;
import com.qhdp.utils.ServiceLockTool;
import com.qhdp.vo.SeckillVoucherFullVO;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.qhdp.constant.Constant.BLOOM_FILTER_HANDLER_VOUCHER;
import static com.qhdp.constant.DistributedLockConstants.UPDATE_SECKILL_VOUCHER_LOCK;
import static com.qhdp.constant.DistributedLockConstants.UPDATE_SECKILL_VOUCHER_STOCK_LOCK;
import static com.qhdp.constant.RedisConstants.*;

/**
* @author phoenix
* @description 针对表【tb_seckill_voucher(秒杀优惠券表，与优惠券是一对一关系)】的数据库操作Service实现
* @createDate 2026-03-11 14:32:01
*/
@RequiredArgsConstructor
@Service
@Slf4j
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher>
    implements SeckillVoucherService{

    private final RedisUtils redisUtils;

    private final BloomFilterHandlerFactory bloomFilterHandlerFactory;

    private final ServiceLockTool serviceLockTool;

    private final VoucherMapper voucherMapper;

    private final SeckillVoucherCaffeineUtils seckillVoucherCaffeineUtils;

    @Override
    @ServiceLock(lockType= LockType.Read,name = UPDATE_SECKILL_VOUCHER_LOCK,keys = {"#voucherId"})
    public SeckillVoucherFullVO queryByVoucherId(@NotNull Long voucherId) {
        String seckillVoucherRedisKey =
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_TAG_KEY, voucherId);
        String seckillVoucherNullRedisKey =
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_NULL_TAG_KEY, voucherId);
        SeckillVoucherFullVO localCacheHit = seckillVoucherCaffeineUtils.get(seckillVoucherRedisKey);
        if (Objects.nonNull(localCacheHit)) {
            return localCacheHit;
        }
        SeckillVoucherFullVO seckillVoucherFullModel =
                redisUtils.get(seckillVoucherRedisKey, SeckillVoucherFullVO.class);
        if (Objects.nonNull(seckillVoucherFullModel)) {
            seckillVoucherCaffeineUtils.put(seckillVoucherRedisKey, seckillVoucherFullModel);
            return seckillVoucherFullModel;
        }
        log.info("查询秒杀优惠券 从Redis缓存没有查询到 秒杀优惠券的优惠券id : {}",voucherId);
        if (!bloomFilterHandlerFactory.get(BLOOM_FILTER_HANDLER_VOUCHER).contains(String.valueOf(voucherId))) {
            log.info("查询秒杀优惠券 布隆过滤器判断不存在 秒杀优惠券id : {}",voucherId);
            throw new RuntimeException("查询秒杀优惠券不存在");
        }
        Boolean existResult = redisUtils.hasKey(seckillVoucherNullRedisKey);
        if (existResult){
            throw new RuntimeException("查询秒杀优惠券不存在");
        }
        RLock lock = serviceLockTool.getLock(LockType.Reentrant, LOCK_SECKILL_VOUCHER_KEY, new String[]{String.valueOf(voucherId)});
        lock.lock();
        try {
            seckillVoucherFullModel = redisUtils.get(seckillVoucherRedisKey, SeckillVoucherFullVO.class);
            if (Objects.nonNull(seckillVoucherFullModel)) {
                seckillVoucherCaffeineUtils.put(seckillVoucherRedisKey, seckillVoucherFullModel);
                return seckillVoucherFullModel;
            }
            existResult = redisUtils.hasKey(seckillVoucherNullRedisKey);
            if (existResult){
                throw new RuntimeException("查询优惠券不存在");
            }
            SeckillVoucher seckillVoucher = lambdaQuery().eq(SeckillVoucher::getVoucherId,voucherId).one();
            if (Objects.isNull(seckillVoucher)) {
                redisUtils.set(seckillVoucherNullRedisKey,
                        "这是一个空值",
                        CACHE_NULL_TTL,
                        TimeUnit.MINUTES);
                throw new RuntimeException("查询秒杀优惠券不存在");
            }
            long ttlSeconds = Math.max(
                    DateUtil.betweenMs(DateUtil.date(), seckillVoucher.getEndTime())/1000,
                    1L
            );
            Voucher voucher = voucherMapper.selectById(voucherId);
            seckillVoucherFullModel = new SeckillVoucherFullVO();
            BeanUtils.copyProperties(seckillVoucher, seckillVoucherFullModel);
            seckillVoucherFullModel.setShopId(voucher.getShopId());
            seckillVoucherFullModel.setStatus(voucher.getStatus());
            seckillVoucherFullModel.setStock(null);
            redisUtils.set(
                    seckillVoucherRedisKey,
                    seckillVoucherFullModel,
                    ttlSeconds,
                    TimeUnit.SECONDS
            );
            seckillVoucherCaffeineUtils.put(seckillVoucherRedisKey, seckillVoucherFullModel);
            return seckillVoucherFullModel;
        }finally {
            lock.unlock();
        }
    }

    @Override
    @ServiceLock(lockType= LockType.Read,name = UPDATE_SECKILL_VOUCHER_STOCK_LOCK,keys = {"#voucherId"})
    public void loadVoucherStock(Long voucherId){
        if (!bloomFilterHandlerFactory.get(BLOOM_FILTER_HANDLER_VOUCHER).contains(String.valueOf(voucherId))) {
            log.info("加载库存 布隆过滤器判断不存在 秒杀优惠券id : {}",voucherId);
            throw new RuntimeException("查询秒杀优惠券不存在");
        }
        String stock =
                redisUtils.get(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_STOCK_TAG_KEY, voucherId), String.class);
        if (Objects.nonNull(stock)) {
            return;
        }
        RLock lock = serviceLockTool.getLock(LockType.Reentrant, LOCK_SECKILL_VOUCHER_STOCK_KEY,
                new String[]{String.valueOf(voucherId)});
        lock.lock();
        try {
            stock = redisUtils.get(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_STOCK_TAG_KEY, voucherId), String.class);
            if (Objects.nonNull(stock)) {
                return;
            }
            SeckillVoucher seckillVoucher = lambdaQuery().eq(SeckillVoucher::getVoucherId,voucherId).one();
            if (Objects.nonNull(seckillVoucher)) {
                long ttlSeconds = Math.max(
                        DateUtil.betweenMs(DateUtil.date(), seckillVoucher.getEndTime())/1000,
                        1L
                );
                redisUtils.set(
                        RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_STOCK_TAG_KEY, voucherId),
                        String.valueOf(seckillVoucher.getStock()),
                        ttlSeconds,
                        TimeUnit.SECONDS
                );
            }
        }finally {
            lock.unlock();
        }
    }
}




