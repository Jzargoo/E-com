package com.jzargo.productservice.api;

import com.jzargo.productservice.exception.InvalidUpdateRequest;
import com.jzargo.productservice.exception.ProductNotFoundException;
import com.jzargo.productservice.exception.ShopDoesNotOwnProductException;
import com.jzargo.productservice.model.*;
import com.jzargo.productservice.saga.SagaProductCreation;
import com.jzargo.productservice.saga.SagaProductCreationManager;
import com.jzargo.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final SagaProductCreationManager sagaProductCreationManager;

    @GetMapping("/{id}")
    ResponseEntity<ProductDetails>  getProductById(@PathVariable Long id){
        try {
            return ResponseEntity.ok(productService.getProductById(id));
        } catch (Exception e) {
            return ResponseEntity.noContent().build();
        }
    }

    @PostMapping
    ResponseEntity<String> createProduct (
            @RequestBody CreateAndUpdateProductDetails createProductDetails,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            Integer shopId = (Integer) jwt.getClaim("shop_id");

            if(shopId == null){
                return ResponseEntity.badRequest().build();
            }

            sagaProductCreationManager.startSaga(createProductDetails);
            return ResponseEntity.ok("Product created successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    ResponseEntity<ProductDetails> updateProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CreateAndUpdateProductDetails createAndUpdateProductDetails
            ) throws ShopDoesNotOwnProductException, InvalidUpdateRequest, ProductNotFoundException {
        Integer shopId = jwt.getClaim("shop_id");

        productService.updateProduct(createAndUpdateProductDetails, shopId);

        return ResponseEntity.ok(new ProductDetails());
    }

    @DeleteMapping("/{id}")
    ResponseEntity<String> deleteProduct(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt
            ){
        return ResponseEntity.ok("");
    }
}
