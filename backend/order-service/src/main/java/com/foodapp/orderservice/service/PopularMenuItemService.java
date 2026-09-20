package com.foodapp.orderservice.service;

import java.util.List;

public interface PopularMenuItemService {

    List<Long> getPopularMenuItemIds(int limit);
}
