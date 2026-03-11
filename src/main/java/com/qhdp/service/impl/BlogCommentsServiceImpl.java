package com.qhdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qhdp.entity.BlogComments;
import com.qhdp.service.BlogCommentsService;
import com.qhdp.mapper.BlogCommentsMapper;
import org.springframework.stereotype.Service;

/**
* @author phoenix
* @description 针对表【tb_blog_comments】的数据库操作Service实现
* @createDate 2026-03-11 14:31:00
*/
@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments>
    implements BlogCommentsService{

}




