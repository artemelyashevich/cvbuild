package com.bsu.cvbuilder.security.exception;

import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.util.HandleSecurityErrorUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, 
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        HandleSecurityErrorUtil.handleError(response, new AppException("There are no access token", 401)).getWriter().flush();
    }
}