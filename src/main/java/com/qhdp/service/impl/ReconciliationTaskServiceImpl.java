package com.qhdp.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.qhdp.entity.SeckillVoucher;
import com.qhdp.entity.VoucherOrder;
import com.qhdp.entity.VoucherReconcileLog;
import com.qhdp.enums.ReconciliationStatus;
import com.qhdp.enums.RedisKeyManage;
import com.qhdp.service.ReconciliationTaskService;
import com.qhdp.service.SeckillVoucherService;
import com.qhdp.service.VoucherOrderService;
import com.qhdp.service.VoucherReconcileLogService;
import com.qhdp.servicelocker.LockType;
import com.qhdp.annotation.ServiceLock;
import com.qhdp.utils.RedisKeyBuild;
import com.qhdp.utils.RedisUtils;
import com.qhdp.vo.RedisTraceLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.qhdp.constant.DistributedLockConstants.UPDATE_SECKILL_VOUCHER_STOCK_LOCK;


/**
 * &#064;description:  对账执行 接口
 * &#064;author:  phoenix
 **/
@RequiredArgsConstructor
@Slf4j
@Service
public class ReconciliationTaskServiceImpl implements ReconciliationTaskService {

    private final SeckillVoucherService seckillVoucherService;

    private final VoucherOrderService voucherOrderService;

    private final VoucherReconcileLogService voucherReconcileLogService;

    private final RedisUtils redisUtils;
    
    @Override
    public void reconciliationTaskExecute() {
        List<SeckillVoucher> seckillVoucherList = seckillVoucherService.lambdaQuery().list();
        for (SeckillVoucher seckillVoucher : seckillVoucherList) {
            reconciliationTaskExecute(seckillVoucher.getVoucherId());
        }
    }
    
    public void reconciliationTaskExecute(Long voucherId){
        List<VoucherOrder> voucherOrderList = loadPendingOrders(voucherId);
        for (VoucherOrder voucherOrder : voucherOrderList) {
            List<VoucherReconcileLog> logs = loadReconcileLogs(voucherOrder.getId());
            if (CollectionUtil.isEmpty(logs)) {
                ((ReconciliationTaskServiceImpl) AopContext.currentProxy())
                        .markOrderStatus(voucherOrder.getId(), ReconciliationStatus.ABNORMAL);
                continue;
            }
            Map<String, RedisTraceLogVO> redisTraceLogMap = loadRedisTraceLogMap(voucherId);
            String traceLogKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_TRACE_LOG_TAG_KEY, voucherId);
            long ttlSeconds = resolveTraceTtlSeconds(traceLogKey, voucherId);
            boolean anyMissing = backfillMissingTraceLogs(logs, redisTraceLogMap, traceLogKey, ttlSeconds);

            int dbLogCount = logs.size();
            boolean markConsistent = true;
            if (dbLogCount == 1 || dbLogCount == 2) {
                if (anyMissing) {
                    ((ReconciliationTaskService) AopContext.currentProxy()).delRedisStock(voucherId);
                }
            } else {
                ((ReconciliationTaskServiceImpl) AopContext.currentProxy())
                        .markOrderStatus(voucherOrder.getId(), ReconciliationStatus.ABNORMAL);
                markConsistent = false;
            }
            if (markConsistent) {
                ((ReconciliationTaskServiceImpl) AopContext.currentProxy())
                        .markOrderStatus(voucherOrder.getId(), ReconciliationStatus.CONSISTENT);
            }
        }
    }
    
    @Override
    @ServiceLock(lockType= LockType.Write,name = UPDATE_SECKILL_VOUCHER_STOCK_LOCK,keys = {"#voucherId"})
    public void delRedisStock(Long voucherId){
        String stockKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_STOCK_TAG_KEY, voucherId);
        redisUtils.delete(stockKey);
    }

    private List<VoucherOrder> loadPendingOrders(Long voucherId) {
        return voucherOrderService.lambdaQuery()
                .eq(VoucherOrder::getVoucherId, voucherId)
                .le(VoucherOrder::getCreateTime, LocalDateTimeUtil.offset(LocalDateTimeUtil.now(), 2, ChronoUnit.MINUTES))
                .eq(VoucherOrder::getReconciliationStatus, ReconciliationStatus.PENDING.getCode())
                .orderByAsc(VoucherOrder::getCreateTime)
                .list();
    }

    private List<VoucherReconcileLog> loadReconcileLogs(Long orderId) {
        return voucherReconcileLogService.lambdaQuery()
                .eq(VoucherReconcileLog::getOrderId, orderId)
                .orderByAsc(VoucherReconcileLog::getCreateTime)
                .list();
    }

    private Map<String, RedisTraceLogVO> loadRedisTraceLogMap(Long voucherId) {
        return redisUtils.hGetAll(
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_TRACE_LOG_TAG_KEY, voucherId),
                RedisTraceLogVO.class
        );
    }

    private long resolveTraceTtlSeconds(String traceLogKey, Long voucherId) {
        Long ttlSeconds = redisUtils.getExpire(traceLogKey, TimeUnit.SECONDS);
        if (ttlSeconds != null && ttlSeconds > 0) {
            return ttlSeconds;
        }
        SeckillVoucher voucher = seckillVoucherService.lambdaQuery()
                .eq(SeckillVoucher::getVoucherId, voucherId)
                .one();
        long computedTtl = 3600L;
        if (voucher != null && voucher.getEndTime() != null) {
            Date now = DateUtil.date();
            long secondsUntilEnd = Math.max(0L, Duration.between((Temporal) now, (Temporal) voucher.getEndTime()).getSeconds());
            computedTtl = Math.max(1L, secondsUntilEnd + Duration.ofDays(1).getSeconds());
        }
        return computedTtl;
    }

    private boolean backfillMissingTraceLogs(List<VoucherReconcileLog> logs,
                                             Map<String, RedisTraceLogVO> redisTraceLogMap,
                                             String traceLogKey,
                                             long ttlSeconds) {
        boolean anyMissing = false;
        for (VoucherReconcileLog log : logs) {
            String traceIdStr = String.valueOf(log.getTraceId());
            RedisTraceLogVO existed = redisTraceLogMap.get(traceIdStr);
            if (existed != null) {
                continue;
            }
            anyMissing = true;
            RedisTraceLogVO model = new RedisTraceLogVO();
            model.setLogType(String.valueOf(log.getLogType()));
            model.setTs(log.getCreateTime().getTime());
            model.setOrderId(String.valueOf(log.getOrderId()));
            model.setTraceId(traceIdStr);
            model.setUserId(String.valueOf(log.getUserId()));
            model.setVoucherId(String.valueOf(log.getVoucherId()));
            model.setBeforeQty(log.getBeforeQty());
            model.setChangeQty(log.getChangeQty());
            model.setAfterQty(log.getAfterQty());
            redisUtils.hSet(traceLogKey, traceIdStr, model);
            Long currentTtl = redisUtils.getExpire(traceLogKey, TimeUnit.SECONDS);
            if (currentTtl == null || currentTtl <= 0) {
                redisUtils.expire(traceLogKey, ttlSeconds);
            }
        }
        return anyMissing;
    }

    @Transactional(rollbackFor = Exception.class)
    public void markOrderStatus(Long orderId, ReconciliationStatus status) {
        voucherOrderService.lambdaUpdate()
                .set(VoucherOrder::getReconciliationStatus, status.getCode())
                .set(VoucherOrder::getUpdateTime, LocalDateTime.now())
                .eq(VoucherOrder::getId, orderId)
                .update();
        voucherReconcileLogService.lambdaUpdate()
                .set(VoucherReconcileLog::getReconciliationStatus, status.getCode())
                .set(VoucherReconcileLog::getUpdateTime, LocalDateTime.now())
                .eq(VoucherReconcileLog::getOrderId, orderId)
                .update();
    }
}
