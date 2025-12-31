package com.bsu.cvbuilder.configuration;

import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.filter.AuthFilter;
import com.bsu.cvbuilder.security.exception.CustomAccessDeniedHandler;
import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.util.HandleSecurityErrorUtil;
import com.bsu.cvbuilder.util.PathUtil;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.security.oauth2",
        name = "enabled",
        havingValue = "true"
)
public class SecurityOAuth2Configuration {

    private final SecurityService securityService;
    private final AuthFilter authFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(CorsConfigurer::disable)
                .authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers(PathUtil.PUBLIC_RESOURCES.toArray(new String[0])).permitAll()
                                .anyRequest().authenticated()
                )
                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2Login -> oauth2Login.successHandler(
                        (request, response, authentication) -> {
                            try {
                                var authResponse = securityService.authenticate(authentication);
                                response.addCookie(new Cookie("access-token", authResponse.accessToken()));
                                response.addCookie(new Cookie("refresh-token", authResponse.accessToken()));
                            } catch (AppException e) {
                                HandleSecurityErrorUtil.handleError(response, e);
                            } catch (Exception e) {
                                HandleSecurityErrorUtil.handleError(response, new AppException(e, 500));
                            }
                        }
                ))
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(new CustomAccessDeniedHandler())
                )
                .logout(l -> securityService.logout())
                .build();
    }
}