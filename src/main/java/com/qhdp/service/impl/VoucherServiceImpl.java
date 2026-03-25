package com.qhdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qhdp.delay.context.DelayQueueContext;
import com.qhdp.delay.message.DelayedVoucherReminderMessage;
import com.qhdp.enums.BaseCode;
import com.qhdp.enums.RedisKeyManage;
import com.qhdp.dto.*;
import com.qhdp.entity.SeckillVoucher;
import com.qhdp.entity.Voucher;
import com.qhdp.enums.StockUpdateType;
import com.qhdp.enums.SubscribeStatus;
import com.qhdp.exception.qhdpFrameException;
import com.qhdp.factory.BloomFilterHandlerFactory;
import com.qhdp.mapper.SeckillVoucherMapper;
import com.qhdp.service.VoucherOrderService;
import com.qhdp.service.VoucherService;
import com.qhdp.mapper.VoucherMapper;
import com.qhdp.servicelocker.LockType;
import com.qhdp.annotation.ServiceLock;
import com.qhdp.toolkit.SnowflakeIdGenerator;
import com.qhdp.utils.*;
import com.qhdp.vo.GetSubscribeStatusVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.qhdp.constant.Constant.BLOOM_FILTER_HANDLER_VOUCHER;
import static com.qhdp.constant.Constant.DELAY_VOUCHER_REMINDER;
import static com.qhdp.constant.DistributedLockConstants.UPDATE_SECKILL_VOUCHER_LOCK;
import static com.qhdp.service.impl.VoucherOrderServiceImpl.SECKILL_ORDER_EXECUTOR;

/**
* @author phoenix
* @description 针对表【tb_voucher】的数据库操作Service实现
* @createDate 2026-03-11 14:33:43
*/
@RequiredArgsConstructor
@Service
@Slf4j
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher>
    implements VoucherService{

    private final SeckillVoucherMapper seckillVoucherMapper;

    private final BloomFilterHandlerFactory bloomFilterHandlerFactory;

    private final SnowflakeIdGenerator snowflakeIdGenerator;

    private final RedisUtils redisUtils;

    private final DelayQueueContext delayQueueContext;

    private final SeckillVoucherCacheInvalidationPublisher seckillVoucherCacheInvalidationPublisher;

    private final VoucherOrderService voucherOrderService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addSeckillVoucher(SeckillVoucherDTO seckillVoucherDTO) {
        return doAddSeckillVoucherPlus(seckillVoucherDTO);
    }

    private Long doAddSeckillVoucherPlus(SeckillVoucherDTO seckillVoucherDTO) {
        VoucherDTO voucherDTO = new VoucherDTO();
        BeanUtil.copyProperties(seckillVoucherDTO, voucherDTO);
        Long voucherId = addVoucher(voucherDTO);
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setId(snowflakeIdGenerator.nextId());
        seckillVoucher.setVoucherId(voucherId);
        seckillVoucher.setInitStock(seckillVoucherDTO.getStock());
        seckillVoucher.setStock(seckillVoucherDTO.getStock());
        seckillVoucher.setBeginTime(seckillVoucherDTO.getBeginTime());
        seckillVoucher.setEndTime(seckillVoucherDTO.getEndTime());
        seckillVoucher.setAllowedLevels(seckillVoucherDTO.getAllowedLevels());
        seckillVoucher.setMinLevel(seckillVoucherDTO.getMinLevel());
        seckillVoucherMapper.insert(seckillVoucher);
        long ttlSeconds = Math.max(
                DateUtil.betweenMs(DateUtil.date(), seckillVoucher.getEndTime())/1000,
                1L
        );
        redisUtils.set(
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_STOCK_TAG_KEY, voucherId),
                String.valueOf(seckillVoucher.getStock()),
                ttlSeconds,
                TimeUnit.SECONDS
        );
        seckillVoucher.setStock(null);
        redisUtils.set(
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_TAG_KEY, voucherId),
                seckillVoucher,
                ttlSeconds,
                TimeUnit.SECONDS
        );
        sendDelayedVoucherReminder(seckillVoucher);
        return voucherId;
    }

    private void sendDelayedVoucherReminder(SeckillVoucher seckillVoucher) {
    }

    @Override
    public List<Voucher> queryVoucherOfShop(Long shopId) {
        return list(new LambdaQueryWrapper<Voucher>()
                .eq(Voucher::getShopId, shopId));
    }

    @Override
    public Long addVoucher(VoucherDTO voucherDTO) {
        Voucher one = lambdaQuery().orderByDesc(Voucher::getId).one();
        long newId = 1L;
        if (one != null) {
            newId = one.getId() + 1;
        }
        Voucher voucher = new Voucher();
        BeanUtil.copyProperties(voucherDTO, voucher);
        voucher.setId(newId);
        save(voucher);
        bloomFilterHandlerFactory.get(BLOOM_FILTER_HANDLER_VOUCHER).add(voucher.getId().toString());
        return voucher.getId();
    }

    @Override
    @ServiceLock(lockType= LockType.Write,name = UPDATE_SECKILL_VOUCHER_LOCK,keys = {"#updateSeckillVoucherDTO.voucherId"})
    @Transactional(rollbackFor = Exception.class)
    public void updateSeckillVoucher(UpdateSeckillVoucherDTO updateSeckillVoucherDTO) {
        Long voucherId = updateSeckillVoucherDTO.getVoucherId();
        boolean updatedVoucher = false;
        var voucherUpdate = this.lambdaUpdate().eq(Voucher::getId, voucherId);
        if (updateSeckillVoucherDTO.getTitle() != null) {
            voucherUpdate.set(Voucher::getTitle, updateSeckillVoucherDTO.getTitle());
            updatedVoucher = true;
        }
        if (updateSeckillVoucherDTO.getSubTitle() != null) {
            voucherUpdate.set(Voucher::getSubTitle, updateSeckillVoucherDTO.getSubTitle());
            updatedVoucher = true;
        }
        if (updateSeckillVoucherDTO.getRules() != null) {
            voucherUpdate.set(Voucher::getRules, updateSeckillVoucherDTO.getRules());
            updatedVoucher = true;
        }
        if (updateSeckillVoucherDTO.getPayValue() != null) {
            voucherUpdate.set(Voucher::getPayValue, updateSeckillVoucherDTO.getPayValue());
            updatedVoucher = true;
        }
        if (updateSeckillVoucherDTO.getActualValue() != null) {
            voucherUpdate.set(Voucher::getActualValue, updateSeckillVoucherDTO.getActualValue());
            updatedVoucher = true;
        }
        if (updateSeckillVoucherDTO.getType() != null) {
            voucherUpdate.set(Voucher::getType, updateSeckillVoucherDTO.getType());
            updatedVoucher = true;
        }
        if (updateSeckillVoucherDTO.getStatus() != null) {
            voucherUpdate.set(Voucher::getStatus, updateSeckillVoucherDTO.getStatus());
            updatedVoucher = true;
        }
        if (updatedVoucher) {
            voucherUpdate.set(Voucher::getUpdateTime, LocalDateTimeUtil.now()).update();
        }

        boolean updatedSeckill = false;
        LambdaUpdateWrapper<SeckillVoucher> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SeckillVoucher::getVoucherId, voucherId);
        if (updateSeckillVoucherDTO.getBeginTime() != null) {
            wrapper.set(SeckillVoucher::getBeginTime, updateSeckillVoucherDTO.getBeginTime());
            updatedSeckill = true;
        }
        if (updateSeckillVoucherDTO.getEndTime() != null) {
            wrapper.set(SeckillVoucher::getEndTime, updateSeckillVoucherDTO.getEndTime());
            updatedSeckill = true;
        }
        if (updateSeckillVoucherDTO.getAllowedLevels() != null) {
            wrapper.set(SeckillVoucher::getAllowedLevels, updateSeckillVoucherDTO.getAllowedLevels());
            updatedSeckill = true;
        }
        if (updateSeckillVoucherDTO.getMinLevel() != null) {
            wrapper.set(SeckillVoucher::getMinLevel, updateSeckillVoucherDTO.getMinLevel());
            updatedSeckill = true;
        }
        if (updatedSeckill) {
            wrapper.set(SeckillVoucher::getUpdateTime, LocalDateTimeUtil.now());
        }
        if (updatedSeckill) {
            seckillVoucherMapper.update(null, wrapper);
        }
// 6. 最终逻辑：更新成功后清理缓存
        if (updatedVoucher || updatedSeckill) {
            seckillVoucherCacheInvalidationPublisher.publishInvalidate(voucherId, "update");
        }
    }

    @Override
    @ServiceLock(lockType= LockType.Write,name = UPDATE_SECKILL_VOUCHER_LOCK,keys = {"#updateSeckillVoucherDTO.voucherId"})
    @Transactional(rollbackFor = Exception.class)
    public void updateSeckillVoucherStock(UpdateSeckillVoucherStockDTO updateSeckillVoucherStockDTO) {
        SeckillVoucher seckillVoucher = seckillVoucherMapper.selectOne(
                Wrappers.lambdaQuery(SeckillVoucher.class)
                        .eq(SeckillVoucher::getVoucherId, updateSeckillVoucherStockDTO.getVoucherId()));
        if (Objects.isNull(seckillVoucher)) {
            throw new qhdpFrameException(BaseCode.SECKILL_VOUCHER_NOT_EXIST);
        }
        Integer oldStock = seckillVoucher.getStock();
        Integer oldInitStock = seckillVoucher.getInitStock();
        Integer newInitStock = updateSeckillVoucherStockDTO.getInitStock();
        int changeStock = newInitStock - oldInitStock;
        if (changeStock == 0) {
            return;
        }
        int newStock = oldStock + changeStock;
        if (newStock < 0 ) {
            throw new qhdpFrameException(BaseCode.AFTER_SECKILL_VOUCHER_REMAIN_STOCK_NOT_NEGATIVE_NUMBER);
        }
        StockUpdateType stockUpdateType = StockUpdateType.INCREASE;
        if (changeStock < 0) {
            stockUpdateType = StockUpdateType.DECREASE;
        }
        seckillVoucherMapper.update(
                null,
                Wrappers.lambdaUpdate(SeckillVoucher.class)
                        .set(SeckillVoucher::getStock, newStock)
                        .set(SeckillVoucher::getInitStock, newInitStock)
                        .set(SeckillVoucher::getUpdateTime, LocalDateTimeUtil.now())
                        .eq(SeckillVoucher::getVoucherId, seckillVoucher.getVoucherId()));
        String oldRedisStockStr = redisUtils.get(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_STOCK_TAG_KEY,
                updateSeckillVoucherStockDTO.getVoucherId()), String.class);
        Integer newRedisStock = null;
        if (StrUtil.isBlank(oldRedisStockStr)) {
            redisUtils.set(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_STOCK_TAG_KEY,
                    updateSeckillVoucherStockDTO.getVoucherId()),String.valueOf(newInitStock));
        }else {
            int oldRedisStock = Integer.parseInt(oldRedisStockStr);
            newRedisStock = oldRedisStock + changeStock;
            if (newRedisStock < 0 ) {
                throw new qhdpFrameException(BaseCode.AFTER_SECKILL_VOUCHER_REMAIN_STOCK_NOT_NEGATIVE_NUMBER);
            }
            redisUtils.set(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_STOCK_TAG_KEY,
                    updateSeckillVoucherStockDTO.getVoucherId()),String.valueOf(newRedisStock));
        }
        log.info("修改库存成功！修改库存类型：{},修改前：数据库初始库存：{},redis旧库存：{},修改后：数据库初始库存：{},redis新库存：{}",
                stockUpdateType.getMsg(),
                oldInitStock,
                StrUtil.isBlank(oldRedisStockStr) ? null : oldRedisStockStr,
                newInitStock,
                newRedisStock
        );
        //如果是增加库存,尝试将资格自动分配给订阅队列中最早的未购用户
        if (stockUpdateType == StockUpdateType.INCREASE) {
            SECKILL_ORDER_EXECUTOR.execute(() -> voucherOrderService
                    .autoIssueVoucherToEarliestSubscriber(seckillVoucher.getVoucherId(),null));
        }
    }

    public void subscribe(final VoucherSubscribeDTO voucherSubscribeDTO) {
        Long voucherId = voucherSubscribeDTO.getVoucherId();
        Long userId = UserHolder.getUser().getId();
        String userIdStr = String.valueOf(userId);

        //计算统一 TTL（过期秒数）
        Long ttlSeconds = redisUtils.getExpire(
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_TAG_KEY, voucherId),
                TimeUnit.SECONDS
        );
        if (Objects.isNull(ttlSeconds) || ttlSeconds <= 0) {
            SeckillVoucher sv = seckillVoucherMapper.selectOne(
                    Wrappers.lambdaQuery(SeckillVoucher.class)
                            .eq(SeckillVoucher::getVoucherId, voucherId));
            if (Objects.nonNull(sv) && Objects.nonNull(sv.getEndTime())) {
                ttlSeconds = Math.max(
                        DateUtil.betweenMs(DateUtil.date(), sv.getEndTime())/1000,
                        1L
                );
            } else {
                ttlSeconds = 3600L;
            }
        }
        //检查是否已购买，判断用户是否在 SECKILL_USER_TAG_KEY:{voucherId} 集合中（已购集合）
        boolean purchased = Boolean.TRUE.equals(redisUtils.sIsMember(
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_USER_TAG_KEY, voucherId),
                userIdStr
        ));


        String statusKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_SUBSCRIBE_STATUS_TAG_KEY, voucherId);
        if (purchased) {
            redisUtils.hSet(statusKey, userIdStr, SubscribeStatus.SUCCESS.getCode(), ttlSeconds, TimeUnit.SECONDS);
            redisUtils.expire(statusKey, ttlSeconds);
            return;
        }

        // 加入订阅集合（SET），幂等
        String setKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_SUBSCRIBE_USER_TAG_KEY, voucherId);
        Long added = redisUtils.sAdd(setKey, userIdStr);
        redisUtils.expire(setKey, ttlSeconds);

        // 加入订阅队列（ZSET），仅首次加入时写入顺序分数
        String zsetKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_SUBSCRIBE_ZSET_TAG_KEY, voucherId);
        if (Objects.nonNull(added) && added > 0) {
            redisUtils.zAdd(zsetKey, userIdStr, (double) System.currentTimeMillis());
        } else {
            // 已存在则仅对齐TTL
            redisUtils.expire(zsetKey, ttlSeconds);
        }

        // 更新订阅状态为 SUBSCRIBED（如已是 SUCCESS 则不降级）
        Integer prev = redisUtils.hGet(statusKey, userIdStr, Integer.class);
        if (!SubscribeStatus.SUCCESS.getCode().equals(prev)) {
            redisUtils.hSet(statusKey, userIdStr, SubscribeStatus.SUBSCRIBED.getCode(), ttlSeconds, TimeUnit.SECONDS);
        }
        redisUtils.expire(statusKey, ttlSeconds);
    }

    @Override
    public void unsubscribe(VoucherSubscribeDTO voucherSubscribeDTO) {
        Long voucherId = voucherSubscribeDTO.getVoucherId();
        Long userId = UserHolder.getUser().getId();
        String userIdStr = String.valueOf(userId);

        String setKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_SUBSCRIBE_USER_TAG_KEY, voucherId);
        String zsetKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_SUBSCRIBE_ZSET_TAG_KEY, voucherId);
        String statusKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_SUBSCRIBE_STATUS_TAG_KEY, voucherId);

        // 从订阅集合与队列移除
        redisUtils.sRemove(setKey, userIdStr);
        redisUtils.zRemove(zsetKey, userIdStr);

        boolean purchased = Boolean.TRUE.equals(redisUtils.sIsMember(
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_USER_TAG_KEY, voucherId),
                userIdStr
        ));
        Long ttlSeconds = redisUtils.getExpire(
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_TAG_KEY, voucherId),
                TimeUnit.SECONDS
        );
        if (ttlSeconds == null || ttlSeconds <= 0) {
            ttlSeconds = 3600L;
        }
        redisUtils.hSet(
                statusKey,
                userIdStr,
                purchased ? SubscribeStatus.SUCCESS.getCode() : SubscribeStatus.UNSUBSCRIBED.getCode(),
                ttlSeconds, TimeUnit.SECONDS);
        redisUtils.expire(statusKey, ttlSeconds);
    }

    @Override
    public Integer getSubscribeStatus(VoucherSubscribeDTO voucherSubscribeDTO) {
        Long voucherId = voucherSubscribeDTO.getVoucherId();
        Long userId = UserHolder.getUser().getId();
        String userIdStr = String.valueOf(userId);

        String statusKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_SUBSCRIBE_STATUS_TAG_KEY, voucherId);
        Integer st = redisUtils.hGet(statusKey, userIdStr, Integer.class);
        if (st != null) {
            return st;
        }

        boolean purchased = Boolean.TRUE.equals(redisUtils.sIsMember(
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_USER_TAG_KEY, voucherId),
                userIdStr
        ));
        if (purchased) {
            Long ttlSeconds = redisUtils.getExpire(
                    RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_TAG_KEY, voucherId),
                    TimeUnit.SECONDS
            );
            if (ttlSeconds == null || ttlSeconds <= 0) {
                ttlSeconds = 3600L;
            }
            redisUtils.hSet(statusKey, userIdStr, SubscribeStatus.SUCCESS.getCode(), ttlSeconds, TimeUnit.SECONDS);
            redisUtils.expire(statusKey, ttlSeconds);
            return SubscribeStatus.SUCCESS.getCode();
        }

        boolean inQueue = Boolean.TRUE.equals(redisUtils.sIsMember(
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_SUBSCRIBE_USER_TAG_KEY, voucherId),
                userIdStr
        ));
        return inQueue ? SubscribeStatus.SUBSCRIBED.getCode() : SubscribeStatus.UNSUBSCRIBED.getCode();
    }

    @Override
    public List<GetSubscribeStatusVO> getSubscribeStatusBatch(VoucherSubscribeBatchDTO voucherSubscribeBatchDTO) {
        Long userId = UserHolder.getUser().getId();
        String userIdStr = String.valueOf(userId);
        List<GetSubscribeStatusVO> res = new ArrayList<>();
        for (Long voucherId : voucherSubscribeBatchDTO.getVoucherIdList()) {
            // 优先使用HASH缓存
            String statusKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_SUBSCRIBE_STATUS_TAG_KEY, voucherId);
            Integer st = redisUtils.hGet(statusKey, userIdStr, Integer.class);
            if (st != null) {
                res.add(new GetSubscribeStatusVO(voucherId, st));
                continue;
            }
            boolean purchased = Boolean.TRUE.equals(redisUtils.sIsMember(
                    RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_USER_TAG_KEY, voucherId),
                    userIdStr
            ));
            if (purchased) {
                Long ttlSeconds = redisUtils.getExpire(
                        RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_TAG_KEY, voucherId),
                        TimeUnit.SECONDS
                );
                if (ttlSeconds == null || ttlSeconds <= 0) {
                    ttlSeconds = 3600L;
                }
                redisUtils.hSet(statusKey, userIdStr, SubscribeStatus.SUCCESS.getCode(), ttlSeconds, TimeUnit.SECONDS);
                redisUtils.expire(statusKey, ttlSeconds);
                res.add(new GetSubscribeStatusVO(voucherId, SubscribeStatus.SUCCESS.getCode()));
                continue;
            }
            boolean inQueue = Boolean.TRUE.equals(redisUtils.sIsMember(
                    RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_SUBSCRIBE_USER_TAG_KEY, voucherId),
                    userIdStr
            ));
            res.add(new GetSubscribeStatusVO(voucherId, inQueue ? SubscribeStatus.SUBSCRIBED.getCode() : SubscribeStatus.UNSUBSCRIBED.getCode()));
        }
        return res;
    }

    @Override
    public void delayVoucherReminder(DelayVoucherReminderDTO delayVoucherReminderDTO) {
        SeckillVoucher seckillVoucher = seckillVoucherMapper.selectById(delayVoucherReminderDTO.getVoucherId());
        if (Objects.isNull(seckillVoucher)) {
            throw new qhdpFrameException(BaseCode.SECKILL_VOUCHER_NOT_EXIST);
        }
        DelayedVoucherReminderMessage msg = new DelayedVoucherReminderMessage(
                seckillVoucher.getVoucherId(),
                seckillVoucher.getBeginTime()
        );
        String content = JSON.toJSONString(msg);
        String topic = SpringUtil.getPrefixDistinctionName() + "-" + DELAY_VOUCHER_REMINDER;
        Integer delaySeconds = delayVoucherReminderDTO.getDelaySeconds();
        delayQueueContext.sendMessage(topic, content, delayVoucherReminderDTO.getDelaySeconds(), TimeUnit.SECONDS);
        log.info("[测试延迟发送] 已调度提醒消息 voucherId={} delaySeconds={} topic={}", seckillVoucher.getVoucherId(), delaySeconds, topic);
    }
}




