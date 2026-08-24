package com.jzargo.productAssetsService;

import com.jzargo.protobuf.MediaServiceGrpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.grpc.client.ImportGrpcClients;
import org.springframework.web.reactive.config.EnableWebFlux;

@EnableWebFlux
@SpringBootApplication
@ImportGrpcClients(types = MediaServiceGrpc.MediaServiceStub.class)
public class ProductAssetsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductAssetsServiceApplication.class, args);
    }

}