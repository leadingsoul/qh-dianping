package com.qhdp.ratelimit.extention;

import com.qhdp.constant.RateLimitContext;
import com.qhdp.constant.SeckillRateLimitConfigProperties;
import com.qhdp.enums.BaseCode;
import com.qhdp.enums.RedisKeyManage;
import com.qhdp.ratelimit.extention.extentionInterface.RateLimitPenaltyPolicy;
import com.qhdp.utils.RedisKeyBuild;
import com.qhdp.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * &#064;description:  基于阈值的临时封禁
 * &#064;author:  phoenix
 **/
@Slf4j
public class ThresholdPenaltyPolicy implements RateLimitPenaltyPolicy {

    private final RedisUtils redisUtils;
    private final SeckillRateLimitConfigProperties props;

    public ThresholdPenaltyPolicy(RedisUtils redisUtils, SeckillRateLimitConfigProperties props) {
        this.redisUtils = redisUtils;
        this.props = props;
    }

    @Override
    public void apply(RateLimitContext context, BaseCode reason) {
        try {
            if (reason == BaseCode.SECKILL_RATE_LIMIT_IP_EXCEEDED) {
                applyForIp(context);
            } else if (reason == BaseCode.SECKILL_RATE_LIMIT_USER_EXCEEDED) {
                applyForUser(context);
            }
        } catch (Exception e) {
            log.debug("Penalty policy apply failed: {}", e.getMessage());
        }
    }

    private void applyForIp(RateLimitContext ctx) {
        Long voucherId = ctx.getVoucherId();
        String clientIp = ctx.getClientIp();
        if (Objects.isNull(voucherId) || Objects.isNull(clientIp)) {
            return;
        }
        String violationKey = RedisKeyBuild.createRedisKey(
                RedisKeyManage.SECKILL_VIOLATION_IP_TAG_KEY, voucherId, clientIp);
        long count = redisUtils.increment(violationKey, 1L);
        if (count == 1L) {
            redisUtils.expire(violationKey, props.getViolationWindowSeconds());
        }
        if (count >= props.getIpBlockThreshold()) {
            String blockKey = RedisKeyBuild.createRedisKey(
                    RedisKeyManage.SECKILL_BLOCK_IP_TAG_KEY, voucherId, clientIp);
            redisUtils.set(blockKey, "1", Long.valueOf(props.getIpBlockTtlSeconds()), TimeUnit.SECONDS);
            log.warn("Temporary banned IP: voucherId={}, ip={}, ttlSeconds={}, violationCount={}",
                    voucherId, clientIp, props.getIpBlockTtlSeconds(), count);
        }
    }

    private void applyForUser(RateLimitContext ctx) {
        Long voucherId = ctx.getVoucherId();
        Long userId = ctx.getUserId();
        if (Objects.isNull(voucherId) || Objects.isNull(userId)) {
            return;
        }
        String violationKey = RedisKeyBuild.createRedisKey(
                RedisKeyManage.SECKILL_VIOLATION_USER_TAG_KEY, voucherId, userId);
        long count = redisUtils.increment(violationKey, 1L);
        if (count == 1L) {
            redisUtils.expire(violationKey, props.getViolationWindowSeconds());
        }
        if (count >= props.getUserBlockThreshold()) {
            String blockKey = RedisKeyBuild.createRedisKey(
                    RedisKeyManage.SECKILL_BLOCK_USER_TAG_KEY, voucherId, userId);
            redisUtils.set(blockKey, "1", Long.valueOf(props.getUserBlockTtlSeconds()), TimeUnit.SECONDS);
            log.warn("Temporary banned user: voucherId={}, userId={}, ttlSeconds={}, violationCount={}",
                    voucherId, userId, props.getUserBlockTtlSeconds(), count);
        }
    }
}