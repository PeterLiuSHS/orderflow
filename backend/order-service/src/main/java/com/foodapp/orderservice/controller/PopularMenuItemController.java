package com.foodapp.orderservice.controller;

import com.foodapp.orderservice.service.PopularMenuItemService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/popular")
public class PopularMenuItemController {

    private final PopularMenuItemService popularMenuItemService;

    public PopularMenuItemController(
            PopularMenuItemService popularMenuItemService
    ) {
        this.popularMenuItemService = popularMenuItemService;
    }

    @GetMapping("/menu-items")
    public List<Long> getPopularMenuItems(
            @RequestParam(defaultValue = "5") int limit
    ) {
        return popularMenuItemService
                .getPopularMenuItemIds(limit);
    }
}