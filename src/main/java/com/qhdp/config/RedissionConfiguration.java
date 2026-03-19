package com.qhdp.config;

import com.qhdp.constant.LockInfoType;
import com.qhdp.factory.LockInfoHandleFactory;
import com.qhdp.factory.ServiceLockFactory;
import com.qhdp.handler.handlerInterface.LockInfoHandle;
import com.qhdp.servicelocker.ManageLocker;
import com.qhdp.servicelocker.aspect.ServiceLockAspect;
import com.qhdp.servicelocker.info.ServiceLockInfoHandle;
import com.qhdp.utils.ServiceLockTool;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissionConfiguration {
    // 从 application.yml 中读取 Redis 配置
    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.database}")
    private int database;
    /**
     * 注册 RedissonClient 到 Spring 容器
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        // 单机Redis配置（集群/哨兵模式修改此处即可）
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(database);

        // 创建并返回 RedissonClient 实例
        return Redisson.create(config);
    }

    @Bean(LockInfoType.SERVICE_LOCK)
    public LockInfoHandle serviceLockInfoHandle(){
        return new ServiceLockInfoHandle();
    }

    @Bean
    public ManageLocker manageLocker(RedissonClient redissonClient){

        return new ManageLocker(redissonClient);
    }

    @Bean
    public ServiceLockFactory serviceLockFactory(ManageLocker manageLocker){
        return new ServiceLockFactory(manageLocker);
    }

    @Bean
    public ServiceLockAspect serviceLockAspect(LockInfoHandleFactory lockInfoHandleFactory, ServiceLockFactory serviceLockFactory){
        return new ServiceLockAspect(lockInfoHandleFactory,serviceLockFactory);
    }

    @Bean
    public ServiceLockTool serviceLockTooL(LockInfoHandleFactory lockInfoHandleFactory, ServiceLockFactory serviceLockFactory){
        return new ServiceLockTool(lockInfoHandleFactory,serviceLockFactory);
    }
}
