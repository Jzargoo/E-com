package com.jzargo.productservice.api;

import com.jzargo.productservice.exception.InvalidUpdateRequest;
import com.jzargo.productservice.exception.ProductNotFoundException;
import com.jzargo.productservice.exception.ShopDoesNotOwnProductException;
import com.jzargo.productservice.model.CreateAndUpdateProductDetails;
import com.jzargo.productservice.model.ProductDetails;
import com.jzargo.productservice.saga.SagaProductCreation;
import com.jzargo.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final SagaProductCreation sagaProductCreation;

    @GetMapping("/{id}")
    ResponseEntity<ProductDetails>  getProductById(@PathVariable Long id){
        try {
            return ResponseEntity.ok(productService.getProductById(id));
        } catch (Exception e) {
            return ResponseEntity.noContent().build();
        }
    }

    @PreAuthorize(
            "(hasAuthority('ROLE_SHOP_OWNER') or hasAuthority('SCOPE_ROLE_SHOP_OWNER')) and " +
                    "authentication.principal.claims['mode'] == 'OWNER' and " +
                    "authentication.principal.claims['shop_id'] == #createProductDetails.shopId"
    )
    @PostMapping
    ResponseEntity<String> createProduct (
            @Validated @RequestBody CreateAndUpdateProductDetails createProductDetails
    ) {

        try {

            log.info("Caught a request to create a product");

            sagaProductCreation.initiateProductCreation(createProductDetails);

            log.debug("A request was successfully processed");

            return ResponseEntity.ok("Product created successfully");

        } catch (Exception e) {
            log.error("Product creation failed", e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }

    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "(hasAuthority('ROLE_SHOP_OWNER') or hasAuthority('SCOPE_ROLE_SHOP_OWNER')) and " +
                    "authentication.principal.claims['mode'] == 'OWNER' and " +
                    "authentication.principal.claims['shop_id'] == #createAndUpdateProductDetails.shopId"
    )    ResponseEntity<ProductDetails> updateProduct(
            @PathVariable Long id,
            @RequestBody CreateAndUpdateProductDetails createAndUpdateProductDetails
            ) throws ShopDoesNotOwnProductException, InvalidUpdateRequest, ProductNotFoundException {

        if (
                id == null ||
                        !id.equals(createAndUpdateProductDetails.getId())
        ) {

           return ResponseEntity.badRequest().build();

        }

        createAndUpdateProductDetails.setId(id);

        ProductDetails productDetails = productService.updateProduct(createAndUpdateProductDetails);

        return ResponseEntity.ok(productDetails);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(
            "(hasAuthority('ROLE_SHOP_OWNER') or hasAuthority('SCOPE_ROLE_SHOP_OWNER')) and " +
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
