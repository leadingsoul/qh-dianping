package com.qhdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qhdp.entity.SeckillVoucher;
import com.qhdp.service.SeckillVoucherService;
import com.qhdp.mapper.SeckillVoucherMapper;
import org.springframework.stereotype.Service;

/**
* @author phoenix
* @description 针对表【tb_seckill_voucher(秒杀优惠券表，与优惠券是一对一关系)】的数据库操作Service实现
* @createDate 2026-03-11 14:32:01
*/
@Service
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher>
    implements SeckillVoucherService{

}




