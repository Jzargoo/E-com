package com.jzargo.productservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateAndUpdateProductDetails {
    private Long id;
    private String name;
    private String avatarUri;
    private Double price;
    private Integer shopId;
    private HashMap<String, String> characteristics;
    private String description;
    private String category;
}
