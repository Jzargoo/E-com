package com.jzargo.productAssetsService.repository;

import com.jzargo.productAssetsService.entity.Avatar;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface AvatarRepository extends R2dbcRepository<Avatar,Long> {
}
