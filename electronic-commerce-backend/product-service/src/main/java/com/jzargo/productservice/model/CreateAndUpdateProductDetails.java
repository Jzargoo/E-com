package com.jzargo.productservice.model;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateAndUpdateProductDetails {
    private Long id;

    @NotNull(message = "name cannot be null")
    @NotEmpty(message = "name cannot be empty")
    private String name;
    private String avatarUri;

    @NotNull(message = "Price cannot be null")
    @Digits(integer = 10, fraction = 2, message =  "Product should have no more than 2 fraction")
    private BigDecimal price;

    private HashMap<String, String> characteristics;
    private String description;
    private String category;

}
