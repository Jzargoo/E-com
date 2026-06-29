package com.jzargo.productservice.api;

import com.jzargo.productservice.exception.ProductNotFoundException;
import com.jzargo.productservice.exception.ShopDoesNotOwnProductException;
import com.jzargo.productservice.service.MediaService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/products/media")
@RequiredArgsConstructor
public class MediaController {
    private final MediaService mediaService;

    @PutMapping("/{productId}")
    public ResponseEntity<String> addMediaContent(
            @RequestBody @NotNull List<MultipartFile> multipartFiles,
            @PathVariable Long productId,
            @AuthenticationPrincipal Jwt jwt) throws IOException, ProductNotFoundException, ShopDoesNotOwnProductException {

        log.debug("Adding media content to product {}",productId);

        Integer shopId = jwt.getClaim("shop_id");

        mediaService.addMediaContent(multipartFiles, productId, shopId);

        return ResponseEntity.ok(
                "new image of the product was added successfully"
        );
    }

    @GetMapping("/{productId}")
    public ResponseEntity<List<MultipartFile>> getAllMediaContents(
            @PathVariable Long productId
    ) throws IOException, ProductNotFoundException {

        log.debug("Getting all media content from product {}",productId);

        return ResponseEntity.ok(
                mediaService.getMediaContent(productId)
        );
    }

    @GetMapping("/avatar/{productId}")
    public ResponseEntity<MultipartFile> getAvatar(
            @PathVariable Long productId
    ) throws IOException, ProductNotFoundException {

        log.debug("Getting avatar from product {}",productId);

        return ResponseEntity.ok(
                mediaService.getAvatar(productId)
        );
    }

    @PostMapping("/{productId}")
    public ResponseEntity<String> addAvatar (
            @RequestBody @NotNull MultipartFile multipartFile,
            @PathVariable Long productId,
            @AuthenticationPrincipal Jwt jwt) throws IOException, ProductNotFoundException, ShopDoesNotOwnProductException {

        log.debug("Adding avatar to product {}",productId);

        Integer shopId = jwt.getClaim("shop_id");

        mediaService.addAvatar(multipartFile, productId, shopId);

        return ResponseEntity.ok(
                "new avatar was added successfully"
        );

    }
}
