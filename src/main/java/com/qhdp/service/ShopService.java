package com.qhdp.service;

import com.qhdp.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author phoenix
* @description 针对表【tb_shop】的数据库操作Service
* @createDate 2026-03-11 14:32:16
*/
public interface ShopService extends IService<Shop> {


    /**
     * 根据店铺ID查询店铺信息
     * @param id 店铺ID
     * @return 返回对应的Shop对象，如果未找到则返回null
     */
    Shop queryShopById(Long id);
}
