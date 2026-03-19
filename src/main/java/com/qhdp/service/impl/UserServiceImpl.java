package com.qhdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qhdp.dto.LoginFormDTO;
import com.qhdp.constant.RedisKeyManage;
import com.qhdp.dto.UserDTO;
import com.qhdp.entity.User;
import com.qhdp.entity.UserInfo;
import com.qhdp.entity.UserPhone;
import com.qhdp.service.UserInfoService;
import com.qhdp.service.UserPhoneService;
import com.qhdp.service.UserService;
import com.qhdp.mapper.UserMapper;
import com.qhdp.toolkit.SnowflakeIdGenerator;
import com.qhdp.utils.RedisKeyBuild;
import com.qhdp.utils.RedisUtils;
import com.qhdp.utils.RegexUtils;
import com.qhdp.utils.UserHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.qhdp.constant.RedisConstants.*;
import static com.qhdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
* @author phoenix
* @description 针对表【tb_user】的数据库操作Service实现
* @createDate 2026-03-11 14:33:10
*/
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    private final UserPhoneService userPhoneService;
    private final RedisUtils redisUtils;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final UserInfoService userInfoService;

    @Override
    public String sendCode(String phone, HttpSession session) {
        // 1.校验手机号格式
        if (RegexUtils.isPhoneInvalid(phone)) {
            throw new RuntimeException("手机号格式错误！");
        }
        // 2.生成6位数字验证码
        String code = RandomUtil.randomNumbers(6);
        // 3.将验证码存入Redis，并设置60秒过期时间
        // key = login:code:13800138000
        redisUtils.set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL*30);
        // 4.打印日志
        log.info("发送短信验证码成功，手机号：{}，验证码：{}", phone, code);
        // 5.返回验证码
        return code;
    }

    @Override
    public String login(LoginFormDTO loginForm, HttpSession session) {
        // 1.校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2.如果不符合，返回错误信息
            throw new RuntimeException("手机号格式错误！");
        }
        // 3.校验验证码,从redis中获得验证码
        Object cacheCode = redisUtils.get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if(cacheCode == null || !cacheCode.toString().equals(code)){
            //3.不一致，报错
            throw new RuntimeException("验证码错误！");
        }
        //一致，根据手机号查询用户
        User user = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone));
        //5.判断用户是否存在
        if(user == null){
            //不存在，则创建
            user =  createUserWithPhone(phone);
        }
        //6.保存用户信息到redis中
        String token = UUID.randomUUID().toString();
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName,fieldValue) -> fieldValue != null ? fieldValue.toString() : null));
        // 存储
        String tokenKey = LOGIN_USER_KEY + token;
        redisUtils.hSetAll(tokenKey,userMap);
        // 设置token有效期
        redisUtils.expire(tokenKey,LOGIN_USER_TTL*5);
        return token;
    }

    private User createUserWithPhone(String phone) {
        //创建用户
        User user = new User();
        user.setId(snowflakeIdGenerator.nextId());
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        // 3.保存用户信息
        UserInfo userInfo = new UserInfo();
        userInfo.setId(snowflakeIdGenerator.nextId());
        userInfo.setUserId(user.getId());
        userInfo.setLevel(1);
        userInfoService.save(userInfo);
        try {
            maintainLevelSetMembership(user.getId());
        } catch (Exception e) {
            // 忽略异常，避免影响注册逻辑
        }
        // 2. 同步保存到 UserPhone 表
        UserPhone userPhone = new UserPhone();
        userPhone.setId(snowflakeIdGenerator.nextId());
        userPhone.setUserId(user.getId()); // 关联用户ID
        userPhone.setPhone(phone);
        // 保存手机号表
        userPhoneService.save(userPhone);
        return user;
    }

    private void maintainLevelSetMembership(Long userId) {
        if (userId == null) {
            return;
        }
        UserInfo info = userInfoService.lambdaQuery().eq(UserInfo::getUserId, userId).one();
        if (info == null || info.getLevel() == null || info.getLevel() <= 0) {
            return;
        }
        Integer level = info.getLevel();
        redisUtils.sAdd(
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_USER_LEVEL_MEMBERS_TAG_KEY, level),
                userId
        );
    }

    @Override
    public void logout(HttpServletRequest request) {
        UserDTO user = UserHolder.getUser();
        if (user != null) {
            String token = getTokenFromRequest(request); // 需要从请求中获取token
            if (token != null) {
                redisUtils.delete(LOGIN_USER_KEY + token);
            }
            UserHolder.removeUser();
        }
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        return request.getHeader("authorization");
    }
}




