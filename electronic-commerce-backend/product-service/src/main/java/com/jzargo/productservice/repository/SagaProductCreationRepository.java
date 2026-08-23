package com.jzargo.productservice.repository;

import com.jzargo.productservice.entity.SagaProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SagaProductCreationRepository extends JpaRepository<SagaProductEntity, Long> {

    @Modifying
    @Query("UPDATE SagaProductEntity s SET s.status = 'EXPIRED', " +
            "s.errorMessage= :error WHERE s.status = 'PROCESSING' " +
            "AND s.expirationDate < CURRENT_TIMESTAMP")
    Integer checkAndSetExpiration(String error);
}
