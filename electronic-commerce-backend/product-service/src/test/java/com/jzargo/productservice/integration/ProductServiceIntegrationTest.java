package com.jzargo.productservice.integration;


import com.jzargo.productservice.entity.Category;
import com.jzargo.productservice.entity.Product;
import com.jzargo.productservice.entity.Status;
import com.jzargo.productservice.exception.CategoryNotFoundException;
import com.jzargo.productservice.exception.ProductNotFoundException;
import com.jzargo.productservice.exception.ShopDoesNotOwnProductException;
import com.jzargo.productservice.model.CreateAndUpdateProductDetails;
import com.jzargo.productservice.model.ProductDetails;
import com.jzargo.productservice.repository.CategoryRepository;
import com.jzargo.productservice.repository.ProductRepository;
import com.jzargo.productservice.service.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.metrics.KafkaMetricsAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DirtiesContext
@ActiveProfiles("test")
@SpringBootTest(properties = "kafka.enabled=false")
@ImportAutoConfiguration(
        exclude ={
                KafkaAutoConfiguration.class,
                KafkaMetricsAutoConfiguration.class
        }
)
@Transactional
public class ProductServiceIntegrationTest {

    @Autowired
    ProductServiceImpl productService;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ProductRepository productRepository;

    private final Product product = Product.builder()
            .name("test 1")
            .shopId(1)
            .status(Status.AVAILABLE)
            .stockPrice(BigDecimal.valueOf(1.00))
            .description("")
            .build();

    private Category existingCategory;

    @BeforeEach
    public void setup(){
        // ARRANGE
        existingCategory = categoryRepository.save(
                Category.builder()
                        .name("test category")
                        .build()
        );

        product.setCategory(existingCategory);
    }

    @Test
    @DisplayName("should return a product")
    public void test_getProductById_success() throws ProductNotFoundException {

        // ARRANGE
        Product save = productRepository.save(product);

        // ACT
        ProductDetails productById = productService.getProductById(save.getId());

        // ASSERT
        assertEquals(productById.getCategory(), product.getCategory().getName());
        assertEquals(0, product.getStockPrice()
                .compareTo(
                        productById.getPrice()
                ));
        assertEquals(productById.getCharacteristics(), product.getCharacteristics());
        assertEquals(productById.getName(), product.getName());
    }

    @Test
    @DisplayName("should create a product")
    public void test_createProduct_success() throws CategoryNotFoundException {

        // ARRANGE
        CreateAndUpdateProductDetails details = CreateAndUpdateProductDetails.builder()
                .price(BigDecimal.TWO)
                .name("test 1")
                .shopId(1)
                .characteristics(new HashMap<>())
                .description("...")
                .category(existingCategory.getId())
                .build();

        // ACT

        Long id = productService.createProduct(details);

        Optional<Product> savedProduct = productRepository.findById(id);

        // ASSERT
        assertTrue(savedProduct.isPresent(), "cannot find a saved product");

        Product product = savedProduct.get();

        assertEquals(details.getCategory(), product.getCategory().getId());

        assertEquals(details.getDescription(), product.getDescription());

        assertEquals(details.getName(), product.getName());

        assertEquals(details.getShopId(), product.getShopId());

        assertEquals(
                0,
                details.getPrice()
                        .compareTo(product.getStockPrice())
        );

    }

    @Test
    @DisplayName("should throw category is not found")
    public void test_createProduct_CategoryNotFound() {

        // ARRANGE
        CreateAndUpdateProductDetails details = CreateAndUpdateProductDetails.builder()
                .price(BigDecimal.TWO)
                .name("test 1")
                .shopId(1)
                .characteristics(new HashMap<>())
                .description("...")
                .category(10)
                .build();

        // ACT && ASSERT
        assertThrows( CategoryNotFoundException.class, () ->
                productService.createProduct(details)
        );

    }

    @Test
    @DisplayName("should throw category is not found")
    public void test_updateProduct_CategoryNotFound() {

        // ARRANGE
        Product save = productRepository.save(product);

        CreateAndUpdateProductDetails updateDetails = CreateAndUpdateProductDetails.builder()
                .id(save.getId())
                .shopId(1)
                .name("Updated Name")
                .category(10)
                .build();

        // ACT && ASSERT
        assertThrows(CategoryNotFoundException.class, () ->
                productService.updateProduct(updateDetails)
        );

    }

    @Test
    @DisplayName("should throw shop does not own")
    public void test_updateProduct_shopDoesNotOwn() {

        // ARRANGE
        Product save = productRepository.save(product);

        CreateAndUpdateProductDetails updateDetails = CreateAndUpdateProductDetails.builder()
                .id(save.getId())
                .shopId(99)
                .name("Malicious Update")
                .build();

        // ACT && ASSERT
        assertThrows(ShopDoesNotOwnProductException.class, () ->
                productService.updateProduct(updateDetails)
        );

    }

    @Test
    @DisplayName("should update a product and keep old values when fields are null")
    public void test_updateProduct_success() throws Exception {

        // ARRANGE
        BigDecimal originalPrice = new BigDecimal("150.00");

        String originalDescription = "Original Long Description";

        product.setStockPrice(originalPrice);
        product.setDescription(originalDescription);

        Product save = productRepository.save(product);

        CreateAndUpdateProductDetails updateDetails = CreateAndUpdateProductDetails.builder()
                .id(save.getId())
                .shopId(1)
                .name("Updated Product Name")
                .price(null)
                .description(null)
                .category(existingCategory.getId())
                .build();

        // ACT
        productService.updateProduct(updateDetails);

        // ASSERT
        Optional<Product> updatedProductOpt = productRepository.findById(save.getId());

        assertTrue(updatedProductOpt.isPresent(), "Product should be present in DB");

        Product updatedProduct = updatedProductOpt.get();

        assertEquals("Updated Product Name", updatedProduct.getName());

        assertEquals(0, originalPrice.compareTo(updatedProduct.getStockPrice()), "Price should remain unchanged");

        assertEquals(originalDescription, updatedProduct.getDescription(), "Description should remain unchanged");
    }

    @Test
    @DisplayName("should delete a product")
    public void test_deleteProduct_success() throws Exception {

        // ARRANGE
        Product save = productRepository.save(product);

        // ACT
        String resultMessage = productService.deleteProduct(save.getId());

        // ASSERT
        Optional<Product> archivedProductOpt = productRepository.findById(save.getId());

        assertTrue(archivedProductOpt.isPresent(), "Product should still exist in DB");

        Product archivedProduct = archivedProductOpt.get();

        assertEquals(Status.ARCHIVED, archivedProduct.getStatus(), "Product status should be ARCHIVED");

        assertNotNull(resultMessage);
    }

}