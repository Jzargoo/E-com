package com.jzargo.productservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Validated
@Data
public class ProductDetails {
    private String category;
    private String name;
    private Float avgRate;
    private BigDecimal price;
    private String description;
    private Integer shopId;
    private Map<String, String> characteristics;
}
