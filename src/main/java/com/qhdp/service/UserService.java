package com.qhdp.service;

import com.qhdp.dto.LoginFormDTO;
import com.qhdp.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpSession;

/**
* @author phoenix
* @description 针对表【tb_user】的数据库操作Service
* @createDate 2026-03-11 14:33:10
*/
public interface UserService extends IService<User> {

    /**
     * 发送验证码到指定手机号
     * 
     * @param phone 接收验证码的手机号码
     * @param session 当前会话对象，用于存储验证码信息
     * @return 返回发送结果，可能是成功或失败的提示信息
     */
    String sendCode(String phone, HttpSession session);

    /**
     * 用户登录方法
     * @param loginForm 登录表单数据传输对象，包含用户名、密码等登录信息
     * @param session HTTP会话对象，用于保存用户登录状态等信息
     */
    void login(LoginFormDTO loginForm, HttpSession session);

    void logout();
}
