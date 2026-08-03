package com.jzargo.productservice.repository;

import com.jzargo.productservice.entity.FallbackMediaContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface FallbackMediaContentRepository extends JpaRepository<FallbackMediaContent, Long> {

    Optional<FallbackMediaContent> findFirstByMediaUriIsNotNull();
}
