package com.qhdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qhdp.entity.UserPhone;
import com.qhdp.service.UserPhoneService;
import com.qhdp.mapper.UserPhoneMapper;
import org.springframework.stereotype.Service;

/**
* @author phoenix
* @description 针对表【tb_user_phone(用户手机表)】的数据库操作Service实现
* @createDate 2026-03-11 14:33:32
*/
@Service
public class UserPhoneServiceImpl extends ServiceImpl<UserPhoneMapper, UserPhone>
    implements UserPhoneService{

}




