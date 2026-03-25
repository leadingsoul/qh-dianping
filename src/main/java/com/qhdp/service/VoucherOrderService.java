package com.qhdp.service;

import com.qhdp.dto.CancelVoucherOrderDTO;
import com.qhdp.dto.GetVoucherOrderByVoucherIdDTO;
import com.qhdp.dto.GetVoucherOrderDTO;
import com.qhdp.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;
import com.qhdp.kafka.message.MessageExtend;
import com.qhdp.kafka.message.SeckillVoucherMessage;
import jakarta.validation.Valid;

/**
* @author phoenix
* @description 针对表【tb_voucher_order】的数据库操作Service
* @createDate 2026-03-11 14:33:53
*/
public interface VoucherOrderService extends IService<VoucherOrder> {

    boolean autoIssueVoucherToEarliestSubscriber(Long voucherId, Long excludeUserId);

    Long seckillVoucher(Long voucherId);

    Long getSeckillVoucherOrder(@Valid GetVoucherOrderDTO getVoucherOrderDTO);

    Long getSeckillVoucherOrderIdByVoucherId(@Valid GetVoucherOrderByVoucherIdDTO getVoucherOrderByVoucherIdDTO);

    Boolean cancel(@Valid CancelVoucherOrderDTO cancelVoucherOrderDTO);

    boolean createVoucherOrderPlus(MessageExtend<SeckillVoucherMessage> message);
}
