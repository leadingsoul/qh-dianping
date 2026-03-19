package com.qhdp.controller;


import com.qhdp.dto.Result;
import com.qhdp.entity.SeckillVoucher;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/voucher-order")
public class VoucherOrderController {
    @PostMapping("seckill/{id}")
    public Result<SeckillVoucher> seckillVoucher(@PathVariable("id") Long voucherId) {
        return Result.error("功能未完成");
    }

    /**
     * 查询用户是否已购买优惠券
     * 接口路径：/voucher-order/get/seckill/voucher/order-id/by/voucher-id
     * 请求方式：POST
     * 参数：voucherId（请求体）
     */
    @PostMapping("/get/seckill/voucher/order-id/by/voucher-id")
    public Result getSeckillVoucherOrderByVoucherId(@RequestBody Long voucherId) {
        // 这里后面写你的业务逻辑：根据 voucherId + 当前用户 查询订单
        return Result.success("功能待实现，接口已匹配成功");
    }
}
