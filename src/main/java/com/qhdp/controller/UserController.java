package com.qhdp.controller;


import com.qhdp.dto.LoginFormDTO;
import com.qhdp.dto.Result;
import com.qhdp.entity.User;
import com.qhdp.entity.UserInfo;
import com.qhdp.service.UserInfoService;
import com.qhdp.service.UserService;
import com.qhdp.utils.RegexUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    private final UserInfoService userInfoService;

    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    public Result<Void> sendCode(@RequestParam("phone") String phone, HttpSession session) {
        String code = userService.sendCode(phone,session);
        return Result.success("成功向手机号:"+phone+"发送验证码："+code);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    public Result<Void> login(@RequestBody LoginFormDTO loginForm, HttpSession session){
        userService.login(loginForm,session);
        return Result.success("登录成功");
    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    public Result<Void> logout(){
        userService.logout();
        return Result.success("登出成功");
    }

    @GetMapping("/me")
    public Result<User> me(HttpSession session){
        User user = (User) session.getAttribute("user");
        return Result.success(user);
    }

    @GetMapping("/info/{id}")
    public Result<UserInfo> info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.success(new UserInfo());
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.success(info);
    }
}
