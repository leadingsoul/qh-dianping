package com.qhdp.controller;


import com.qhdp.dto.CancelVoucherOrderDTO;
import com.qhdp.dto.GetVoucherOrderByVoucherIdDTO;
import com.qhdp.dto.GetVoucherOrderDTO;
import com.qhdp.dto.Result;
import com.qhdp.enums.RateLimitScene;
import com.qhdp.handler.handlerInterface.RateLimitHandler;
import com.qhdp.service.ReconciliationTaskService;
import com.qhdp.service.SeckillAccessTokenService;
import com.qhdp.service.VoucherOrderService;
import com.qhdp.utils.UserHolder;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
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

    private final VoucherOrderService voucherOrderService;
    @Resource
    private RateLimitHandler rateLimitHandler;
    private final SeckillAccessTokenService accessTokenService;
    private final ReconciliationTaskService reconciliationTaskService;

    @PostMapping("seckill/{id}")
    public Result<Long> seckillVoucher(@PathVariable("id") Long voucherId,
                                       @RequestParam(name = "accessToken", required = false) String accessToken) {
        Long userId = UserHolder.getUser().getId();
        rateLimitHandler.execute(voucherId, userId, RateLimitScene.SECKILL_ORDER);
        if (accessTokenService.isEnabled()) {
            if (accessToken == null || !accessTokenService.validateAndConsume(voucherId, userId, accessToken)) {
                return Result.error("令牌校验失败或令牌已失效");
            }
        }
        return Result.success(voucherOrderService.seckillVoucher(voucherId), "秒杀成功");
    }

    @PostMapping("/get/seckill/voucher/order-id")
    public Result<Long> getSeckillVoucherOrder(@Valid @RequestBody GetVoucherOrderDTO getVoucherOrderDTO) {
        return Result.success(voucherOrderService.getSeckillVoucherOrder(getVoucherOrderDTO), "查询成功");
    }

    @GetMapping("/seckill/token/{id}")
    public Result<String> issueSeckillAccessToken(@PathVariable("id") Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        rateLimitHandler.execute(voucherId, userId, RateLimitScene.ISSUE_TOKEN);
        String token = accessTokenService.issueAccessToken(voucherId, userId);
        return Result.success(token, "获取成功");
    }


    /**
     * 查询用户是否已购买优惠券
     * 接口路径：/voucher-order/get/seckill/voucher/order-id/by/voucher-id
     * 请求方式：POST
     * 参数：voucherId（请求体）
     */
    @PostMapping("/get/seckill/voucher/order-id/by/voucher-id")
    public Result<Long> getSeckillVoucherOrderIdByVoucherId(@Valid @RequestBody GetVoucherOrderByVoucherIdDTO getVoucherOrderByVoucherIdDTO) {
        return Result.success(voucherOrderService.getSeckillVoucherOrderIdByVoucherId(getVoucherOrderByVoucherIdDTO), "查询成功");
    }

    @PostMapping("/cancel")
    public Result<Boolean> cancel(@Valid @RequestBody CancelVoucherOrderDTO cancelVoucherOrderDTO) {
        return Result.success(voucherOrderService.cancel(cancelVoucherOrderDTO), "取消成功");
    }

    @PostMapping(value = "/reconciliation/task/all")
    public Result<Void> reconciliationTaskAll() {
        reconciliationTaskService.reconciliationTaskExecute();
        return Result.success(null);
    }

}
