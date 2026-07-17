package com.jzargo.productservice.mvc;


import com.c4_soft.springaddons.security.oauth2.test.AuthenticationFactoriesTestConf;
import com.c4_soft.springaddons.security.oauth2.test.annotations.WithJwt;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jzargo.productservice.api.CategoryController;
import com.jzargo.productservice.config.security.ProductSecurity;
import com.jzargo.productservice.model.CategoryDetails;
import com.jzargo.productservice.model.CreateAndUpdateCategoryDetails;
import com.jzargo.productservice.service.CategoryServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
@EnableMethodSecurity
@WebMvcTest(CategoryController.class)
@Import({
        ProductSecurity.class,
        AuthenticationFactoriesTestConf.class,
})
public class CategoryControllerWebMvcTest {

    @Autowired
    private MockMvcTester mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private CategoryServiceImpl categoryService;
    @MockitoBean
    private JwtDecoder jwtDecoder;


    @Test
    @WithJwt("category-admin.json")
    @DisplayName("POST creates a category; success case")
    public void testCreateCategory_whenJwtValid_successCase() throws Exception {

        CreateAndUpdateCategoryDetails createAndUpdateCategoryDetails =
                new CreateAndUpdateCategoryDetails();

        createAndUpdateCategoryDetails.setName("Electronics");

        CategoryDetails categoryDetails = new CategoryDetails(
                new HashMap<>(),
                "Electronics",
                1
        );

        when(
                categoryService.createCategory(any())
        ).thenReturn(categoryDetails);

        mockMvc
                .method(HttpMethod.POST)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAndUpdateCategoryDetails))
                .uri("/api/products/category")
                .assertThat()
                .hasStatus(200)
                .bodyJson()
                .convertTo(CategoryDetails.class)
                .isEqualTo(categoryDetails);

    }



    @Test
    @WithJwt(value = "category-admin.json")
    void debugAuthorities() throws Exception {

        var authentication = SecurityContextHolder.getContext().getAuthentication();

        System.out.println("=== SECURITY CONTEXT DEBUG ===");

        System.out.println("Authentication class: " + authentication.getClass().getName());

        System.out.println("Authorities: " + authentication.getAuthorities());

        System.out.println("Principal attributes: " + ((Jwt) authentication.getPrincipal()).getClaims());

        System.out.println("==============================");

    }
}
