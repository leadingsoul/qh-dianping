package com.qhdp.service;

import com.qhdp.dto.UserDTO;
import com.qhdp.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author phoenix
* @description 针对表【tb_follow】的数据库操作Service
* @createDate 2026-03-11 14:31:24
*/
public interface FollowService extends IService<Follow> {

    List<UserDTO> followCommons(Long id);

    boolean isFollow(Long followUserId);

    void follow(Long followUserId, Boolean isFollow);
}
