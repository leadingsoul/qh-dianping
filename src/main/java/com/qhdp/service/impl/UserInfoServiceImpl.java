package com.qhdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qhdp.entity.UserInfo;
import com.qhdp.enums.BaseCode;
import com.qhdp.enums.RedisKeyManage;
import com.qhdp.exception.qhdpFrameException;
import com.qhdp.service.UserInfoService;
import com.qhdp.mapper.UserInfoMapper;
import com.qhdp.servicelocker.LockType;
import com.qhdp.annotation.ServiceLock;
import com.qhdp.utils.RedisKeyBuild;
import com.qhdp.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static com.qhdp.constant.DistributedLockConstants.UPDATE_USER_INFO_LOCK;

/**
* @author phoenix
* @description 针对表【tb_user_info】的数据库操作Service实现
* @createDate 2026-03-11 14:33:22
*/
@Service
@RequiredArgsConstructor
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo>
    implements UserInfoService{

    private final RedisUtils redisUtils;

    @Override
    @ServiceLock(lockType= LockType.Read,name = UPDATE_USER_INFO_LOCK,keys = {"#userId"})
    public UserInfo getByUserId(Long userId){
        UserInfo userInfo = redisUtils.get(RedisKeyBuild.createRedisKey(RedisKeyManage.USER_INFO_KEY, userId), UserInfo.class);
        if (Objects.nonNull(userInfo)){
            return userInfo;
        }
        userInfo = lambdaQuery().eq(UserInfo::getUserId, userId).one();
        if (Objects.isNull(userInfo)) {
            throw new qhdpFrameException(BaseCode.USER_NOT_EXIST);
        }
        redisUtils.set(RedisKeyBuild.createRedisKey(RedisKeyManage.USER_INFO_KEY, userId), userInfo);
        return userInfo;
    }
}




