package com.qhdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qhdp.entity.Sign;
import com.qhdp.service.SignService;
import com.qhdp.mapper.SignMapper;
import org.springframework.stereotype.Service;

/**
* @author phoenix
* @description 针对表【tb_sign】的数据库操作Service实现
* @createDate 2026-03-11 14:32:38
*/
@Service
public class SignServiceImpl extends ServiceImpl<SignMapper, Sign>
    implements SignService{

}




