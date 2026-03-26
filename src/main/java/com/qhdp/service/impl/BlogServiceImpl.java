package com.qhdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qhdp.dto.ScrollResult;
import com.qhdp.dto.UserDTO;
import com.qhdp.entity.Blog;
import com.qhdp.entity.User;
import com.qhdp.mapper.UserMapper;
import com.qhdp.service.BlogService;
import com.qhdp.mapper.BlogMapper;
import com.qhdp.service.UserService;
import com.qhdp.utils.RedisUtils;
import com.qhdp.utils.SystemConstants;
import com.qhdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.qhdp.constant.RedisConstants.BLOG_LIKED_KEY;
import static com.qhdp.constant.RedisConstants.FEED_KEY;

/**
* @author phoenix
* @description 针对表【tb_blog】的数据库操作Service实现
* @createDate 2026-03-11 14:29:34
*/
@RequiredArgsConstructor
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog>
    implements BlogService{

    private final BlogMapper blogMapper;
    private final UserMapper userMapper;
    private final RedisUtils redisUtils;

    @Override
    public void saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        save(blog);
    }

    @Override
    public void increaseLike(Long id) {
        LambdaUpdateWrapper<Blog> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Blog::getId, id).setSql("like_count = like_count + 1");
         int rows = blogMapper.update(null, updateWrapper);
         if(rows <= 0){
             throw new RuntimeException("点赞失败,请稍后再试");
         }
    }

    @Override
    @Transactional
    public List<Blog> queryMyBlog(Integer current) {
        // 1. 获取登录用户
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            throw new RuntimeException("请先登录");
        }
        // 2. 构建分页对象（当前页、每页条数）
        Page<Blog> page = new Page<>(current, SystemConstants.MAX_PAGE_SIZE);
        LambdaQueryWrapper<Blog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Blog::getUserId, user.getId()); // 等于当前用户ID
        page(page, wrapper);
        return page.getRecords();
    }

    @Override
    @Transactional
    public List<Blog> queryHotBlog(Integer current) {
        // 1. 构建分页对象
        Page<Blog> page = new Page<>(current, SystemConstants.MAX_PAGE_SIZE);
        // 2. MP 标准条件：按点赞数降序
        LambdaQueryWrapper<Blog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Blog::getLiked);
        // 3. 执行分页查询
        page(page, wrapper);
        // 4. 为博客填充作者信息（昵称、头像）
        List<Blog> records = page.getRecords();
        records.forEach(blog -> {
            User user = userMapper.selectById(blog.getUserId());
            if (user != null) {
                blog.setName(user.getNickName());
                blog.setIcon(user.getIcon());
            }
        });
        return records;
    }

    @Override
    public Blog queryBlogById(Long id) {
        // 1.查询blog
        Blog blog = getById(id);
        if (blog == null) {
            throw new RuntimeException("笔记不存在！");
        }
        // 2.查询blog有关的用户
        queryBlogUser(blog);
        // 3.查询blog是否被点赞
        isBlogLiked(blog);
        return blog;
    }

    @Override
    public List<UserDTO> queryBlogLikes(Long id) {
        String key = BLOG_LIKED_KEY + id;
        // 1.查询top5的点赞用户 zrange key 0 4
        Set<String> top5 = redisUtils.zRange(key, 0, 4, String.class);
        if (top5 == null || top5.isEmpty()) {
            return Collections.emptyList();
        }
        // 2.解析出其中的用户id
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        String idStr = StrUtil.join(",", ids);
        // 3.根据用户id查询用户 WHERE id IN ( 5 , 1 ) ORDER BY FIELD(id, 5, 1)
        // 4.返回
        return userMapper.selectList(
                        new QueryWrapper<User>()
                                .in("id", ids)
                                .last("ORDER BY FIELD(id," + idStr + ")")
                ).stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public ScrollResult queryBlogOfFollow(Long max, Integer offset) {
        // 1.获取当前用户
        Long userId = UserHolder.getUser().getId();
        // 2.查询收件箱 ZREVRANGEBYSCORE key Max Min LIMIT offset count
        String key = FEED_KEY + userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = redisUtils.zRangeByScoreWithScore(key,0,max,offset,2,String.class);
        // 3.非空判断
        if (typedTuples == null || typedTuples.isEmpty()) {
            return new ScrollResult();
        }
        // 4.解析数据：blogId、minTime（时间戳）、offset
        List<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime = 0; // 2
        int os = 1; // 2
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) { // 5 4 4 2 2
            // 4.1.获取id
            ids.add(Long.valueOf(Objects.requireNonNull(tuple.getValue())));
            // 4.2.获取分数(时间戳）
            long time = Objects.requireNonNull(tuple.getScore()).longValue();
            if(time == minTime){
                os++;
            }else{
                minTime = time;
                os = 1;
            }
        }

        // 5.根据id查询blog
        String idStr = StrUtil.join(",", ids);
        List<Blog> blogs = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();

        for (Blog blog : blogs) {
            // 5.1.查询blog有关的用户
            queryBlogUser(blog);
            // 5.2.查询blog是否被点赞
            isBlogLiked(blog);
        }
        // 6.封装并返回
        ScrollResult r = new ScrollResult();
        r.setList(blogs);
        r.setOffset(os);
        r.setMinTime(minTime);
        return r;
    }
    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在！");
        }
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
    private void isBlogLiked(Blog blog) {
        // 1.获取登录用户
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            // 用户未登录，无需查询是否点赞
            return;
        }
        Long userId = user.getId();
        // 2.判断当前登录用户是否已经点赞
        String key = "blog:liked:" + blog.getId();
        Double score = redisUtils.zScore(key, userId.toString());
        blog.setIsLike(score != null);
    }
}




