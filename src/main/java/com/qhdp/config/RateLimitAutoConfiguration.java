package com.qhdp.config;

import com.qhdp.constant.SeckillRateLimitConfigProperties;
import com.qhdp.handler.RedisRateLimitHandler;
import com.qhdp.ratelimit.core.SlidingRateLimitOperate;
import com.qhdp.ratelimit.core.TokenBucketRateLimitOperate;
import com.qhdp.ratelimit.extention.NoOpRateLimitEventListener;
import com.qhdp.ratelimit.extention.NoOpRateLimitPenaltyPolicy;
import com.qhdp.ratelimit.extention.ThresholdPenaltyPolicy;
import com.qhdp.ratelimit.extention.extentionInterface.RateLimitEventListener;
import com.qhdp.ratelimit.extention.extentionInterface.RateLimitPenaltyPolicy;
import com.qhdp.utils.RedisUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * &#064;description:  布隆过滤器 配置
 * &#064;author: phoenix
 **/
@Configuration
@EnableConfigurationProperties(SeckillRateLimitConfigProperties.class)
public class RateLimitAutoConfiguration {
    
    @Bean
    public SlidingRateLimitOperate slidingRateLimitOperate(RedisUtils redisUtils){
        return new SlidingRateLimitOperate(redisUtils);
    }
    
    @Bean
    public TokenBucketRateLimitOperate tokenBucketRateLimitOperate(RedisUtils redisUtils){
        return new TokenBucketRateLimitOperate(redisUtils);
    }

    @Bean
    public RateLimitEventListener rateLimitEventListener(){
        return new NoOpRateLimitEventListener();
    }

    @Bean
    public RateLimitPenaltyPolicy rateLimitPenaltyPolicy(SeckillRateLimitConfigProperties seckillRateLimitConfigProperties,
                                                         RedisUtils redisUtils){
        
        Boolean enable = seckillRateLimitConfigProperties.getEnablePenalty();
        if (Boolean.TRUE.equals(enable)) {
            return new ThresholdPenaltyPolicy(redisUtils, seckillRateLimitConfigProperties);
        }
        return new NoOpRateLimitPenaltyPolicy();
    }

    @Bean
    public RedisRateLimitHandler redisRateLimitHandler(SeckillRateLimitConfigProperties seckillRateLimitConfigProperties,
                                                       RedisUtils redisUtils,
                                                       SlidingRateLimitOperate slidingRateLimitOperate,
                                                       TokenBucketRateLimitOperate tokenBucketRateLimitOperate,
                                                       RateLimitEventListener rateLimitEventListener,
                                                       RateLimitPenaltyPolicy rateLimitPenaltyPolicy) {
        return new RedisRateLimitHandler(
                seckillRateLimitConfigProperties, 
                redisUtils,
                slidingRateLimitOperate,
                tokenBucketRateLimitOperate,
                rateLimitEventListener,
                rateLimitPenaltyPolicy
        );
    }
}
