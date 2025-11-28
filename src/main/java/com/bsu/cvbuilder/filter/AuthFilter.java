package com.bsu.cvbuilder.filter;

import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.IpAddressService;
import com.bsu.cvbuilder.service.SecurityService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthFilter implements Filter {

    private final SecurityService securityService;

    @Override
    public void doFilter(
            @NonNull ServletRequest servletRequest,
            @NonNull ServletResponse servletResponse,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        log.debug("-- AUTH FILTER -- start");
        var req = (HttpServletRequest) servletRequest;

        var authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AppException(401);
        }

        var authToken = authHeader.substring(7);
        if (authToken.isBlank()) {
            throw new AppException(401);
        }

        if (!securityService.isTokenValid(authToken)) {
            securityService.logout();
            throw new AppException(401);
        }

        if (securityService.isTokenExpire(authToken)) {
            securityService.logout();
            throw new AppException(401);
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            var ctx = SecurityContextHolder.createEmptyContext();
            var token = new UsernamePasswordAuthenticationToken(authToken, null, null);
            ctx.setAuthentication(token);
            SecurityContextHolder.setContext(ctx);
            securityService.findCurrentUser();
        }

        CompletableFuture.runAsync(securityService::checkSecureData);

        filterChain.doFilter(servletRequest, servletResponse);
    }
}
