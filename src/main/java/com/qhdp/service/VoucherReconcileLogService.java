package com.qhdp.service;

import com.qhdp.dto.VoucherReconcileLogDTO;
import com.qhdp.entity.VoucherReconcileLog;
import com.baomidou.mybatisplus.extension.service.IService;
import com.qhdp.kafka.message.MessageExtend;
import com.qhdp.kafka.message.SeckillVoucherMessage;
import org.springframework.transaction.annotation.Transactional;

/**
* @author phoenix
* @description 针对表【tb_voucher_reconcile_log】的数据库操作Service
* @createDate 2026-03-11 14:34:12
*/
public interface VoucherReconcileLogService extends IService<VoucherReconcileLog> {

    @Transactional(rollbackFor = Exception.class)
    boolean saveReconcileLog(Integer logType, Integer businessType, String detail, MessageExtend<SeckillVoucherMessage> message);

    @Transactional(rollbackFor = Exception.class)
    boolean saveReconcileLog(Integer logType, Integer businessType, String detail, Long traceId, MessageExtend<SeckillVoucherMessage> message);

    boolean saveReconcileLog(VoucherReconcileLogDTO voucherReconcileLogDTO);
}
