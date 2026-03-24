package com.qhdp.service;

import com.qhdp.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author phoenix
* @description 针对表【tb_voucher_order】的数据库操作Service
* @createDate 2026-03-11 14:33:53
*/
public interface VoucherOrderService extends IService<VoucherOrder> {

    boolean autoIssueVoucherToEarliestSubscriber(Long voucherId, Long excludeUserId);
}
