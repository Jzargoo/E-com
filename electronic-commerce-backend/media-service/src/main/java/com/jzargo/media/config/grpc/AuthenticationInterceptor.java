package com.jzargo.media.config.grpc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.security.AuthenticationProcessInterceptor;
import org.springframework.grpc.server.security.GrpcSecurity;
import org.springframework.security.config.Customizer;

@Configuration
public class AuthenticationInterceptor {

    @Bean
    @GlobalServerInterceptor
    public AuthenticationProcessInterceptor jwtAuthenticationProcessInterceptor(
            GrpcSecurity grpc
    ) throws Exception {

        return grpc
                .authorizeRequests(
                        requests -> requests
                                .methods(
                                        "MediaService/changeMediaFile",
                                        "MediaService/addMediaFile",
                                        "MediaService/existsByUri"
                                ).hasAnyAuthority("ROLE_SHOP_OWNER", "ROLE_WORKER")

                                .methods("grpc.*/*").permitAll()
                                .allRequests().denyAll()
                )
                .oauth2ResourceServer(Customizer.withDefaults())
                .preauth(Customizer.withDefaults())
                .build();

    }

}
