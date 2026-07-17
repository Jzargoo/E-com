package com.jzargo.productservice.api;

import com.jzargo.productservice.config.security.ProductSecurity;
import com.jzargo.productservice.exception.InvalidUpdateRequest;
import com.jzargo.productservice.exception.ProductNotFoundException;
import com.jzargo.productservice.exception.ShopDoesNotOwnProductException;
import com.jzargo.productservice.model.*;
import com.jzargo.productservice.saga.SagaProductCreation;
import com.jzargo.productservice.saga.SagaProductCreationManager;
import com.jzargo.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
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

    @PreAuthorize(
            "hasAuthority('ROLE_SHOP_OWNER') or hasAuthority('SCOPE_ROLE_SHOP_OWNER') and " +
                    "authentication.principal.claims['mode'] == 'OWNER' and " +
                    "authentication.principal.claims['shop_id'] == #createProductDetails.shopId"
    )
    @PostMapping
    ResponseEntity<String> createProduct (
            @Validated @RequestBody CreateAndUpdateProductDetails createProductDetails) {

        try {

            sagaProductCreationManager.startSaga(createProductDetails);

            return ResponseEntity.ok("Product created successfully");

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }

    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('ROLE_SHOP_OWNER') or hasAuthority('SCOPE_ROLE_SHOP_OWNER') and " +
                    "authentication.principal.claims['mode'] == 'OWNER' and " +
                    "authentication.principal.claims['shop_id'] == #createAndUpdateProductDetails.shopId"
    )
    ResponseEntity<ProductDetails> updateProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt,
            @Validated @RequestBody CreateAndUpdateProductDetails createAndUpdateProductDetails
            ) throws ShopDoesNotOwnProductException, InvalidUpdateRequest, ProductNotFoundException {

        Integer shopId = jwt.getClaim("shop_id");

        if (id == null || !id.equals(createAndUpdateProductDetails.getId())) {
           return ResponseEntity.badRequest().build();
        }

        createAndUpdateProductDetails.setId(id);

        createAndUpdateProductDetails.setShopId(shopId);

        ProductDetails productDetails = productService.updateProduct(createAndUpdateProductDetails);

        return ResponseEntity.ok(productDetails);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('ROLE_SHOP_OWNER') or hasAuthority('SCOPE_ROLE_SHOP_OWNER') and " +
                    "authentication.principal.claims['mode'] == 'OWNER'"
    )
    ResponseEntity<String> deleteProduct(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt
            ){

        Integer shopId = jwt.getClaim("shop_id");

        // TODO: Implement delete product saga

        return ResponseEntity.ok("");
    }
}
