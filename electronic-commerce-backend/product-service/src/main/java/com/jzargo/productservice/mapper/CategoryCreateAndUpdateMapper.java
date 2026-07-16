package com.jzargo.productservice.mapper;

import com.jzargo.core.mapper.Mapper;
import com.jzargo.productservice.entity.AttributeType;
import com.jzargo.productservice.entity.Category;
import com.jzargo.productservice.model.CreateAndUpdateCategoryDetails;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CategoryCreateAndUpdateMapper implements Mapper<CreateAndUpdateCategoryDetails, Category> {

    @Override
    public Category map(CreateAndUpdateCategoryDetails from) {
        return Category.builder()
                .name(from.getName())
                .attributes(

                        from.getAttributes().entrySet()
                                .stream()
                                .map(entry ->
                                        new AbstractMap.SimpleEntry<>(
                                                entry.getKey(),
                                                AttributeType.valueOf( entry.getValue() )
                                        )
                                ).collect(
                                        Collectors.toMap(
                                                Map.Entry::getKey,
                                                Map.Entry::getValue
                                        )
                                )
                )
                .build();
    }
}
