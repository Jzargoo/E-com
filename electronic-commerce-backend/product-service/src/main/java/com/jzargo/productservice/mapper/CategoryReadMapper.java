package com.jzargo.productservice.mapper;

import com.jzargo.core.mapper.Mapper;
import com.jzargo.productservice.entity.Category;
import com.jzargo.productservice.model.CategoryDetails;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CategoryReadMapper implements Mapper<Category, CategoryDetails> {
    @Override
    public CategoryDetails map(Category from) {
        return CategoryDetails.builder()
                .attributes(
                        from.getAttributes().entrySet().stream()
                                .map((entry) -> {
                                    var entryValue = entry.getValue().toString();

                                    return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entryValue);
                                })
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        Map.Entry::getValue
                                ))
                )
                .name(from.getName())
                .id(from.getId())
                .build();
    }
}
