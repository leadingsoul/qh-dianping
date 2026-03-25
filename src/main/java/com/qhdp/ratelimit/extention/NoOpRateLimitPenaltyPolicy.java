package com.qhdp.ratelimit.extention;


import com.qhdp.constant.RateLimitContext;
import com.qhdp.enums.BaseCode;
import com.qhdp.ratelimit.extention.extentionInterface.RateLimitPenaltyPolicy;

/**
 * &#064;description:  默认空实现
 * &#064;author:  phoenix
 **/
public class NoOpRateLimitPenaltyPolicy implements RateLimitPenaltyPolicy {
    @Override
    public void apply(RateLimitContext ctx, BaseCode reason) {
        // no-op
    }
}