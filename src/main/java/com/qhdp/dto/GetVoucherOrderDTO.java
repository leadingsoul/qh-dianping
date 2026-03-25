package com.qhdp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * &#064;description:  获取优惠券订单
 * &#064;author:  phoenix
 **/
@Data
@EqualsAndHashCode(callSuper = false)
public class GetVoucherOrderDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单id
     */
    @NotNull
    private Long orderId;

}
