package com.qhdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qhdp.dto.UserDTO;
import com.qhdp.entity.Blog;
import com.qhdp.entity.User;
import com.qhdp.service.BlogService;
import com.qhdp.mapper.BlogMapper;
import com.qhdp.service.UserService;
import com.qhdp.utils.SystemConstants;
import com.qhdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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
    private final UserService userService;

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
            User user = userService.getById(blog.getUserId());
            if (user != null) {
                blog.setName(user.getNickName());
                blog.setIcon(user.getIcon());
            }
        });
        return records;
    }
}




