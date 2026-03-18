package com.qhdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.qhdp.dto.UserDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.qhdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.qhdp.utils.RedisConstants.LOGIN_USER_TTL;

@Slf4j
@RequiredArgsConstructor
public class RefreshTokenInterceptor implements HandlerInterceptor {


    private final RedisUtils redisUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1.获取请求头中的token
        String token = request.getHeader("authorization");
        // token为空，说明用户未登录，直接放行
        if (StrUtil.isBlank(token)) {
            return true;
        }
        // 2.基于TOKEN获取redis中的用户
        String key  = LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = redisUtils.hGetAll(key);
        // 3.判断用户是否存在
        //用户不存在，说明用户未登录，直接放行
        if (userMap.isEmpty()) {
            return true;
        }
        //用户存在，说明用户已登录，刷新token有效期
        // 5.将查询到的hash数据转为UserDTO
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        // 6.存在，保存用户信息到 ThreadLocal
        UserHolder.saveUser(userDTO);
        // 7.刷新token有效期（按秒设置，避免 Redisson pExpire 递归问题）
        redisUtils.expire(
                key,
                TimeUnit.SECONDS.convert(LOGIN_USER_TTL, TimeUnit.MINUTES)
        );
        // 8.放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        try {
            // 安全移除用户（ThreadLocal 必须清理）
            UserHolder.removeUser();
        } catch (Exception e) {
            // 只打印日志，不抛出异常，避免影响主请求
            log.error("清理用户信息失败", e);
        }
    }
}
