package com.jzargo.productAssetsService.api;

import com.jzargo.productAssetsService.exception.AssetNotFoundException;
import com.jzargo.productAssetsService.helper.ContentTypeParser;
import com.jzargo.productAssetsService.model.PlainFile;
import com.jzargo.productAssetsService.service.MediaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/products/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    /**
     * @Slf4j
     * @RestController
     * @RequestMapping("/api/products/media")
     * public class MediaController {
     *
     *     private final MediaService mediaService;
     *
     *     public MediaController(@Qualifier("asyncMediaService") MediaService mediaService) {
     *         this.mediaService = mediaService;
     *     }
     *
     *     @PutMapping("/{productId}")
     *     @PreAuthorize(
     *             "( hasAuthority('ROLE_SHOP_OWNER') or hasAuthority('SCOPE_ROLE_SHOP_OWNER') ) and " +
     *                     "authentication.principal.claims['mode'] == 'OWNER'"
     *     )
     *     public ResponseEntity<String> addMediaContent(
     *             @RequestBody @NotNull MultipartFile multipartFile,
     *             @PathVariable Long productId,
     *             @AuthenticationPrincipal Jwt jwt
     *     ) throws IOException, ProductNotFoundException, ShopDoesNotOwnProductException {
     *
     *         log.debug("Adding media content to product {}",productId);
     *
     *         Number shopId = jwt.getClaim("shop_id");
     *
     *         if (shopId == null || shopId.intValue() <= 0) {
     *             return ResponseEntity.badRequest().build();
     *         }
     *
     *         mediaService.addMediaContent(
     *                 multipartFile,
     *                 productId,
     *                 shopId.intValue()
     *         );
     *
     *         return ResponseEntity.status(HttpStatus.ACCEPTED).body("Started saving a file");
     *
     *     }
     *
     *     @GetMapping(path = "/{}",produces = "multipart/x-mixed-replace; boundary=--end-of-the-file")
     *     public StreamingResponseBody getAllMediaContents(
     *             @PathVariable Long productId
     *     ) throws IOException, ProductNotFoundException {
     *
     *         log.debug("Getting all media content from product {}",productId);
     *
     *         List<PlainFile> mediaContent = mediaService.getMediaContent(productId);
     *
     *         return outputStream -> {
     *             for (PlainFile media : mediaContent) {
     *                 String headers =
     *                         "Content-Type: %s \r\n" +
     *                                 "Content-Disposition: attachment; filename=\""
     *                                         .formatted(
     *                                                 ContentTypeParser.parseIntoMime(media.getContentType())
     *                                         );
     *
     *                 outputStream.write(headers.getBytes(StandardCharsets.UTF_8));
     *
     *                 media.getContent().
     *
     *             }
     *         }
     *
     *     }
     *
     *     @GetMapping("/avatar/{productId}")
     *     public ResponseEntity<PlainFile> getAvatar(
     *             @PathVariable Long productId
     *     ) throws IOException, ProductNotFoundException {
     *
     *         log.debug("Getting avatar from product {}",productId);
     *
     *         return ResponseEntity.ok(
     *                 mediaService.getAvatar(productId)
     *         );
     *     }
     *
     *     @PostMapping("/{productId}")
     *     @PreAuthorize(
     *             "( hasAuthority('ROLE_SHOP_OWNER') or hasAuthority('SCOPE_ROLE_SHOP_OWNER') ) and " +
     *                     "authentication.principal.claims['mode'] == 'OWNER'"
     *     )
     *     public ResponseEntity<String> addAvatar (
     *             @RequestBody @NotNull MultipartFile multipartFile,
     *             @PathVariable Long productId,
     *             @AuthenticationPrincipal Jwt jwt) throws IOException, ProductNotFoundException, ShopDoesNotOwnProductException {
     *
     *         log.debug("Adding avatar to product {}",productId);
     *
     *         Integer shopId = jwt.getClaim("shop_id");
     *
     *         mediaService.addAvatar(multipartFile, productId, shopId);
     *
     *         return ResponseEntity.ok(
     *                 "new avatar was added successfully"
     *         );
     */

    @GetMapping("/{productId}")
    public Flux<Long> getIdsByProductId(@PathVariable Long productId) {
        log.info("Caught request to get product with id {}", productId);

        return mediaService.findIdsByProductId(productId);
    }


    @GetMapping(
            path = "assets/{assetId}"
    )
    public Mono<ResponseEntity<Flux<DataBuffer>>> getAssetsByAssetId(@PathVariable Long assetId) {
        log.info("Caught request to get asset with id {}", assetId);

        try {

            PlainFile mediaContent = mediaService.getMediaContent(assetId);

            return mediaContent

                    .getContentType()

                    .map(contentType -> ResponseEntity
                            .status(HttpStatus.OK)
                            .contentType(
                                    MediaType.parseMediaType(
                                            ContentTypeParser.parseIntoMime(
                                                    contentType
                                            )
                                    )
                            )
                            .body(mediaContent.getUpload())

                    );

        } catch (IOException | AssetNotFoundException e) {
            log.error("Error getting asset with id {}", assetId, e);

            return null;
        }
    }

}
