package com.qhdp.service.impl;

import cn.hutool.core.util.IdUtil;
import com.qhdp.enums.RedisKeyManage;
import com.qhdp.ratelimit.core.SeckillAccessTokenOperate;
import com.qhdp.service.SeckillAccessTokenService;
import com.qhdp.utils.RedisKeyBuild;
import com.qhdp.utils.RedisUtils;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * &#064;description:  令牌实现 接口
 * &#064;author:  phoenix
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillAccessTokenServiceImpl implements SeckillAccessTokenService {

    @Value("${seckill.access.token.enabled:true}")
    private boolean enabled;

    @Value("${seckill.access.token.ttl-seconds:30}")
    private long ttlSeconds;

    private final RedisUtils redisUtils;

    private final MeterRegistry meterRegistry;

    private SeckillAccessTokenOperate operate;

    @PostConstruct
    public void init() {
        operate = new SeckillAccessTokenOperate(redisUtils);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String issueAccessToken(Long voucherId, Long userId) {
        String token = IdUtil.simpleUUID();
        boolean ok = redisUtils.setIfAbsent(
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_ACCESS_TOKEN_TAG_KEY, voucherId, userId),
                token, 
                ttlSeconds, 
                TimeUnit.SECONDS);
        if (!ok) {
            String existing = redisUtils.get(
                    RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_ACCESS_TOKEN_TAG_KEY, voucherId, userId), 
                    String.class);
            safeInc("seckill_access_token_issue_conflict", "component", "service_impl");
            return existing != null ? existing : token;
        }
        safeInc("seckill_access_token_issue_success", "component", "service_impl");
        log.info("获取到令牌成功！令牌：{}", token);
        return token;
    }

    @Override
    public boolean validateAndConsume(Long voucherId, Long userId, String token) {
        String key = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_ACCESS_TOKEN_TAG_KEY, voucherId, userId);
        boolean success = operate.validateAndConsume(key, token);
        safeInc(success ? "seckill_access_token_consume_success" : "seckill_access_token_consume_fail",
                "component", "service_impl");
        return success;
    }

    private void safeInc(String name, String tagKey, String tagValue) {
        try {
            if (meterRegistry != null) {
                meterRegistry.counter(name, tagKey, tagValue).increment();
            }
        } catch (Exception ignore) {
        }
    }
}