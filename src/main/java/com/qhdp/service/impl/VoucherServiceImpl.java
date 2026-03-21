package com.qhdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qhdp.constant.RedisKeyManage;
import com.qhdp.dto.SeckillVoucherDTO;
import com.qhdp.dto.VoucherDTO;
import com.qhdp.entity.SeckillVoucher;
import com.qhdp.entity.Voucher;
import com.qhdp.factory.BloomFilterHandlerFactory;
import com.qhdp.service.SeckillVoucherService;
import com.qhdp.service.VoucherService;
import com.qhdp.mapper.VoucherMapper;
import com.qhdp.toolkit.SnowflakeIdGenerator;
import com.qhdp.utils.RedisKeyBuild;
import com.qhdp.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.qhdp.constant.Constant.BLOOM_FILTER_HANDLER_VOUCHER;

/**
* @author phoenix
* @description 针对表【tb_voucher】的数据库操作Service实现
* @createDate 2026-03-11 14:33:43
*/
@RequiredArgsConstructor
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher>
    implements VoucherService{

    private final SeckillVoucherService seckillVoucherService;

    private final BloomFilterHandlerFactory bloomFilterHandlerFactory;

    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final RedisUtils redisUtils;

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
        seckillVoucherService.save(seckillVoucher);
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
}




