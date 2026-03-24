package com.qhdp.service.impl;

import com.qhdp.entity.RollbackFailureLog;
import com.qhdp.enums.RedisKeyManage;
import com.qhdp.service.RollbackAlertService;
import com.qhdp.utils.RedisKeyBuild;
import com.qhdp.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @description: 回滚失败通知服务：用于发送短信/邮件告警（可插拔实现）。
 * @author: phoenix
 **/
@Slf4j
@RequiredArgsConstructor
@Service
public class RollbackAlertServiceImpl implements RollbackAlertService {

    @Value("${seckill.rollback.alert.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${seckill.rollback.alert.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${seckill.rollback.alert.sms.to:}")
    private String smsTo;

    @Value("${seckill.rollback.alert.email.to:}")
    private String emailTo;

    @Value("${seckill.rollback.alert.dedup.window.seconds:300}")
    private long dedupWindowSeconds;

    private final RedisUtils redisUtils;

    @Override
    public void sendRollbackAlert(RollbackFailureLog logEntity) {
        try {
            if (!shouldNotify(logEntity.getVoucherId())) {
                return;
            }
            String content = formatContent(logEntity);
            if (smsEnabled && smsTo != null && !smsTo.isEmpty()) {
                log.warn("[ROLLBACK_SMS] to={} content={} ", smsTo, content);
            }
            if (emailEnabled && emailTo != null && !emailTo.isEmpty()) {
                log.warn("[ROLLBACK_EMAIL] to={} content={} ", emailTo, content);
            }
        } catch (Exception e) {
            log.warn("发送回滚失败通知异常", e);
        }
    }

    private boolean shouldNotify(Long voucherId) {
        try {
            return redisUtils.setIfAbsent(
                    RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_ROLLBACK_ALERT_DEDUP_KEY,voucherId),
                    "1", 
                    dedupWindowSeconds, 
                    TimeUnit.SECONDS);
        } catch (Exception e) {
            return true;
        }
    }

    private String formatContent(RollbackFailureLog rollbackFailureLog) {
        String time = "";
        if (rollbackFailureLog.getCreateTime() != null) {
            Date createTime = rollbackFailureLog.getCreateTime();
            Instant instant = createTime.toInstant();
            var localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
            time = localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return String.format("回滚失败告警 | voucherId=%s userId=%s orderId=%s traceId=%s attempts=%s source=%s time=%s detail=%s", 
                rollbackFailureLog.getVoucherId(), 
                rollbackFailureLog.getUserId(), 
                rollbackFailureLog.getOrderId(), 
                rollbackFailureLog.getTraceId(), 
                rollbackFailureLog.getRetryAttempts(), 
                rollbackFailureLog.getSource(),
                time,
                rollbackFailureLog.getDetail());
    }
}