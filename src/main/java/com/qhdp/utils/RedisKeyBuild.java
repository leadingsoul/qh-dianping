package com.qhdp.utils;

import com.qhdp.constant.RedisKeyManage;
import lombok.Getter;

import java.util.Objects;

/**
 * @description: redis key包装
 * @author: 阿星不是程序员
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
     * 构建真实的key
     * @param level 占位符的值
     * */
    public static String buildSeckillUserLevelKey(Integer level){
        // 第一步：格式化占位符
        String formattedKey = String.format(RedisKeyManage.SECKILL_USER_LEVEL_MEMBERS_TAG_KEY.getKey(), level);
        // 第二步：拼接环境前缀（最关键！）
        return SpringUtil.getPrefixDistinctionName() + "-" + formattedKey;
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
