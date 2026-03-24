package com.qhdp.kafka.lua;

import lombok.Data;

/**
 * @description: lua秒杀返回数据
 * @author: phoenix
 **/
@Data
public class SeckillVoucherDomain {

    private Integer code;
    
    private Integer beforeQty;
    
    private Integer deductQty;
    
    private Integer afterQty;

}
