package com.qhdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qhdp.entity.UserInfo;
import com.qhdp.service.UserInfoService;
import com.qhdp.mapper.UserInfoMapper;
import org.springframework.stereotype.Service;

/**
* @author phoenix
* @description 针对表【tb_user_info】的数据库操作Service实现
* @createDate 2026-03-11 14:33:22
*/
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo>
    implements UserInfoService{

}




