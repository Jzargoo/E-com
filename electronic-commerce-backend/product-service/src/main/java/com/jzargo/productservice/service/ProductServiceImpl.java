package com.jzargo.productservice.service;

import com.jzargo.productservice.entity.Product;
import com.jzargo.productservice.entity.SagaProductEntity;
import com.jzargo.productservice.entity.Status;
import com.jzargo.productservice.exception.ProductNotFoundException;
import com.jzargo.productservice.mapper.ProductCreateAndUpdateMapper;
import com.jzargo.productservice.mapper.ReadProductDetailsMapper;
import com.jzargo.productservice.model.CreateAndUpdateProductDetails;
import com.jzargo.productservice.model.ProductDetails;
import com.jzargo.productservice.repository.ProductRepository;
import com.jzargo.productservice.saga.SagaProductCreationManager;
import lombok.extern.slf4j.Slf4j;
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
    private final SagaProductCreationManager sagaProductCreationManager;

    public ProductServiceImpl(ProductRepository productRepository, ReadProductDetailsMapper readProductDetailsMapper, ProductCreateAndUpdateMapper productCreateAndUpdateMapper, SagaProductCreationManager sagaProductCreationManager) {
        this.productRepository = productRepository;
        this.readProductDetailsMapper = readProductDetailsMapper;
        this.productCreateAndUpdateMapper = productCreateAndUpdateMapper;
        this.sagaProductCreationManager = sagaProductCreationManager;
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
    public Long createProduct(CreateAndUpdateProductDetails createProductDetails) {
        log.debug("Product creation starting...");

        Product product = productRepository.save(
                productCreateAndUpdateMapper.map(createProductDetails)
        );

        log.info("Product was created with id {}", product.getId());

        return product.getId();
    }

    @Override
    @Transactional
    @Cacheable(value = "product", key = "#updateProductDetails.id")
    public ProductDetails updateProduct(CreateAndUpdateProductDetails updateProductDetails) throws ProductNotFoundException{

        // If null -> the data did not change

        // TODO: fill nulls with previous info fields that did not change

        Product newProduct = productRepository
                .findById(updateProductDetails.getId())
                .map((product) -> productCreateAndUpdateMapper.map(updateProductDetails, product))
                .orElseThrow(ProductNotFoundException::new);

        return readProductDetailsMapper.map(newProduct);
    }

    @Override
    @Transactional
    @Cacheable(value = "product", key = "#productId")
    public String deleteProduct(Long productId)
            throws ProductNotFoundException {
        Product product = productRepository.findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        product.setStatus(Status.ARCHIVED);

        productRepository.save(product);

        return "Deletion of the process started successfully";
    }

    @Override
    public String startSaga(CreateAndUpdateProductDetails details) {
        try{
            sagaProductCreationManager.startSaga(details);
            return "productCreateSaga.success";
        } catch (Exception e) {
            throw new InternalError("productCreateSaga.fail");
        }
    }
}
