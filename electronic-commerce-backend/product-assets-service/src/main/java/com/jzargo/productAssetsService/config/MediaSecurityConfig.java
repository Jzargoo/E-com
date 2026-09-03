package com.jzargo.productAssetsService.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

@Configuration
public class MediaSecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {

        return http

                .authorizeExchange(
                        exchange -> exchange
                                .pathMatchers(HttpMethod.GET, "/api/products/media/**")
                                .permitAll()

                                .pathMatchers(HttpMethod.PUT, "/api/products/media/*")
                                .hasAnyRole("WORKER", "SCOPE_WORKER", "OWNER", "SCOPE_OWNER")

                )

                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                .oauth2ResourceServer(spec -> spec
                        .jwt(Customizer.withDefaults()))

                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(ApplicationPropertyStorage applicationPropertyStorage) {
        String jwks =
                applicationPropertyStorage.getSecurity().getJwksUri();

        return NimbusJwtDecoder.withJwkSetUri(jwks).build();
    }

    @Bean
    public JwtAuthenticationConverter converter() {

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

        jwtAuthenticationConverter.setPrincipalClaimName("user_id");

        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
                jwt -> {
                    Collection<GrantedAuthority> authorities= jwtGrantedAuthoritiesConverter.convert(jwt);

                    List<String> roles = (List<String>) jwt.getClaimAsMap("realm_access").getOrDefault("roles", List.of());

                    return Stream
                            .concat(
                                    authorities.stream(),
                                    roles.stream()
                                            .filter(role -> role.startsWith("ROLE_"))
                                            .map(SimpleGrantedAuthority::new)
                                            .map(GrantedAuthority.class::cast)
                            ).toList();
                }
        );

        return jwtAuthenticationConverter;

    }
}
