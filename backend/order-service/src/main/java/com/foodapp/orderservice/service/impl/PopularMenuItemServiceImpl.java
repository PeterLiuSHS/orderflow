package com.foodapp.orderservice.service.impl;

import com.foodapp.orderservice.exception.BadRequestException;
import com.foodapp.orderservice.repository.MenuItemPopularityRepository;
import com.foodapp.orderservice.service.PopularMenuItemService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class PopularMenuItemServiceImpl implements PopularMenuItemService {

    private final MenuItemPopularityRepository repository;

    public PopularMenuItemServiceImpl(MenuItemPopularityRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Long> getPopularMenuItemIds(int limit){

        if (limit<1 || limit>20){
            throw new BadRequestException("Limit must be between 1 and 20.");
        }

        Set<String> values = repository.getTopMenuItemIds(limit);

        List<Long> result = new ArrayList<>();

        if (values == null){
            return result;
        }

        for (String value : values){
            result.add(Long.valueOf(value));
        }

        return result;
    }
}
