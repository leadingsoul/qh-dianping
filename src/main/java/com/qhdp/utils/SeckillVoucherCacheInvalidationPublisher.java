package com.qhdp.utils;

import com.qhdp.enums.RedisKeyManage;
import com.qhdp.kafka.message.SeckillVoucherInvalidationMessage;
import com.qhdp.kafka.producer.SeckillVoucherInvalidationProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.qhdp.constant.Constant.SECKILL_VOUCHER_CACHE_INVALIDATION_TOPIC;


/**
 * @description: 业务发布入口：触发秒杀券缓存失效广播
 * @author: phoenix
 **/
@Component
@RequiredArgsConstructor
public class SeckillVoucherCacheInvalidationPublisher {

    private final RedisUtils redisUtils;

    private final SeckillVoucherInvalidationProducer invalidationProducer;

    private final SeckillVoucherCaffeineUtils seckillVoucherCaffeineUtils;
    
    public void publishInvalidate(Long voucherId, String reason) {
        String seckillVoucherRedisKey =
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_TAG_KEY, voucherId);
        seckillVoucherCaffeineUtils.invalidate(seckillVoucherRedisKey);
        redisUtils.delete(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_TAG_KEY, voucherId));
        redisUtils.delete(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_STOCK_TAG_KEY, voucherId));
        redisUtils.delete(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_NULL_TAG_KEY, voucherId));
        
        SeckillVoucherInvalidationMessage payload = new SeckillVoucherInvalidationMessage(voucherId, reason);
        invalidationProducer.sendPayload(
                SpringUtil.getPrefixDistinctionName() + "-" + SECKILL_VOUCHER_CACHE_INVALIDATION_TOPIC,
                payload
        );
    }
}