package com.qhdp.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qhdp.annotation.RepeatExecuteLimit;
import com.qhdp.dto.CancelVoucherOrderDTO;
import com.qhdp.dto.GetVoucherOrderByVoucherIdDTO;
import com.qhdp.dto.GetVoucherOrderDTO;
import com.qhdp.dto.VoucherReconcileLogDTO;
import com.qhdp.entity.*;
import com.qhdp.enums.*;
import com.qhdp.exception.qhdpFrameException;
import com.qhdp.kafka.lua.SeckillVoucherDomain;
import com.qhdp.kafka.lua.SeckillVoucherOperate;
import com.qhdp.kafka.message.MessageExtend;
import com.qhdp.kafka.message.SeckillVoucherMessage;
import com.qhdp.kafka.producer.SeckillVoucherProducer;
import com.qhdp.kafka.redis.RedisVoucherData;
import com.qhdp.mapper.VoucherMapper;
import com.qhdp.mapper.VoucherOrderRouterMapper;
import com.qhdp.service.SeckillVoucherService;
import com.qhdp.service.UserInfoService;
import com.qhdp.service.VoucherOrderService;
import com.qhdp.mapper.VoucherOrderMapper;
import com.qhdp.service.VoucherReconcileLogService;
import com.qhdp.toolkit.SnowflakeIdGenerator;
import com.qhdp.utils.RedisKeyBuild;
import com.qhdp.utils.RedisUtils;
import com.qhdp.utils.SpringUtil;
import com.qhdp.utils.UserHolder;
import com.qhdp.vo.SeckillVoucherFullVO;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.qhdp.constant.Constant.SECKILL_VOUCHER_TOPIC;
import static com.qhdp.constant.RepeatExecuteLimitConstant.SECKILL_VOUCHER_ORDER;

/**
* @author phoenix
* &#064;description  针对表【tb_voucher_order】的数据库操作Service实现
* &#064;createDate  2026-03-11 14:33:53
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder>
    implements VoucherOrderService{

    private final SeckillVoucherService seckillVoucherService;
    private final RedisUtils redisUtils;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final SeckillVoucherProducer seckillVoucherProducer;
    private final UserInfoService userInfoService;
    private final SeckillVoucherOperate seckillVoucherOperate;
    private final VoucherOrderRouterMapper voucherOrderRouterMapper;
    private final VoucherReconcileLogService voucherReconcileLogService;
    private final VoucherMapper voucherMapper;
    private final RedisVoucherData redisVoucherData;


    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("lua/seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    public static final ThreadPoolExecutor SECKILL_ORDER_EXECUTOR =
            new ThreadPoolExecutor(
                    1,
                    1,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(1024),
                    new NamedThreadFactory("seckill-order-", false),
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );

    private static class NamedThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final boolean daemon;
        private final AtomicInteger index = new AtomicInteger(1);

        public NamedThreadFactory(String namePrefix, boolean daemon) {
            this.namePrefix = namePrefix;
            this.daemon = daemon;
        }

        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread t = new Thread(r, namePrefix + index.getAndIncrement());
            t.setDaemon(daemon);
            t.setUncaughtExceptionHandler((thread, ex) ->
                    log.error("未捕获异常，线程={}, err={}", thread.getName(), ex.getMessage(), ex)
            );
            return t;
        }

        @PreDestroy
        private void destroy(){
            try {
                SECKILL_ORDER_EXECUTOR.shutdown();
                if (!SECKILL_ORDER_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                    SECKILL_ORDER_EXECUTOR.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                SECKILL_ORDER_EXECUTOR.shutdownNow();
            }
        }
    }

    @Override
    public boolean autoIssueVoucherToEarliestSubscriber(final Long voucherId, final Long excludeUserId) {
        SeckillVoucherFullVO seckillVoucherFullVO = seckillVoucherService.queryByVoucherId(voucherId);
        if (Objects.isNull(seckillVoucherFullVO)
                ||
                Objects.isNull(seckillVoucherFullVO.getBeginTime())
                ||
                Objects.isNull(seckillVoucherFullVO.getEndTime())) {
            return false;
        }
        seckillVoucherService.loadVoucherStock(voucherId);
        String candidateUserIdStr = findEarliestCandidate(voucherId, excludeUserId);
        if (StrUtil.isBlank(candidateUserIdStr)) {
            return false;
        }
        return issueToCandidate(voucherId, candidateUserIdStr, seckillVoucherFullVO);
    }

    @Override
    public Long seckillVoucher(Long voucherId) {
        return doSeckillVoucherPlus(voucherId);
    }

    private Long doSeckillVoucherPlus(Long voucherId) {
        SeckillVoucherFullVO seckillVoucherFullVO = seckillVoucherService.queryByVoucherId(voucherId);
        seckillVoucherService.loadVoucherStock(voucherId);
        Long userId = UserHolder.getUser().getId();
        verifyUserLevel(seckillVoucherFullVO,userId);
        long orderId = snowflakeIdGenerator.nextId();
        long traceId = snowflakeIdGenerator.nextId();
        List<String> keys = ListUtil.of(
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_STOCK_TAG_KEY, voucherId),
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_USER_TAG_KEY, voucherId),
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_TRACE_LOG_TAG_KEY, voucherId)
        );
        String[] args = new String[9];
        args[0] = voucherId.toString();
        args[1] = userId.toString();
        args[2] = String.valueOf(LocalDateTimeUtil.toEpochMilli(seckillVoucherFullVO.getBeginTime()));
        args[3] = String.valueOf(LocalDateTimeUtil.toEpochMilli(seckillVoucherFullVO.getEndTime()));
        args[4] = String.valueOf(seckillVoucherFullVO.getStatus());
        args[5] = String.valueOf(orderId);
        args[6] = String.valueOf(traceId);
        args[7] = String.valueOf(LogType.DEDUCT.getCode());
        long secondsUntilEnd = Duration.between(LocalDateTimeUtil.now(), seckillVoucherFullVO.getEndTime()).getSeconds();
        long ttlSeconds = Math.max(1L, secondsUntilEnd + Duration.ofDays(1).getSeconds());
        args[8] = String.valueOf(ttlSeconds);
        SeckillVoucherDomain seckillVoucherDomain = seckillVoucherOperate.execute(
                keys,
                args
        );
        if (!seckillVoucherDomain.getCode().equals(BaseCode.SUCCESS.getCode())) {
            throw new qhdpFrameException(Objects.requireNonNull(BaseCode.getRc(seckillVoucherDomain.getCode())));
        }
        SeckillVoucherMessage seckillVoucherMessage = new SeckillVoucherMessage(
                userId,
                voucherId,
                orderId,
                traceId,
                seckillVoucherDomain.getBeforeQty(),
                seckillVoucherDomain.getDeductQty(),
                seckillVoucherDomain.getAfterQty(),
                Boolean.FALSE
        );
        seckillVoucherProducer.sendPayload(
                SpringUtil.getPrefixDistinctionName() + "-" + SECKILL_VOUCHER_TOPIC,
                seckillVoucherMessage);
        return orderId;
    }

    @Override
    public Long getSeckillVoucherOrder(GetVoucherOrderDTO getVoucherOrderDTO) {
        VoucherOrder voucherOrder =
                redisUtils.get(RedisKeyBuild.createRedisKey(
                                RedisKeyManage.DB_SECKILL_ORDER_KEY,
                                getVoucherOrderDTO.getOrderId()),
                        VoucherOrder.class);
        if (Objects.nonNull(voucherOrder)) {
            return voucherOrder.getId();
        }
        VoucherOrderRouter voucherOrderRouter =
                voucherOrderRouterMapper.selectOne(
                Wrappers.lambdaQuery(VoucherOrderRouter.class)
                        .eq(VoucherOrderRouter::getOrderId, getVoucherOrderDTO.getOrderId()));
        if (Objects.nonNull(voucherOrderRouter)) {
            return voucherOrderRouter.getOrderId();
        }
        return null;
    }

    @Override
    public Long getSeckillVoucherOrderIdByVoucherId(GetVoucherOrderByVoucherIdDTO getVoucherOrderByVoucherIdDTO) {
        VoucherOrder voucherOrder = lambdaQuery()
                .eq(VoucherOrder::getUserId, UserHolder.getUser().getId())
                .eq(VoucherOrder::getVoucherId, getVoucherOrderByVoucherIdDTO.getVoucherId())
                .eq(VoucherOrder::getStatus, OrderStatus.NORMAL.getCode())
                .one();
        if (Objects.nonNull(voucherOrder)) {
            return voucherOrder.getId();
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancel(CancelVoucherOrderDTO cancelVoucherOrderDTO) {
        VoucherOrder voucherOrder = lambdaQuery()
                .eq(VoucherOrder::getUserId, UserHolder.getUser().getId())
                .eq(VoucherOrder::getVoucherId, cancelVoucherOrderDTO.getVoucherId())
                .eq(VoucherOrder::getStatus, OrderStatus.NORMAL.getCode())
                .one();
        if (Objects.isNull(voucherOrder)) {
            throw new qhdpFrameException(BaseCode.SECKILL_VOUCHER_ORDER_NOT_EXIST);
        }
        SeckillVoucher seckillVoucher = seckillVoucherService.lambdaQuery()
                .eq(SeckillVoucher::getVoucherId, cancelVoucherOrderDTO.getVoucherId())
                .one();
        if (Objects.isNull(seckillVoucher)) {
            throw new qhdpFrameException(BaseCode.SECKILL_VOUCHER_NOT_EXIST);
        }
        boolean updateResult = lambdaUpdate().set(VoucherOrder::getStatus, OrderStatus.CANCEL.getCode())
                .set(VoucherOrder::getUpdateTime, LocalDateTimeUtil.now())
                .eq(VoucherOrder::getUserId, UserHolder.getUser().getId())
                .eq(VoucherOrder::getVoucherId, cancelVoucherOrderDTO.getVoucherId())
                .update();
        long traceId = snowflakeIdGenerator.nextId();
        VoucherReconcileLogDTO voucherReconcileLogDTO = new VoucherReconcileLogDTO();
        voucherReconcileLogDTO.setOrderId(voucherOrder.getId());
        voucherReconcileLogDTO.setUserId(voucherOrder.getUserId());
        voucherReconcileLogDTO.setVoucherId(voucherOrder.getVoucherId());
        voucherReconcileLogDTO.setDetail("cancel voucher order ");
        voucherReconcileLogDTO.setBeforeQty(seckillVoucher.getStock());
        voucherReconcileLogDTO.setChangeQty(1);
        voucherReconcileLogDTO.setAfterQty(seckillVoucher.getStock() + 1);
        voucherReconcileLogDTO.setTraceId(traceId);
        voucherReconcileLogDTO.setLogType(LogType.RESTORE.getCode());
        voucherReconcileLogDTO.setBusinessType( BusinessType.CANCEL.getCode());
        boolean saveReconcileLogResult = voucherReconcileLogService.saveReconcileLog(voucherReconcileLogDTO);

        boolean rollbackStockResult = seckillVoucherService.rollbackStock(cancelVoucherOrderDTO.getVoucherId());

        Boolean result = updateResult && saveReconcileLogResult && rollbackStockResult;
        if (result) {
            redisVoucherData.rollbackRedisVoucherData(
                    SeckillVoucherOrderOperate.YES,
                    traceId,
                    voucherOrder.getVoucherId(),
                    voucherOrder.getUserId(),
                    voucherOrder.getId(),
                    seckillVoucher.getStock(),
                    1,
                    seckillVoucher.getStock() + 1
            );
            redisUtils.hDelete(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_SUBSCRIBE_STATUS_TAG_KEY,
                            cancelVoucherOrderDTO.getVoucherId()),
                    String.valueOf(voucherOrder.getUserId()));
            Voucher voucher = voucherMapper.selectById(voucherOrder.getVoucherId());
            if (Objects.nonNull(voucher)) {
                String day = LocalDateTime.ofInstant(voucherOrder.getCreateTime().toInstant(), ZoneId.systemDefault())
                        .format(DateTimeFormatter.BASIC_ISO_DATE);
                String dailyKey = RedisKeyBuild.createRedisKey(
                        RedisKeyManage.SECKILL_SHOP_TOP_BUYERS_DAILY_TAG_KEY,
                        voucher.getShopId(),
                        day
                );
                redisUtils.zIncrementScore(dailyKey, String.valueOf(voucherOrder.getUserId()), -1.0);
            }

            try {
                autoIssueVoucherToEarliestSubscriber(
                        voucherOrder.getVoucherId(),
                        voucherOrder.getUserId()
                );
            } catch (Exception e) {
                log.warn("自动发券失败，voucherId={}, err=\n{}", voucherOrder.getVoucherId(), e.getMessage());
            }
        }
        return result;
    }

    @Override
    @RepeatExecuteLimit(name = SECKILL_VOUCHER_ORDER,keys = {"#message.uuid"})
    @Transactional(rollbackFor = Exception.class)
    public boolean createVoucherOrderPlus(MessageExtend<SeckillVoucherMessage> message) {
        SeckillVoucherMessage messageBody = message.getMessageBody();
        Long userId = messageBody.getUserId();
        VoucherOrder normalVoucherOrder = lambdaQuery()
                .eq(VoucherOrder::getVoucherId, messageBody.getVoucherId())
                .eq(VoucherOrder::getUserId, userId)
                .eq(VoucherOrder::getStatus,OrderStatus.NORMAL.getCode())
                .one();
        if (Objects.nonNull(normalVoucherOrder)) {
            log.warn("已存在此订单，voucherId：{},userId：{}", normalVoucherOrder.getVoucherId(), userId);
            throw new qhdpFrameException(BaseCode.VOUCHER_ORDER_EXIST);
        }
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", messageBody.getVoucherId())
                .gt("stock", 0)
                .update();
        if (!success) {
            throw new qhdpFrameException("优惠券库存不足！优惠券id:" + messageBody.getVoucherId());
        }
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(messageBody.getOrderId());
        voucherOrder.setUserId(messageBody.getUserId());
        voucherOrder.setVoucherId(messageBody.getVoucherId());
        save(voucherOrder);
        VoucherOrderRouter voucherOrderRouter = new VoucherOrderRouter();
        voucherOrderRouter.setId(snowflakeIdGenerator.nextId());
        voucherOrderRouter.setOrderId(voucherOrder.getId());
        voucherOrderRouter.setUserId(userId);
        voucherOrderRouter.setVoucherId(voucherOrder.getVoucherId());
        voucherOrderRouterMapper.insert(voucherOrderRouter);
        redisUtils.set(RedisKeyBuild.createRedisKey(
                        RedisKeyManage.DB_SECKILL_ORDER_KEY,messageBody.getOrderId()),
                voucherOrder,
                60L,
                TimeUnit.SECONDS
        );
        voucherReconcileLogService.saveReconcileLog(
                LogType.DEDUCT.getCode(),
                BusinessType.SUCCESS.getCode(),
                "order created",
                message
        );
        return true;

    }

    private String findEarliestCandidate(final Long voucherId, final Long excludeUserId) {
        String subscribeZSetKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_SUBSCRIBE_ZSET_TAG_KEY, voucherId);
        String purchasedSetKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_USER_TAG_KEY, voucherId);

        final long pageCount = 1L;
        long offset = 0L;
        while (true) {
            Set<ZSetOperations.TypedTuple<String>> page = redisUtils.zRangeByScoreWithScore(
                    subscribeZSetKey,
                    Double.NEGATIVE_INFINITY,
                    Double.POSITIVE_INFINITY,
                    offset,
                    pageCount,
                    String.class
            );
            if (CollectionUtil.isEmpty(page)) {
                return null;
            }
            ZSetOperations.TypedTuple<String> tuple = page.iterator().next();
            if (Objects.isNull(tuple) || Objects.isNull(tuple.getValue())) {
                offset++;
                continue;
            }
            String uidStr = tuple.getValue();
            if (StrUtil.isBlank(uidStr)) {
                offset++;
                continue;
            }
            if (Objects.nonNull(excludeUserId) && Objects.equals(uidStr, String.valueOf(excludeUserId))) {
                offset++;
                continue;
            }
            Boolean purchased = redisUtils.sIsMember(purchasedSetKey, uidStr);
            if (BooleanUtil.isTrue(purchased)) {
                offset++;
                continue;
            }
            return uidStr;
        }
    }

    private boolean issueToCandidate(final Long voucherId,
                                     final String candidateUserIdStr,
                                     final SeckillVoucherFullVO seckillVoucherFullVO) {
        Long candidateUserId = Long.valueOf(candidateUserIdStr);
        try {
            verifyUserLevel(seckillVoucherFullVO, candidateUserId);
        } catch (Exception e) {
            log.info("候选用户不满足人群规则，自动发券跳过。voucherId={}, userId={}", voucherId, candidateUserId);
            return false;
        }
        List<String> keys = buildSeckillKeys(voucherId);
        long orderId = snowflakeIdGenerator.nextId();
        long traceId = snowflakeIdGenerator.nextId();
        String[] args = buildSeckillArgs(voucherId, candidateUserIdStr, seckillVoucherFullVO, orderId, traceId);
        SeckillVoucherDomain domain = seckillVoucherOperate.execute(keys, args);
        if (!Objects.equals(domain.getCode(), BaseCode.SUCCESS.getCode())) {
            log.info("自动发券Lua扣减失败，code={}, voucherId={}, userId={}", domain.getCode(), voucherId, candidateUserId);
            return false;
        }
        SeckillVoucherMessage message = new SeckillVoucherMessage(
                candidateUserId,
                voucherId,
                orderId,
                traceId,
                domain.getBeforeQty(),
                domain.getDeductQty(),
                domain.getAfterQty(),
                Boolean.TRUE
        );
        seckillVoucherProducer.sendPayload(
                SpringUtil.getPrefixDistinctionName() + "-" + SECKILL_VOUCHER_TOPIC,
                message
        );
        return true;
    }

    private String[] buildSeckillArgs(final Long voucherId,
                                      final String userIdStr,
                                      final SeckillVoucherFullVO seckillVoucherFullVO,
                                      final long orderId,
                                      final long traceId) {
        String[] args = new String[9];
        args[0] = voucherId.toString();
        args[1] = userIdStr;
        args[2] = String.valueOf(LocalDateTimeUtil.toEpochMilli(seckillVoucherFullVO.getBeginTime()));
        args[3] = String.valueOf(LocalDateTimeUtil.toEpochMilli(seckillVoucherFullVO.getEndTime()));
        args[4] = String.valueOf(seckillVoucherFullVO.getStatus());
        args[5] = String.valueOf(orderId);
        args[6] = String.valueOf(traceId);
        args[7] = String.valueOf(LogType.DEDUCT.getCode());
        args[8] = String.valueOf(computeTtlSeconds(seckillVoucherFullVO));
        return args;
    }

    private long computeTtlSeconds(final SeckillVoucherFullVO seckillVoucherFullVO) {
        long secondsUntilEnd = Duration.between(LocalDateTimeUtil.now(), seckillVoucherFullVO.getEndTime()).getSeconds();
        return Math.max(1L, secondsUntilEnd + Duration.ofDays(1).getSeconds());
    }

    private List<String> buildSeckillKeys(final Long voucherId) {
        String stockKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_STOCK_TAG_KEY, voucherId);
        String userKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_USER_TAG_KEY, voucherId);
        String traceKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_TRACE_LOG_TAG_KEY, voucherId);
        return ListUtil.of(stockKey, userKey, traceKey);
    }

    public void verifyUserLevel(SeckillVoucherFullVO seckillVoucherFullVO,Long userId){
        String allowedLevelsStr = seckillVoucherFullVO.getAllowedLevels();
        Integer minLevel = seckillVoucherFullVO.getMinLevel();
        boolean hasLevelRule = StrUtil.isNotBlank(allowedLevelsStr) || Objects.nonNull(minLevel);
        if (!hasLevelRule) {
            return;
        }
        UserInfo userInfo = userInfoService.getByUserId(userId);
        if (Objects.isNull(userInfo)) {
            throw new qhdpFrameException(BaseCode.USER_NOT_EXIST);
        }
        boolean allowed = true;
        Integer level = userInfo.getLevel();
        if (StrUtil.isNotBlank(allowedLevelsStr)) {
            try {
                Set<Integer> allowedLevels = Arrays.stream(allowedLevelsStr.split(","))
                        .map(String::trim)
                        .filter(StrUtil::isNotBlank)
                        .map(Integer::valueOf)
                        .collect(Collectors.toSet());
                if (CollectionUtil.isNotEmpty(allowedLevels)) {
                    allowed = allowedLevels.contains(level);
                }
            } catch (Exception parseEx) {
                log.warn("allowedLevels 解析失败, voucherId={}, raw={}",
                        seckillVoucherFullVO.getVoucherId(),
                        allowedLevelsStr, parseEx);
            }
        }
        if (allowed && Objects.nonNull(minLevel)) {
            allowed = Objects.nonNull(level) && level >= minLevel;
        }
        if (!allowed) {
            throw new qhdpFrameException("当前会员级别不满足参与条件");
        }
    }
}




