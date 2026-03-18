package com.qhdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qhdp.entity.SeckillVoucher;
import com.qhdp.entity.Voucher;
import com.qhdp.service.SeckillVoucherService;
import com.qhdp.service.VoucherService;
import com.qhdp.mapper.VoucherMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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

    @Override
    public void addSeckillVoucher(Voucher voucher) {
        // 保存优惠券
        save(voucher);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);
    }

    @Override
    public List<Voucher> queryVoucherOfShop(Long shopId) {
        return list(new LambdaQueryWrapper<Voucher>()
                .eq(Voucher::getShopId, shopId));
    }

    @Override
    public void saveVoucher(Voucher voucher) {
        save(voucher);
    }
}




