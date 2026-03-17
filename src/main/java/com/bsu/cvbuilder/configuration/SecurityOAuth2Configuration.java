package com.bsu.cvbuilder.configuration;

import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.security.filter.AuthFilter;
import com.bsu.cvbuilder.security.exception.CustomAccessDeniedHandler;
import com.bsu.cvbuilder.security.filter.RateLimitFilter;
import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.util.HandleSecurityErrorUtil;
import com.bsu.cvbuilder.util.OAuthUtil;
import com.bsu.cvbuilder.util.PathUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.io.IOException;

@Slf4j
@Profile("!test")
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.security.oauth2",
        name = "enabled",
        havingValue = "true"
)
public class SecurityOAuth2Configuration {

    private final SecurityService securityService;
    private final AuthFilter authFilter;
    private final RateLimitFilter rateLimitFilter;
    private final CustomCorsConfiguration corsConfiguration;
    private final ApplicationProperties applicationProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfiguration))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PathUtil.PUBLIC_RESOURCES).permitAll()
                        .requestMatchers(PathUtil.AUTH_RESOURCES).authenticated()
                        .requestMatchers(PathUtil.ADMIN_RESOURCES).hasRole(UserProfile.Role.ADMIN.name())
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(this::onAuthenticationSuccess)
                )
                .exceptionHandling(ex -> ex.accessDeniedHandler(new CustomAccessDeniedHandler()))
                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, AuthFilter.class)
                .logout(logout -> logout
                        .logoutUrl(PathUtil.LOGOUT_URL)
                        .deleteCookies(OAuthUtil.ACCESS_TOKEN, OAuthUtil.REFRESH_TOKEN)
                        .logoutSuccessHandler((req, res, auth) -> SecurityContextHolder.clearContext())
                )
                .build();
    }

    private void onAuthenticationSuccess(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        try {
            var authResponse = securityService.authenticate(authentication);

            response.addCookie(
                    createCookie(
                            OAuthUtil.ACCESS_TOKEN,
                            authResponse.getAccessToken(),
                            applicationProperties.getSecurity().getAccessMaxAgeCookie(),
                            false
                    )
            );
            response.addCookie(
                    createCookie(
                            OAuthUtil.REFRESH_TOKEN,
                            authResponse.getRefreshToken(),
                            applicationProperties.getSecurity().getRefreshMaxAgeCookie(),
                            true
                    )
            );

            response.sendRedirect(applicationProperties.getSecurity().getOauthRedirectUrl());
        } catch (Exception e) {
            log.error("OAuth2 Login Error: ", e);
            HandleSecurityErrorUtil.handleError(response, e);
        }
    }

    private Cookie createCookie(String name, String value, int maxAge, boolean httpOnly) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(httpOnly);
        cookie.setMaxAge(maxAge);
        return cookie;
    }
}