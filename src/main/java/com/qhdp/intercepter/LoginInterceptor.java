package com.qhdp.intercepter;

import com.qhdp.dto.UserDTO;
import com.qhdp.utils.UserHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器，用于处理用户登录状态验证
 * 实现了HandlerInterceptor接口，用于在请求处理前后进行拦截处理
 */
@Slf4j
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (UserHolder.getUser() == null) {
            // 没有，需要拦截，设置状态码
            response.setStatus(401);
            // 拦截
            return false;
        }
        // 有用户，则放行
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
