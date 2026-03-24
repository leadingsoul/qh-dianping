package com.qhdp.service;

import com.qhdp.dto.Result;
import com.qhdp.dto.ScrollResult;
import com.qhdp.dto.UserDTO;
import com.qhdp.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author phoenix
* @description 针对表【tb_blog】的数据库操作Service
* @createDate 2026-03-11 14:29:34
*/
public interface BlogService extends IService<Blog> {

    void saveBlog(Blog blog);

    void increaseLike(Long id);

    List<Blog> queryMyBlog(Integer current);

    List<Blog> queryHotBlog(Integer current);

    Blog queryBlogById(Long id);

    List<UserDTO> queryBlogLikes(Long id);

    ScrollResult queryBlogOfFollow(Long max, Integer offset);
}
