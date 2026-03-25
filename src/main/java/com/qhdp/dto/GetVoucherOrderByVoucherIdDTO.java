package com.qhdp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * @description: 通过优惠券id获取优惠券订单
 * @author: phoenix
 **/
@Data
@EqualsAndHashCode(callSuper = false)
public class GetVoucherOrderByVoucherIdDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * 优惠券id
     */
    @NotNull
    private Long voucherId;

}
