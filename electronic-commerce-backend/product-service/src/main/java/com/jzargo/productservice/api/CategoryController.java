package com.jzargo.productservice.api;

import com.jzargo.productservice.exception.CategoryNotFoundException;
import com.jzargo.productservice.exception.MalformedDataError;
import com.jzargo.productservice.model.CategoryDetails;
import com.jzargo.productservice.model.CreateAndUpdateCategoryDetails;
import com.jzargo.productservice.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products/category")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize(
            "hasAuthority('ROLE_ADMIN') or hasAuthority('SCOPE_ROLE_ADMIN') and " +
                    "authentication.principal.claims['mode'] == 'ADMIN'")
    public ResponseEntity<CategoryDetails> createCategory(
            @RequestBody @Validated CreateAndUpdateCategoryDetails createCategoryDetails
    ) {
        log.debug("Creation a category started to execute");

        try {

            return ResponseEntity.ok(
                    categoryService.createCategory(createCategoryDetails)
            );

        } catch (MalformedDataError e) {
            log.error(e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{categoryName}")
    public ResponseEntity<CategoryDetails> getCategory(String categoryName) {
        log.debug("Get category started to execute");

        try {
            return ResponseEntity.ok(
                    categoryService.getCategoryByName(categoryName)
            );
        } catch (CategoryNotFoundException e) {
            log.error("Cannot find a category", e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<String>> getAllCategories() {
        return ResponseEntity.ok(
                categoryService.getCategories()
        );
    }

    @DeleteMapping("/{categoryName}")
    @PreAuthorize(
            "hasAuthority('ROLE_ADMIN') or hasAuthority('SCOPE_ROLE_ADMIN') and " +
                    "authentication.principal.claims['mode'] == ADMIN")
    public ResponseEntity<String> deleteCategory(String categoryName) {
        log.debug("Delete category started to execute");

        try {
            categoryService.deleteCategoryByName(categoryName);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok("The category was successfully deleted");
    }
}
