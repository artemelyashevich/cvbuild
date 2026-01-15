package com.bsu.cvbuilder.security.filter;

import com.bsu.cvbuilder.domain.dto.auth.TokenType;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.util.HandleSecurityErrorUtil;
import com.bsu.cvbuilder.util.PathUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static com.bsu.cvbuilder.util.OAuthUtil.getOAuth2AuthenticationToken;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthFilter extends OncePerRequestFilter {

    private final SecurityService securityService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            if (true) {
                filterChain.doFilter(request, response);
                return;
            }
            var authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new AppException("There are no access token", 401);
            }

            var authToken = authHeader.substring(7);

            if (authToken.isBlank()) {
                throw new AppException("Empty access token", 401);
            }

            securityService.checkToken(authToken, TokenType.ACCESS);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                var ctx = SecurityContextHolder.createEmptyContext();
                var login = securityService.extractSubject(authToken);
                OAuth2AuthenticationToken authentication = getOAuth2AuthenticationToken(login);
                ctx.setAuthentication(authentication);
                SecurityContextHolder.setContext(ctx);
                securityService.findCurrentUser();
            }
        } catch (AppException e) {
            log.warn(e.getMessage());
            HandleSecurityErrorUtil.handleError(response, e).getWriter().flush();
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return PathUtil.PUBLIC_RESOURCES.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
}
