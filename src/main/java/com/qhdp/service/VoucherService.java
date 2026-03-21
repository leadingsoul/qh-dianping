package com.qhdp.service;

import com.qhdp.dto.Result;
import com.qhdp.dto.SeckillVoucherDTO;
import com.qhdp.dto.VoucherDTO;
import com.qhdp.entity.Voucher;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author phoenix
* @description 针对表【tb_voucher】的数据库操作Service
* @createDate 2026-03-11 14:33:43
*/
public interface VoucherService extends IService<Voucher> {



    Long addSeckillVoucher(SeckillVoucherDTO voucherDTO);

    List<Voucher> queryVoucherOfShop(Long shopId);

    Long addVoucher(VoucherDTO voucherDTO);
}
