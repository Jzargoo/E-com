package com.jzargo.productAssetsService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.config.EnableWebFlux;

@EnableWebFlux
@SpringBootApplication
public class ProductAssetsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductAssetsServiceApplication.class, args);
    }

}