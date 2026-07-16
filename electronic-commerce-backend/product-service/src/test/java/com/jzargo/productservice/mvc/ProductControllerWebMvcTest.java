package com.jzargo.productservice.mvc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jzargo.productservice.api.ProductController;
import com.jzargo.productservice.config.ApplicationPropertyStorage;
import com.jzargo.productservice.config.security.ProductSecurity;
import com.jzargo.productservice.exception.ProductNotFoundException;
import com.jzargo.productservice.model.CreateAndUpdateProductDetails;
import com.jzargo.productservice.model.ProductDetails;
import com.jzargo.productservice.saga.SagaProductCreationManager;
import com.jzargo.productservice.service.ProductService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureWebMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.cors.CorsConfigurationSource;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
@EnableMethodSecurity
@AutoConfigureMockMvc(addFilters = true)
@WebMvcTest(ProductController.class)
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
    @MockitoBean
    ApplicationPropertyStorage applicationPropertyStorage;
    @MockitoBean
    HttpSecurity httpSecurity;


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
    @DisplayName("POST create a product without jwt; should  return 401")
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
                .hasFailed()
                .failure()
                .isInstanceOf(ServletException.class)
                .rootCause()
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);

    }

    @Test
    @DisplayName("POST create a product; success case")
    public void createProduct_whenJwtIsProvided_successCase() throws JsonProcessingException {

        CreateAndUpdateProductDetails createAndUpdateProductDetails = CreateAndUpdateProductDetails.builder()
                .price(BigDecimal.valueOf(24.12))
                .name("Product1")
                .shopId(shopId)
                .description("Description 1")
                .category("Category1")
                .characteristics(new HashMap<>())
                .build();

        Jwt token = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .claim("scope", "ROLE_SHOP_OWNER")
                .claim("shop_id", shopId.toString())
                .claim("mode", "OWNER")
                .build();

        when(
                jwtDecoder.decode(any())
        ).thenReturn(token);

        JwtAuthenticationToken auth = new  JwtAuthenticationToken(
                token,
                List.of(
                        new SimpleGrantedAuthority("ROLE_SHOP_OWNER")
                )
        );

        MockMvcTester.MockMvcRequestBuilder with = mockMvc
                .method(HttpMethod.POST)
                .content(objectMapper.writeValueAsString(createAndUpdateProductDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .header("Bearer", "mock-token")
                .uri("/api/products")
                .with(
                        jwt()
                                .jwt(
                                        builder -> {
                                            builder.header("alg", "none");
                                            builder.claim("scope", "ROLE_SHOP_OWNER");
                                            builder.claim("shop_id", shopId.toString());
                                            builder.claim("mode", "OWNER");
                                        })
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_SHOP_OWNER")
                                )
                );

        with
                .assertThat()
                .hasStatus(200)
                .bodyText()
                .isEqualTo("Product created successfully");

    }

}
