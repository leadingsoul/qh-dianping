package com.qhdp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    /**
     * 保存店铺信息的方法
     * @param shop 需要保存的店铺对象，包含店铺的各类信息
     * @return 返回保存的店铺ID
     */
    Long saveShop(Shop shop);

    /**
     * 根据ID更新店铺信息
     * @param shop 包含更新后店铺信息的Shop对象，其中应包含要更新的店铺ID
     */
    void updateShopById(Shop shop);

    /**
     * 根据店铺类型分页查询店铺信息
     *
     * @param typeId 店铺类型ID，用于筛选特定类型的店铺
     * @param current 当前页码，用于分页查询
     * @return 返回一个包含Shop对象的Page集合，表示查询结果的分页数据
     */
    Page<Shop> queryShopByType(Integer typeId, Integer current, Double x, Double y);
}
