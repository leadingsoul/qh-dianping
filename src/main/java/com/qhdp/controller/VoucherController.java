package com.qhdp.controller;


import com.qhdp.dto.Result;
import com.qhdp.dto.SeckillVoucherDTO;
import com.qhdp.dto.VoucherDTO;
import com.qhdp.entity.Voucher;
import com.qhdp.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * <p>
 *  前端控制器
 * </p>
 *
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/voucher")
public class VoucherController {

    private final VoucherService voucherService;

    /**
     * 新增普通券
     * @param voucherDto 优惠券信息
     * @return 优惠券id
     */
    @PostMapping
    public Result<Long> addVoucher(@Valid @RequestBody VoucherDTO voucherDTO) {
        final Long voucherId = voucherService.addVoucher(voucherDTO);
        return Result.success(voucherId);
    }

    /**
     * 新增秒杀券
     * @param seckillVoucherDTO 优惠券信息，包含秒杀信息
     * @return 优惠券id
     */
    @PostMapping("/seckill")
    public Result<Long> addSeckillVoucher(@Valid @RequestBody SeckillVoucherDTO seckillVoucherDTO) {
        final Long voucherId = voucherService.addSeckillVoucher(seckillVoucherDTO);
        return Result.success(voucherId);
    }

    /**
     * 查询店铺的优惠券列表
     * @param shopId 店铺id
     * @return 优惠券列表
     */
    @GetMapping("/list/{shopId}")
    public Result<List<Voucher>> queryVoucherOfShop(@PathVariable("shopId") Long shopId) {
        return Result.success(voucherService.queryVoucherOfShop(shopId));
    }
}
