package com.jzargo.productAssetsService.service;

import com.jzargo.productAssetsService.entity.ProductAssets;
import com.jzargo.productAssetsService.repository.MediaContentRepository;
import com.jzargo.productAssetsService.repository.ProductAssetsRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AssetsServiceImpl implements AssetsService{
    private final ProductAssetsRepository productAssetsRepository;
    private final MediaContentRepository mediaContentRepository;

    public AssetsServiceImpl(ProductAssetsRepository productAssetsRepository, MediaContentRepository mediaContentRepository) {
        this.productAssetsRepository = productAssetsRepository;
        this.mediaContentRepository = mediaContentRepository;
    }

    @Override
    public Mono<Void> initAssetsCompensation(Long productId) {

        return productAssetsRepository.findById(productId)
                .flatMap(
                        productAssets ->
                                mediaContentRepository

                                        .findAllByProductId(productId)

                                        .flatMap(mediaContentRepository::delete)

                                        .then()

                                        .flatMap(
                                            nothing -> productAssetsRepository.delete(productAssets)
                                        )
                );

    }

    @Override
    public Mono<ProductAssets> initAssetsProduct(Long productId, Integer shopId) {
        ProductAssets build = ProductAssets.builder()
                .productId(productId)
                .shopId(shopId)
                .build();

        return productAssetsRepository.save(build);
    }
}
