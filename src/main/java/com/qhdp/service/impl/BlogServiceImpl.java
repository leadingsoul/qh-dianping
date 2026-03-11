package com.qhdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qhdp.entity.Blog;
import com.qhdp.service.BlogService;
import com.qhdp.mapper.BlogMapper;
import org.springframework.stereotype.Service;

/**
* @author phoenix
* @description 针对表【tb_blog】的数据库操作Service实现
* @createDate 2026-03-11 14:29:34
*/
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog>
    implements BlogService{

}




