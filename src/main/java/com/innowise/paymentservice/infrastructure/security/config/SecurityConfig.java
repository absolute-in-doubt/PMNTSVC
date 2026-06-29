package com.innowise.paymentservice.infrastructure.security.config;


import com.innowise.orderservice.infrastructure.security.converter.JwtConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Value("${application.security.jwksUrl}")
    private String jwksUrl;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtConverter jwtAuthenticationConverter,
                                           BearerTokenResolver bearerTokenResolver
    ) throws Exception {

        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .headers(Customizer.withDefaults())
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .servletApi(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().hasAnyAuthority("USER", "ADMIN")
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(bearerTokenResolver)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                )
                .anonymous(Customizer.withDefaults())
                .exceptionHandling(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwksUrl).build();
    }

    @Bean
    public BearerTokenResolver publicPathsBearerTokenResolver() {
        return new DefaultBearerTokenResolver();
    }
}
