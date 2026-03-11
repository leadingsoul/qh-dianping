package com.qhdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qhdp.dto.LoginFormDTO;
import com.qhdp.entity.User;
import com.qhdp.service.UserService;
import com.qhdp.mapper.UserMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

/**
* @author phoenix
* @description 针对表【tb_user】的数据库操作Service实现
* @createDate 2026-03-11 14:33:10
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Override
    public String sendCode(String phone, HttpSession session) {
        return "";
    }

    @Override
    public void login(LoginFormDTO loginForm, HttpSession session) {

    }

    @Override
    public void logout() {

    }
}




