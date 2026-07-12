package com.jzargo.productservice.service;

import com.jzargo.productservice.config.ApplicationPropertyStorage;
import com.jzargo.productservice.entity.Product;
import com.jzargo.productservice.entity.Status;
import com.jzargo.productservice.exception.CategoryNotFoundException;
import com.jzargo.productservice.exception.InvalidUpdateRequest;
import com.jzargo.productservice.exception.ProductNotFoundException;
import com.jzargo.productservice.exception.ShopDoesNotOwnProductException;
import com.jzargo.productservice.mapper.ProductCreateAndUpdateMapper;
import com.jzargo.productservice.mapper.ReadProductDetailsMapper;
import com.jzargo.productservice.model.CreateAndUpdateProductDetails;
import com.jzargo.productservice.model.ProductDetails;
import com.jzargo.productservice.repository.CategoryRepository;
import com.jzargo.productservice.repository.ProductRepository;
import com.jzargo.productservice.saga.SagaProductCreationManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)  // if not provided, data must be in immutable state
public class ProductServiceImpl implements ProductService{

    private final ProductRepository productRepository;
    private final ReadProductDetailsMapper readProductDetailsMapper;
    private final ProductCreateAndUpdateMapper productCreateAndUpdateMapper;
    private final ApplicationPropertyStorage applicationPropertyStorage;
    private final CategoryRepository categoryRepository;

    public ProductServiceImpl(ProductRepository productRepository, ReadProductDetailsMapper readProductDetailsMapper, ProductCreateAndUpdateMapper productCreateAndUpdateMapper, ApplicationPropertyStorage applicationPropertyStorage, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.readProductDetailsMapper = readProductDetailsMapper;
        this.productCreateAndUpdateMapper = productCreateAndUpdateMapper;
        this.applicationPropertyStorage = applicationPropertyStorage;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Cacheable(value = "product", key = "#id")
    public ProductDetails getProductById(Long id) throws ProductNotFoundException{
        return productRepository
                .findById(id)
                .map(readProductDetailsMapper::map)
                .orElseThrow(ProductNotFoundException::new);
    }



    @Override
    @Transactional
    public Long createProduct(CreateAndUpdateProductDetails createProductDetails) throws CategoryNotFoundException {
        log.debug("Product creation starting...");

        if (!categoryRepository.existsByName(createProductDetails.getCategory())) {
            throw new CategoryNotFoundException();
        }

        String defaultAvatarUri = applicationPropertyStorage.getMedia().getDefaultAvatarUri();

        createProductDetails.setAvatarUri(defaultAvatarUri);

        Product map = productCreateAndUpdateMapper.map(createProductDetails);

        map.setCategory(
                categoryRepository.findByName(
                        createProductDetails.getCategory()
                ).orElseThrow(
                        CategoryNotFoundException::new
                )
        );

        Product product = productRepository.save(map);

        log.info("Product was created with id {}", product.getId());

        return product.getId();
    }



    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#updateProductDetails.id")
    public ProductDetails updateProduct(CreateAndUpdateProductDetails updateProductDetails, Integer shopId)
            throws ProductNotFoundException, ShopDoesNotOwnProductException, InvalidUpdateRequest {

        // Null -> the field was not changed

        if (
                productRepository.findById(
                        updateProductDetails.getId()
                        ).orElseThrow()
                        .getShopId().equals(shopId)
        ) {
            throw new ShopDoesNotOwnProductException();
        }

        Product product = productRepository
                .findById(updateProductDetails.getId())
                .orElseThrow(ProductNotFoundException::new);

        productCreateAndUpdateMapper.updateMap(updateProductDetails, product);

        productRepository.save(product);

        return readProductDetailsMapper.map(product);
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#productId")
    public String deleteProduct(Long productId)
            throws ProductNotFoundException {

        log.info("Product deletion starting...");

        Product product = productRepository.findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        product.setStatus(Status.ARCHIVED);

        productRepository.save(product);

        return "Deletion of the process started successfully";
    }

}