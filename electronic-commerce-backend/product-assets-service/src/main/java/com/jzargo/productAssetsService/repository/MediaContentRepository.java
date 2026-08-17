package com.jzargo.productAssetsService.repository;

import com.jzargo.productAssetsService.entity.MediaContent;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaContentRepository extends R2dbcRepository<MediaContent, Long> {

}
