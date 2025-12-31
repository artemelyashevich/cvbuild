package com.bsu.cvbuilder.configuration;

import com.bsu.cvbuilder.filter.AuthFilter;
import com.bsu.cvbuilder.security.exception.CustomAccessDeniedHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.security.oauth2",
        name = "enabled",
        havingValue = "false"
)
public class SecurityCommonConfiguration {

    private final AuthFilter authFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(CorsConfigurer::disable)
                .authorizeHttpRequests(auth ->
                                auth
                                        .anyRequest().permitAll()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(new CustomAccessDeniedHandler())
                )
                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
