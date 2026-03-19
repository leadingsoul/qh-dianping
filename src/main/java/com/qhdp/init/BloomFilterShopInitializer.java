package com.qhdp.init;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qhdp.constant.Constant;
import com.qhdp.entity.Shop;
import com.qhdp.factory.BloomFilterHandlerFactory;
import com.qhdp.handler.BloomFilterHandler;
import com.qhdp.mapper.ShopMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 项目启动时，自动将所有ShopId加入布隆过滤器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BloomFilterShopInitializer implements CommandLineRunner {

    private final ShopMapper shopMapper;
    private final BloomFilterHandlerFactory bloomFilterHandlerFactory;

    @Override
    public void run(String... args) {
        try {
            // 获取商铺布隆过滤器
            BloomFilterHandler filterHandler = bloomFilterHandlerFactory.get(Constant.BLOOM_FILTER_HANDLER_SHOP);

            // 查询数据库所有有效商铺ID
            LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.select(Shop::getId); // 只查询ID，提升性能
            List<Shop> shopList = shopMapper.selectList(queryWrapper);

            // 批量添加到布隆过滤器
            int count = 0;
            for (Shop shop : shopList) {
                filterHandler.add(String.valueOf(shop.getId()));
                count++;
            }

            log.info("✅ 商铺布隆过滤器初始化完成，共加载 [{}] 个商铺ID", count);
        } catch (Exception e) {
            log.error("❌ 商铺布隆过滤器初始化失败", e);
        }
    }
}