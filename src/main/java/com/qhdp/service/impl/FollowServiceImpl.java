package com.qhdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qhdp.dto.UserDTO;
import com.qhdp.entity.Follow;
import com.qhdp.entity.User;
import com.qhdp.mapper.UserMapper;
import com.qhdp.service.FollowService;
import com.qhdp.mapper.FollowMapper;
import com.qhdp.toolkit.SnowflakeIdGenerator;
import com.qhdp.utils.RedisUtils;
import com.qhdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author phoenix
* &#064;description  针对表【tb_follow】的数据库操作Service实现
* &#064;createDate  2026-03-11 14:31:24
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow>
    implements FollowService{

    private final RedisUtils redisUtils;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final UserMapper userMapper;

    @Override
    public void follow(Long followUserId, Boolean isFollow) {
        // 1.获取登录用户
        Long userId = UserHolder.getUser().getId();
        String key = "follows:" + userId;
        // 1.判断到底是关注还是取关
        if (isFollow) {
            // 2.关注，新增数据
            Follow follow = new Follow();
            follow.setId(snowflakeIdGenerator.nextId());
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean isSuccess = save(follow);
            if (isSuccess) {
                // 把关注用户的id，放入redis的set集合 sadd userId followerUserId
                redisUtils.sAdd(key, followUserId.toString());
            }
        } else {
            // 3.取关，删除 delete from tb_follow where user_id = ? and follow_user_id = ?
            boolean isSuccess = remove(new QueryWrapper<Follow>()
                    .eq("user_id", userId).eq("follow_user_id", followUserId));
            if (isSuccess) {
                // 把关注用户的id从Redis集合中移除
                redisUtils.sRemove(key, followUserId.toString());
            }
        }
    }

    @Override
    public boolean isFollow(Long followUserId) {
        // 1.获取登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.查询是否关注 select count(*) from tb_follow where user_id = ? and follow_user_id = ?
        Long count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
        // 3.判断
        return count > 0;
    }

    /**
    * 查询关注的公共用户
     * @return 共同关注的用户
     */
    @Override
    public List<UserDTO> followCommons(Long id) {
        // 1.获取当前用户
        Long userId = UserHolder.getUser().getId();
        String key = "follows:" + userId;
        // 2.求交集
        String key2 = "follows:" + id;
        Set<String> intersect = redisUtils.sIntersect(key, key2, String.class);
        if (intersect == null || intersect.isEmpty()) {
            // 无交集
            return Collections.emptyList();
        }
        // 3.解析id集合
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        // 4.查询用户
        return userMapper.selectList(
                        new LambdaQueryWrapper<User>()
                                .in(User::getId, ids)  // 核心：id IN (ids集合)
                ).stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
    }
}




