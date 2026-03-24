package com.qhdp.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qhdp.entity.UserInfo;
import com.qhdp.entity.VoucherOrder;
import com.qhdp.enums.BaseCode;
import com.qhdp.enums.LogType;
import com.qhdp.enums.RedisKeyManage;
import com.qhdp.exception.qhdpFrameException;
import com.qhdp.kafka.lua.SeckillVoucherDomain;
import com.qhdp.kafka.lua.SeckillVoucherOperate;
import com.qhdp.kafka.message.SeckillVoucherMessage;
import com.qhdp.kafka.producer.SeckillVoucherProducer;
import com.qhdp.service.SeckillVoucherService;
import com.qhdp.service.UserInfoService;
import com.qhdp.service.VoucherOrderService;
import com.qhdp.mapper.VoucherOrderMapper;
import com.qhdp.toolkit.SnowflakeIdGenerator;
import com.qhdp.utils.RedisKeyBuild;
import com.qhdp.utils.RedisUtils;
import com.qhdp.utils.SpringUtil;
import com.qhdp.vo.SeckillVoucherFullVO;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
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

/**
* @author phoenix
* @description 针对表【tb_voucher_order】的数据库操作Service实现
* @createDate 2026-03-11 14:33:53
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




