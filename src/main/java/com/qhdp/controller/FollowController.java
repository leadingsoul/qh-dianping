package com.qhdp.controller;


import com.qhdp.dto.Result;
import com.qhdp.dto.UserDTO;
import com.qhdp.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/follow")
public class FollowController {
    private final FollowService followService;

    @PutMapping("/{id}/{isFollow}")
    public Result<Void> follow(@PathVariable("id") Long followUserId, @PathVariable Boolean isFollow) {
        followService.follow(followUserId, isFollow);
        return Result.success("操作成功");
    }

    @GetMapping("/or/not/{id}")
    public Result<Boolean> isFollow(@PathVariable("id") Long followUserId) {

        return Result.success(followService.isFollow(followUserId));
    }

    @GetMapping("/common/{id}")
    public Result<List<UserDTO>> followCommons(@PathVariable Long id) {
        return Result.success(followService.followCommons(id));
    }
}
