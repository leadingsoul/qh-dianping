package com.qhdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qhdp.entity.ShopType;
import com.qhdp.service.ShopTypeService;
import com.qhdp.mapper.ShopTypeMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author phoenix
* @description 针对表【tb_shop_type】的数据库操作Service实现
* @createDate 2026-03-11 14:32:28
*/
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType>
    implements ShopTypeService{

    @Override
    public List<ShopType> queryAllTypeList() {
        return list(new LambdaQueryWrapper<ShopType>()
                .orderByAsc(ShopType::getSort));
    }
}




