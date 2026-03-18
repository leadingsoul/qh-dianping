package com.qhdp.controller;


import com.qhdp.dto.Result;
import com.qhdp.entity.ShopType;
import com.qhdp.service.ShopTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/shop-type")
public class ShopTypeController {

    private final ShopTypeService shopTypeService;

    @GetMapping("list")
    public Result<List<ShopType>> queryTypeList() {
        List<ShopType> typeList = shopTypeService.queryAllTypeList();
        return Result.success(typeList);
    }
}
