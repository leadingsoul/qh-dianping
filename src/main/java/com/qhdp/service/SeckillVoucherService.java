package com.qhdp.service;

import com.qhdp.entity.SeckillVoucher;
import com.baomidou.mybatisplus.extension.service.IService;
import com.qhdp.vo.SeckillVoucherFullVO;
import jakarta.validation.constraints.NotNull;

/**
* @author phoenix
* @description 针对表【tb_seckill_voucher(秒杀优惠券表，与优惠券是一对一关系)】的数据库操作Service
* @createDate 2026-03-11 14:32:01
*/
public interface SeckillVoucherService extends IService<SeckillVoucher> {

    /**
     * 根据优惠券ID查询秒杀优惠券完整信息
     *
     * @param voucherId 优惠券ID，不能为null
     * @return 返回秒杀优惠券的完整信息对象，包含优惠券的所有相关字段
     */
    SeckillVoucherFullVO queryByVoucherId(@NotNull Long voucherId);

    void loadVoucherStock(Long voucherId);
}
