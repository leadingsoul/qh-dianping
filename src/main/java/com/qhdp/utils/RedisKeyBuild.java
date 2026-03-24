package com.qhdp.utils;

import com.qhdp.enums.RedisKeyManage;
import lombok.Getter;

import java.util.Objects;

/**
 * @description: redis key包装
 * @author: phoenix
 **/
@Getter

public final class RedisKeyBuild {
    /**
     * 实际使用的key
     * */
    private final String relKey;

    private RedisKeyBuild(String relKey) {
        this.relKey = relKey;
    }


    /**
     * 创建Redis键的静态工厂方法
     * @param redisKeyManage Redis键管理对象，包含键的模板信息
     * @param args 可变参数，用于格式化Redis键模板中的占位符
     * @return 返回一个String字符串，包含完整的Redis键
     */
    public static String createRedisKey(RedisKeyManage redisKeyManage, Object... args){
    // 使用String.format方法根据传入的参数格式化Redis键模板
        String redisRelKey = String.format(redisKeyManage.getKey(),args);
    // 创建并返回String字符串，拼接前缀和格式化后的键
        return SpringUtil.getPrefixDistinctionName() + "-" + redisRelKey;
    }

    /**
     * 创建Redis键的静态工厂方法
     * @param redisKeyManage Redis键管理对象，包含键的模板信息
     * @return 返回一个String字符串，包含完整的Redis键
     */
    public static String createRedisKey(RedisKeyManage redisKeyManage){
        // 使用String.format方法根据传入的参数格式化Redis键模板
        String redisRelKey = String.format(redisKeyManage.getKey());
        // 创建并返回String字符串，拼接前缀和格式化后的键
        return SpringUtil.getPrefixDistinctionName() + "-" + redisRelKey;
    }
    
    public static String getRedisKey(RedisKeyManage redisKeyManage) {
        return SpringUtil.getPrefixDistinctionName() + "-" + redisKeyManage.getKey();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RedisKeyBuild that = (RedisKeyBuild) o;
        return relKey.equals(that.relKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relKey);
    }
}
