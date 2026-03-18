package com.qhdp.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qhdp.dto.Result;
import com.qhdp.dto.UserDTO;
import com.qhdp.entity.Blog;
import com.qhdp.entity.User;
import com.qhdp.service.BlogService;
import com.qhdp.service.UserService;
import com.qhdp.utils.SystemConstants;
import com.qhdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/blog")
public class BlogController {

    private final BlogService blogService;
    private final UserService userService;

    @PostMapping
    public Result<Long> saveBlog(@RequestBody Blog blog) {
        blogService.saveBlog(blog);
        // 返回id
        return Result.success(blog.getId());
    }

    @PutMapping("/like/{id}")
    public Result<Void> likeBlog(@PathVariable("id") Long id) {
        // 修改点赞数量
        blogService.increaseLike(id);
        return Result.success("点赞成功");
    }

    @GetMapping("/of/me")
    public Result<List<Blog>> queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        List<Blog> blogs = blogService.queryMyBlog(current);
        return Result.success(blogs);
    }

    @GetMapping("/hot")
    public Result<List<Blog>> queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 根据用户查询
        List<Blog> blogs = blogService.queryHotBlog(current);
        return Result.success(blogs);
    }
}
