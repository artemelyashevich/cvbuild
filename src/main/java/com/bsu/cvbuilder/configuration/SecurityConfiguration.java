package com.bsu.cvbuilder.configuration;

import com.bsu.cvbuilder.service.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final SecurityService securityService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(CorsConfigurer::disable)
                .authorizeHttpRequests(auth ->
                        auth
                                .anyRequest().permitAll()
                              //  .requestMatchers("/api/v1/auth/**").permitAll()
                               // .anyRequest().permitAll()//.authenticated()
                )
                .oauth2Login(oauth2Login -> oauth2Login.successHandler(
                        (request, response, authentication) -> securityService.authenticate(authentication)
                ))
                .logout(l -> securityService.logout())
                .build();
    }
}