package com.jzargo.inventory.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableMethodSecurity
@EnableWebSecurity
@Profile("!test")
public class SecurityConfig {

    @Bean
    public SecurityFilterChain inventorySecurityFilterChain(HttpSecurity http) throws Exception {
        return http

                .oauth2ResourceServer(
                        (serverConfigurer) ->
                                serverConfigurer.jwt(Customizer.withDefaults())
                )


                .sessionManagement(
                        sessionManagement ->
                                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(
                        router ->
                                router
                                        .requestMatchers(HttpMethod.PUT, "/api/inventory").hasRole("OWNER")
                )

                .csrf(
                        AbstractHttpConfigurer::disable
                )

                .build();
    }

}
