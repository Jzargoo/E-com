package com.jzargo.productAssetsService.api;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products/media")
public class MediaController {

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
     *
     *     }
     * }
     */
}
