package com.jzargo.productservice;

import com.jzargo.protobuf.MediaServiceGrpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.grpc.client.ImportGrpcClients;

@SpringBootApplication
@EnableConfigurationProperties
@ImportGrpcClients(types = MediaServiceGrpc.MediaServiceStub.class)
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }

}