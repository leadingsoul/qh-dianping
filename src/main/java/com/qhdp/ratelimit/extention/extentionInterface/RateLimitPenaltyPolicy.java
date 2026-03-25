package com.qhdp.ratelimit.extention.extentionInterface;


import com.qhdp.constant.RateLimitContext;
import com.qhdp.enums.BaseCode;

/**
 * @description: 惩罚策略扩展点：在命中限流后可执行封禁、打标、告警等动作。
 * @author: phoenix
 **/
public interface RateLimitPenaltyPolicy {

    /**
     * 应用惩罚策略
     * @param ctx    当前限流上下文
     * @param reason 命中原因（IP/USER 限流等）
     */
    void apply(RateLimitContext ctx, BaseCode reason);
}