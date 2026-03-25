package com.qhdp.vo;

import lombok.Data;

/**
 * &#064;description:  redis 中的记录日志信息
 * &#064;author:  phoenix
 **/
@Data
public class RedisTraceLogVO {

    private String logType;
    
    private Long ts;
    
    private String orderId;
    
    private String traceId;
    
    private String userId;
    
    private String voucherId;
    
    private Integer beforeQty;
    
    private Integer changeQty;
    
    private Integer afterQty;
}
