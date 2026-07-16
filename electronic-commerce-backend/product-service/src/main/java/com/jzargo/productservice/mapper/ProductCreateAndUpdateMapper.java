package com.jzargo.productservice.mapper;

import com.jzargo.core.mapper.Mapper;
import com.jzargo.productservice.entity.Category;
import com.jzargo.productservice.entity.Product;
import com.jzargo.productservice.exception.InvalidUpdateRequest;
import com.jzargo.productservice.model.CreateAndUpdateProductDetails;
import com.jzargo.productservice.repository.CategoryRepository;
import org.springframework.stereotype.Component;

@Component
public class ProductCreateAndUpdateMapper implements Mapper<CreateAndUpdateProductDetails, Product> {

    private final CategoryRepository categoryRepository;

    public ProductCreateAndUpdateMapper(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Product map(CreateAndUpdateProductDetails from) {
        return Product.builder()
                .name(from.getName())
                .description(from.getDescription())
                .characteristics(from.getCharacteristics())
                .avatar(
                        from.getAvatarUri()
                )
                .shopId(
                        from.getShopId()
                )
                .stockPrice(from.getPrice())
                .build();
    }

    public void updateMap(CreateAndUpdateProductDetails from, Product old) throws InvalidUpdateRequest {

        if (from.getCategory() != null) {
            Category category = categoryRepository.findByName(from.getCategory())
                    .stream()
                    .findAny().orElseThrow(
                            () -> new InvalidUpdateRequest("Category not found")
                    );

            old.setCategory(category);
        }

        if (from.getName() !=  null) {
            old.setName(from.getName());
        }

        if (from.getDescription() != null) {
            old.setDescription(from.getDescription());
        }

        if (from.getCharacteristics() != null) {
            old.setCharacteristics(from.getCharacteristics());
        }

        if (from.getPrice() != null) {
            old.setStockPrice(from.getPrice());
        }
    }

}