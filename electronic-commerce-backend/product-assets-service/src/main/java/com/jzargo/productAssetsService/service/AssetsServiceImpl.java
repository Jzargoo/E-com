package com.jzargo.productAssetsService.service;

import com.jzargo.productAssetsService.entity.ProductAssets;
import com.jzargo.productAssetsService.exception.AssetNotFoundException;
import com.jzargo.productAssetsService.exception.AssetsAlreadyExistsException;
import com.jzargo.productAssetsService.repository.MediaContentRepository;
import com.jzargo.productAssetsService.repository.ProductAssetsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@Transactional(readOnly = true)
public class AssetsServiceImpl implements AssetsService{

    private final ProductAssetsRepository productAssetsRepository;
    private final MediaContentRepository mediaContentRepository;

    public AssetsServiceImpl(ProductAssetsRepository productAssetsRepository, MediaContentRepository mediaContentRepository) {
        this.productAssetsRepository = productAssetsRepository;
        this.mediaContentRepository = mediaContentRepository;
    }

    @Override
    @Transactional
    public Mono<Void> initAssetsCompensation(Long productId) {
        Mono<ProductAssets> byId = productAssetsRepository.findById(productId);

        return byId
                .switchIfEmpty(
                        Mono.error(new AssetNotFoundException("Asset with product id was not found: " + productId))
                )
                .flatMap(
                        productAssets ->
                                mediaContentRepository

                                        .findAllByProductAssetsId(productAssets.getId())

                                        .flatMap(mediaContentRepository::delete)

                                        .then(productAssetsRepository.delete(productAssets))
                );

    }

    @Override
    @Transactional
    public Mono<ProductAssets> initAssetsProduct(Long productId, Integer shopId) {

        return productAssetsRepository.existsByProductId(productId)
                .flatMap(
                        value -> {
                            if (value) {
                                return Mono.error(
                                        new AssetsAlreadyExistsException("Product with product id " + productId + " already exists")
                                );
                            }

                            ProductAssets build = ProductAssets.builder()
                                    .productId(productId)
                                    .shopId(shopId)
                                    .build();


                            return productAssetsRepository.save(build);
                        }
                );
    }
}
