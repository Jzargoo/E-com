package com.jzargo.productservice.repository;

import com.jzargo.productservice.entity.SagaProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SagaProductCreationRepository extends JpaRepository<SagaProductEntity, Long> {
}
