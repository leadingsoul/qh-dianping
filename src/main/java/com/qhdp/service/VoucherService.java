package com.qhdp.service;

import com.qhdp.dto.*;
import com.qhdp.entity.Voucher;
import com.baomidou.mybatisplus.extension.service.IService;
import com.qhdp.vo.GetSubscribeStatusVO;
import jakarta.validation.Valid;

import java.util.List;

/**
* @author phoenix
* @description 针对表【tb_voucher】的数据库操作Service
* @createDate 2026-03-11 14:33:43
*/
public interface VoucherService extends IService<Voucher> {



    Long addSeckillVoucher(SeckillVoucherDTO voucherDTO);

    List<Voucher> queryVoucherOfShop(Long shopId);

    Long addVoucher(VoucherDTO voucherDTO);

    void updateSeckillVoucher(@Valid UpdateSeckillVoucherDTO updateSeckillVoucherDTO);

    void updateSeckillVoucherStock(@Valid UpdateSeckillVoucherStockDTO updateSeckillVoucherStockDTO);

    void subscribe(@Valid VoucherSubscribeDTO voucherSubscribeDTO);

    void unsubscribe(@Valid VoucherSubscribeDTO voucherSubscribeDTO);

    String getSubscribeStatus(@Valid VoucherSubscribeDTO voucherSubscribeDTO);

    List<GetSubscribeStatusVO> getSubscribeStatusBatch(@Valid VoucherSubscribeBatchDTO voucherSubscribeBatchDTO);

    void delayVoucherReminder(@Valid DelayVoucherReminderDTO delayVoucherReminderDTO);
}
