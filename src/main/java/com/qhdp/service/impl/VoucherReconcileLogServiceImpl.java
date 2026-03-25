package com.qhdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qhdp.dto.VoucherReconcileLogDTO;
import com.qhdp.entity.VoucherReconcileLog;
import com.qhdp.enums.LogType;
import com.qhdp.kafka.message.MessageExtend;
import com.qhdp.kafka.message.SeckillVoucherMessage;
import com.qhdp.service.VoucherReconcileLogService;
import com.qhdp.mapper.VoucherReconcileLogMapper;
import com.qhdp.toolkit.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
* @author phoenix
* @description 针对表【tb_voucher_reconcile_log】的数据库操作Service实现
* @createDate 2026-03-11 14:34:12
*/
@Service
@RequiredArgsConstructor
public class VoucherReconcileLogServiceImpl extends ServiceImpl<VoucherReconcileLogMapper, VoucherReconcileLog>
    implements VoucherReconcileLogService{

    private final SnowflakeIdGenerator snowflakeIdGenerator;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveReconcileLog(final Integer logType, final Integer businessType, final String detail, final MessageExtend<SeckillVoucherMessage> message) {
        SeckillVoucherMessage messageBody = message.getMessageBody();
        VoucherReconcileLogDTO voucherReconcileLogDTO = new VoucherReconcileLogDTO();
        voucherReconcileLogDTO.setOrderId(messageBody.getOrderId());
        voucherReconcileLogDTO.setUserId(messageBody.getUserId());
        voucherReconcileLogDTO.setVoucherId(messageBody.getVoucherId());
        voucherReconcileLogDTO.setMessageId(message.getUuid());
        voucherReconcileLogDTO.setDetail(detail);
        voucherReconcileLogDTO.setBeforeQty(messageBody.getBeforeQty());
        voucherReconcileLogDTO.setChangeQty(messageBody.getChangeQty());
        voucherReconcileLogDTO.setAfterQty(messageBody.getAfterQty());
        voucherReconcileLogDTO.setTraceId(messageBody.getTraceId());
        voucherReconcileLogDTO.setLogType(logType);
        voucherReconcileLogDTO.setBusinessType(businessType);
        if (voucherReconcileLogDTO.getLogType().equals(LogType.RESTORE.getCode())) {
            voucherReconcileLogDTO.setBeforeQty(messageBody.getAfterQty());
            voucherReconcileLogDTO.setAfterQty(messageBody.getBeforeQty());
        }
        return saveReconcileLog(voucherReconcileLogDTO);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveReconcileLog(final Integer logType, final Integer businessType, final String detail, final Long traceId, final MessageExtend<SeckillVoucherMessage> message) {
        SeckillVoucherMessage messageBody = message.getMessageBody();
        VoucherReconcileLogDTO voucherReconcileLogDTO = new VoucherReconcileLogDTO();
        voucherReconcileLogDTO.setOrderId(messageBody.getOrderId());
        voucherReconcileLogDTO.setUserId(messageBody.getUserId());
        voucherReconcileLogDTO.setVoucherId(messageBody.getVoucherId());
        voucherReconcileLogDTO.setMessageId(message.getUuid());
        voucherReconcileLogDTO.setDetail(detail);
        voucherReconcileLogDTO.setBeforeQty(messageBody.getBeforeQty());
        voucherReconcileLogDTO.setChangeQty(messageBody.getChangeQty());
        voucherReconcileLogDTO.setAfterQty(messageBody.getAfterQty());
        voucherReconcileLogDTO.setTraceId(traceId);
        voucherReconcileLogDTO.setLogType(logType);
        voucherReconcileLogDTO.setBusinessType(businessType);
        if (voucherReconcileLogDTO.getLogType().equals(LogType.RESTORE.getCode())) {
            voucherReconcileLogDTO.setBeforeQty(messageBody.getAfterQty());
            voucherReconcileLogDTO.setAfterQty(messageBody.getBeforeQty());
        }
        return saveReconcileLog(voucherReconcileLogDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveReconcileLog(VoucherReconcileLogDTO voucherReconcileLogDTO) {
        VoucherReconcileLog logEntity = new VoucherReconcileLog();
        logEntity.setId(snowflakeIdGenerator.nextId());
        logEntity.setOrderId(voucherReconcileLogDTO.getOrderId());
        logEntity.setUserId(voucherReconcileLogDTO.getUserId());
        logEntity.setVoucherId(voucherReconcileLogDTO.getVoucherId());
        logEntity.setMessageId(voucherReconcileLogDTO.getMessageId());
        logEntity.setBusinessType(voucherReconcileLogDTO.getBusinessType());
        logEntity.setDetail(voucherReconcileLogDTO.getDetail());
        logEntity.setTraceId(voucherReconcileLogDTO.getTraceId());
        logEntity.setLogType(voucherReconcileLogDTO.getLogType());
        logEntity.setBeforeQty(voucherReconcileLogDTO.getBeforeQty());
        logEntity.setChangeQty(voucherReconcileLogDTO.getChangeQty());
        logEntity.setAfterQty(voucherReconcileLogDTO.getAfterQty());
        return save(logEntity);
    }
}




