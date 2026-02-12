package com.bsu.cvbuilder.security.filter;

import com.bsu.cvbuilder.domain.dto.auth.SecurityProvider;
import com.bsu.cvbuilder.domain.dto.auth.TokenType;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.exception.AuthTokenException;
import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.util.HandleSecurityErrorUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

import static com.bsu.cvbuilder.util.OAuthUtil.getOAuth2AuthenticationToken;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ACCESS_TOKEN_COOKIE = "access_token";

    private final SecurityService securityService;
    private final SecurityProvider securityProvider;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String token = resolveToken(request);

            if (StringUtils.hasText(token)) {
                authenticateRequest(token);
            }
        } catch (AppException | AuthTokenException e) {
            log.warn("Authentication failed: {}", e.getMessage());
            HandleSecurityErrorUtil.handleError(response, e);
            return;
        }

        filterChain.doFilter(request, response);
    }

//    @Override
//    protected boolean shouldNotFilter(HttpServletRequest request) {
//        String path = request.getRequestURI().substring(request.getContextPath().length());
//
//        boolean isPublic = Arrays.stream(PathUtil.PUBLIC_RESOURCES)
//                .anyMatch(p -> pathMatcher.match(p, request.getServletPath()));
//
//        if (isPublic) {
//            log.debug("Path {} is public, skipping filter", path);
//        }
//
//        return isPublic;
//    }

    private String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(cookie -> ACCESS_TOKEN_COOKIE.equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }

    private void authenticateRequest(String token) {
        securityService.checkToken(token, TokenType.ACCESS);

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String login = securityService.extractSubject(token);
            UserProfile.Role role = securityService.extractRole(token);
            OAuth2AuthenticationToken authentication = getOAuth2AuthenticationToken(login, role);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        securityProvider.setAuthentication(SecurityContextHolder.getContext().getAuthentication());
        securityProvider.setToken(token);
    }
}