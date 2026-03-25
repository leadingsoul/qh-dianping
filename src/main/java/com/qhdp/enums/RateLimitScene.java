package com.qhdp.enums;

/**
 * &#064;description:  限流场景
 * &#064;author: phoenix
 **/
public enum RateLimitScene {
    /** 发令牌接口 */
    ISSUE_TOKEN,
    /** 下单（秒杀）接口 */
    SECKILL_ORDER
}