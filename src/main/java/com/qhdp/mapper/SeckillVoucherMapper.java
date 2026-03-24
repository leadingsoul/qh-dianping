package com.qhdp.mapper;

import com.qhdp.entity.SeckillVoucher;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Update;

/**
* @author phoenix
* @description 针对表【tb_seckill_voucher(秒杀优惠券表，与优惠券是一对一关系)】的数据库操作Mapper
* @createDate 2026-03-11 14:32:01
* @Entity com.qhdp.entity.SeckillVoucher
*/
public interface SeckillVoucherMapper extends BaseMapper<SeckillVoucher> {

    @Update("UPDATE tb_seckill_voucher SET stock = stock + 1,update_time = NOW() WHERE voucher_id = #{voucherId}")
    int rollbackStock(Long voucherId);
}




