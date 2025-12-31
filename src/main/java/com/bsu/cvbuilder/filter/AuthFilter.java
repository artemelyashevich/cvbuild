package com.bsu.cvbuilder.filter;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthFilter extends OncePerRequestFilter {

    private final SecurityService securityService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            var authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new AppException("There are no access token", 401);
            }

            var authToken = authHeader.substring(7);
            if (authToken.isBlank()) {
                throw new AppException("Empty access token", 401);
            }

            if (!securityService.isTokenValid(authToken)) {
                securityService.logout();
                throw new AppException("Invalid access token", 401);
            }

            if (securityService.isTokenExpire(authToken)) {
                securityService.logout();
                throw new AppException("Token expire", 401);
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                var ctx = SecurityContextHolder.createEmptyContext();
                var token = new UsernamePasswordAuthenticationToken(authToken, null, null);
                ctx.setAuthentication(token);
                SecurityContextHolder.setContext(ctx);
                securityService.findCurrentUser();
            }

            CompletableFuture.runAsync(securityService::checkSecureData);
        } catch (AppException e) {
            log.warn(e.getMessage());
            HandleSecurityErrorUtil.handleError(response, e).getWriter().flush();
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        return PathUtil.PUBLIC_RESOURCES.contains(path);
    }
}
