package com.jzargo.productservice.mvc;

import com.c4_soft.springaddons.security.oauth2.test.AuthenticationFactoriesTestConf;
import com.c4_soft.springaddons.security.oauth2.test.annotations.WithJwt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jzargo.productservice.api.ProductController;
import com.jzargo.productservice.config.security.ProductSecurity;
import com.jzargo.productservice.exception.InvalidUpdateRequest;
import com.jzargo.productservice.exception.ProductNotFoundException;
import com.jzargo.productservice.exception.ShopDoesNotOwnProductException;
import com.jzargo.productservice.model.CreateAndUpdateProductDetails;
import com.jzargo.productservice.model.ProductDetails;
import com.jzargo.productservice.saga.SagaProductCreationManager;
import com.jzargo.productservice.service.ProductService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
@EnableMethodSecurity
@WebMvcTest(ProductController.class)
@Import({
        ProductSecurity.class,
        AuthenticationFactoriesTestConf.class,
})
public class ProductControllerWebMvcTest {

    private final Integer shopId = 12;

    private final Long productId = 1L;

    @Autowired
    MockMvcTester  mockMvc;

    @MockitoBean
    ProductService productService;

    @MockitoBean
    SagaProductCreationManager sagaProductCreationManager;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    JwtDecoder jwtDecoder;



    @Test
    @DisplayName("GET product by id - should return specific product")
    void getProductById() throws ProductNotFoundException {
        ProductDetails build = ProductDetails.builder()
                .price(
                        BigDecimal.valueOf(24.12)
                )
                .name("Product1")
                .avgRate(4.6F)
                .shopId(shopId)
                .description("Description 1")
                .category("Category1")
                .characteristics(Map.of())
                .build();
        when(
                productService.getProductById(productId)
        ).thenReturn(build);

        mockMvc
                .perform(get("/api/products/" + productId))
                .assertThat()
                .hasStatus(200)
                .bodyJson()
                .convertTo(ProductDetails.class)
                .hasNoNullFieldsOrProperties()
                .isEqualTo(build);

    }



    @Test
    @DisplayName("POST create a product without jwt; should  throw an exception")
    void createProduct_whenJwtIsNotProvided_failureTest() throws JsonProcessingException {

        CreateAndUpdateProductDetails createAndUpdateProductDetails = CreateAndUpdateProductDetails.builder()
                .price(BigDecimal.valueOf(24.12))
                .name("Product1")
                .shopId(shopId)
                .description("Description 1")
                .category("Category1")
                .characteristics(new HashMap<>())
                .build();

        mockMvc
                .method(HttpMethod.POST)
                .content(objectMapper.writeValueAsString(createAndUpdateProductDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .uri("/api/products")
                .assertThat()
                .hasStatus(401);

    }



    @Test
    @WithJwt("shop-owner.json")
    @DisplayName("POST creates a product; success case")
    public void createProduct_whenJwtIsProvided_successCase() throws JsonProcessingException {

        CreateAndUpdateProductDetails createAndUpdateProductDetails = CreateAndUpdateProductDetails.builder()
                .shopId(shopId)
                .name("Product1")
                .price(BigDecimal.ONE)
                .category("Category1")
                .build();

        mockMvc
                .method(HttpMethod.POST)
                .content(objectMapper.writeValueAsString(createAndUpdateProductDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .uri("/api/products")
                .assertThat()
                .hasStatus(200)
                .bodyText()
                .isEqualTo("Product created successfully");

    }



    @Test
    @WithJwt("shop-owner.json")
    @DisplayName("POST create a product with different shop id; fail case")
    public void createProduct_whenShopIdWrong_failCase() throws JsonProcessingException {

        CreateAndUpdateProductDetails createAndUpdateProductDetails = CreateAndUpdateProductDetails.builder()
                .shopId(shopId + 10)
                .name("Product1")
                .price(BigDecimal.ONE)
                .category("Category1")
                .build();

        mockMvc
                .method(HttpMethod.POST)
                .content(objectMapper.writeValueAsString(createAndUpdateProductDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .uri("/api/products")
                .assertThat()
                .hasStatus(403);
    }



    @Test
    @WithJwt("shop-owner.json")
    @DisplayName("POST create a product with incorrect validation; fail case")
    public void createProduct_whenValidationError_failCase() throws JsonProcessingException {

        CreateAndUpdateProductDetails createAndUpdateProductDetails = CreateAndUpdateProductDetails.builder()
                .shopId(shopId)
                .build();

        mockMvc
                .method(HttpMethod.POST)
                .content(objectMapper.writeValueAsString(createAndUpdateProductDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .uri("/api/products")
                .assertThat()
                .hasStatus(400);
    }



    @Test
    @WithJwt("shop-owner.json")
    @DisplayName("PUT update a product; success case")
    public void updateProduct_whenJwtIsProvided_successCase() throws JsonProcessingException, ShopDoesNotOwnProductException, InvalidUpdateRequest, ProductNotFoundException {

        CreateAndUpdateProductDetails createAndUpdateProductDetails = CreateAndUpdateProductDetails.builder()
                .shopId(shopId)
                .name("Product1")
                .price(BigDecimal.ONE)
                .category("Category1")
                .id(productId)
                .build();

        ProductDetails productDetails = ProductDetails.builder()
                .name("Product1")
                .description("Description 1")
                .category("Category1")
                .price(BigDecimal.ONE)
                .shopId(shopId)
                .avgRate(5.0F)
                .build();

        when(
                productService.updateProduct(any())
        ).thenReturn(productDetails);

        mockMvc
                .method(HttpMethod.PUT)
                .uri("/api/products/1")
                .content(objectMapper.writeValueAsString(createAndUpdateProductDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .assertThat()
                .hasStatus(200)
                .bodyJson()
                .convertTo(ProductDetails.class)
                .isEqualTo(productDetails);
    }



    @Test
    @WithJwt(value = "shop-owner.json")
    void debugAuthorities() throws Exception {

        var authentication = SecurityContextHolder.getContext().getAuthentication();

        System.out.println("=== SECURITY CONTEXT DEBUG ===");

        System.out.println("Authentication class: " + authentication.getClass().getName());

        System.out.println("Authorities: " + authentication.getAuthorities());

        System.out.println("Principal attributes: " + ((Jwt) authentication.getPrincipal()).getClaims());

        System.out.println("==============================");

    }

}