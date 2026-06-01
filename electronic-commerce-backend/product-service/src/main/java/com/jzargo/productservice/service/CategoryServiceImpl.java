package com.jzargo.productservice.service;

import com.jzargo.productservice.entity.Category;
import com.jzargo.productservice.exception.MalformedDataError;
import com.jzargo.productservice.mapper.CategoryCreateAndUpdateMapper;
import com.jzargo.productservice.mapper.CategoryReadMapper;
import com.jzargo.productservice.model.CategoryDetails;
import com.jzargo.productservice.model.CreateAndUpdateCategoryDetails;
import com.jzargo.productservice.repository.CategoryRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Data
@Slf4j
@Service
public class CategoryServiceImpl implements CategoryService{

    private final CategoryRepository categoryRepository;
    private final CategoryCreateAndUpdateMapper categoryCreateAndUpdateMapper;
    private final CategoryReadMapper categoryReadMapper;

    public CategoryServiceImpl(CategoryRepository categoryRepository, CategoryCreateAndUpdateMapper categoryCreateAndUpdateMapper, CategoryReadMapper categoryReadMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryCreateAndUpdateMapper = categoryCreateAndUpdateMapper;
        this.categoryReadMapper = categoryReadMapper;
    }

    // It does not require updating of the catalog
    // Because it has categories as mapped on specific products
    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryDetails createCategory(
            CreateAndUpdateCategoryDetails createCategoryDetails
    ) throws MalformedDataError {

        if (createCategoryDetails.getName() == null ||
                (createCategoryDetails.getAttributes().isEmpty() &&
                        createCategoryDetails.getParentId() == null)
        ) {
            log.error("Threw an error in creating category because of a lack of provided data: either attributes or name");

            throw new MalformedDataError();
        }

        Category save = categoryRepository.save(
                categoryCreateAndUpdateMapper.map(
                        createCategoryDetails
                )
        );

        log.info("New category {} was created", createCategoryDetails.getName());

        return categoryReadMapper.map(save);
    }
    @Override
    @Cacheable("categories")
    public List<String> getCategories() {
        return categoryRepository
                .findAll()
                .stream()
                .map(categoryReadMapper::map)
                .map(CategoryDetails::getName)
                .toList();
    }

    // It requires updating of the catalog
    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public String deleteCategory(Integer categoryId) {

        categoryRepository.deleteById(categoryId);

        if (categoryRepository.existsById(categoryId)) {
            return "category.delete.success";
        } else {
            return "category.delete.failure";
        }
    }
}
