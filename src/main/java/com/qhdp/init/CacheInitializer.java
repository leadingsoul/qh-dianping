package com.qhdp.init;

import com.qhdp.service.ShopTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class CacheInitializer implements ApplicationRunner {
    // 注入你的Service
    private final ShopTypeService shopTypeService;

    /**
     * Spring 容器完全启动成功后 才会执行这里
     */
    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("=== 开始初始化 店铺类型 缓存 ===");
            // 直接调用你原来的刷新方法
            shopTypeService.refreshShopTypeCache();
            log.info("=== 店铺类型 缓存初始化完成 ===");
        } catch (Exception e) {
            log.error("=== 店铺类型 缓存初始化失败 ===", e);
        }
    }
}
