package com.qhdp.controller;


import com.qhdp.dto.*;
import com.qhdp.entity.Voucher;
import com.qhdp.service.SeckillVoucherService;
import com.qhdp.service.VoucherService;
import com.qhdp.vo.GetSubscribeStatusVO;
import com.qhdp.vo.SeckillVoucherFullVO;
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

    private final SeckillVoucherService seckillVoucherService;

    /**
     * 新增普通券
     * @param voucherDTO 优惠券信息
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

    /**
     * 根据优惠券ID获取秒杀优惠券信息
     *
     * @param getSeckillVoucherDTO 获取秒杀优惠券的DTO对象，包含voucherId参数
     * @return 返回一个Result对象，其中包含秒杀优惠券的详细信息
     */
    @PostMapping("/get")
    public Result<SeckillVoucherFullVO> get(@Valid @RequestBody GetSeckillVoucherDTO getSeckillVoucherDTO) { // 方法接收一个经过验证的请求体
        return Result.success(seckillVoucherService.queryByVoucherId(getSeckillVoucherDTO.getVoucherId())); // 调用服务层方法查询优惠券信息并返回成功结果
    }

    /**
     * 更新秒杀优惠券信息的接口
     * @param updateSeckillVoucherDTO 包含更新后秒杀优惠券信息的DTO对象
     * @return 返回操作结果，成功时返回秒杀更新成功的提示信息
     */
    @PostMapping("/update/seckill")
    public Result<Void> updateSeckillVoucher(@Valid @RequestBody UpdateSeckillVoucherDTO updateSeckillVoucherDTO) {
        voucherService.updateSeckillVoucher(updateSeckillVoucherDTO);
        return Result.success("秒杀更新成功");
    }

    /**
     * 更新秒杀活动库存的接口
     * @PostMapping 映射HTTP POST请求，路径为"/update/seckill/stock"
     * @param updateSeckillVoucherStockDTO 接收请求体中的数据，包含需要更新的秒杀券库存信息
     * @return 返回操作结果，成功时返回"秒杀库存更新成功"的消息
     */
    @PostMapping("/update/seckill/stock")
    public Result<Void> updateSeckillVoucherStock(@Valid @RequestBody UpdateSeckillVoucherStockDTO updateSeckillVoucherStockDTO) {
        voucherService.updateSeckillVoucherStock(updateSeckillVoucherStockDTO);
        return Result.success("秒杀库存更新成功");
    }

    /**
     * 处理优惠券订阅请求的接口方法
     *
     * @param voucherSubscribeDTO 优惠券订阅的数据传输对象，包含订阅所需的信息
     * @return 返回操作结果，成功时返回订阅成功的信息
     */
    @PostMapping("/subscribe")
    public Result<Void> subscribe(@Valid @RequestBody VoucherSubscribeDTO voucherSubscribeDTO){
        voucherService.subscribe(voucherSubscribeDTO);
        return Result.success("订阅成功");
    }

    /**
     * 取消订阅优惠券的接口方法
     *
     * @param voucherSubscribeDTO 包含取消订阅所需信息的DTO对象，使用@Valid进行参数校验
     * @return 返回操作结果，成功时返回"取消订阅成功"的消息
     */
    @PostMapping("/unsubscribe")
    public Result<Void> unsubscribe(@Valid @RequestBody VoucherSubscribeDTO voucherSubscribeDTO){
        voucherService.unsubscribe(voucherSubscribeDTO);
        return Result.success("取消订阅成功");
    }

    /**
     * 获取订阅状态接口
     * @param voucherSubscribeDTO 订阅信息数据传输对象，包含必要的订阅参数信息
     * @return 返回订阅状态结果，封装在Result对象中，数据类型为Integer
     */
    @PostMapping("/get/subscribe/status")
    public Result<Integer> getSubscribeStatus(@Valid @RequestBody VoucherSubscribeDTO voucherSubscribeDTO){
        return Result.success(voucherService.getSubscribeStatus(voucherSubscribeDTO));
    }

    /**
     * 批量获取订阅状态接口
     * @param voucherSubscribeBatchDTO 订阅批量查询请求参数
     * @return 返回订阅状态列表结果
     */
    @PostMapping("/get/subscribe/status/batch")
    public Result<List<GetSubscribeStatusVO>> getSubscribeStatusBatch(@Valid @RequestBody VoucherSubscribeBatchDTO voucherSubscribeBatchDTO){
        return Result.success(voucherService.getSubscribeStatusBatch(voucherSubscribeBatchDTO));
    }

    /**
     * 延迟秒杀提醒接口
     * @param delayVoucherReminderDTO 延迟秒杀提醒的数据传输对象，包含必要验证信息
     * @return 返回操作结果，成功时返回"秒杀提醒延迟成功"的提示信息
     */
    @PostMapping("/delay/voucher/reminder")
    public Result<Void> delayVoucherReminder(@Valid @RequestBody DelayVoucherReminderDTO delayVoucherReminderDTO){
        voucherService.delayVoucherReminder(delayVoucherReminderDTO);
        return Result.success("秒杀提醒延迟成功");
    }
}
