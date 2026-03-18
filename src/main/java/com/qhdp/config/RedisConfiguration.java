package com.qhdp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Slf4j
@Configuration
public class RedisConfiguration {
    // Redis配置类
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringRedisSerializer = StringRedisSerializer.UTF_8;
        GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
        //设置key的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        //设置value的序列化方式
        template.setValueSerializer(genericJackson2JsonRedisSerializer);
        //设置hash key的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        //设置hash value的序列化方式
        template.setHashValueSerializer(genericJackson2JsonRedisSerializer);
        log.info("RedisTemplate初始化成功...");
        return template;
    }
}
