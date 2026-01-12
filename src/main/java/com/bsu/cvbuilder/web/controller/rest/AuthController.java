package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.entity.user.UserProfile;
import com.bsu.cvbuilder.service.SecurityService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static com.bsu.cvbuilder.util.OAuthUtil.getOAuth2AuthenticationToken;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.REGISTRATION_ID;


@Controller
@RequiredArgsConstructor
public class AuthController {

    private final SecurityService securityService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login")
    public void login(@RequestParam String login, @RequestParam String password, HttpServletResponse response) throws IOException {
        var token = getOAuth2AuthenticationToken(login);
        SecurityContextHolder.getContext().setAuthentication(token);
        var authResponse = securityService.authenticate(token);
        Cookie accessToken = new Cookie("access_token", authResponse.accessToken());
        accessToken.setPath("/");
        accessToken.setHttpOnly(false);
        accessToken.setMaxAge(3600);
        response.addCookie(accessToken);

        Cookie refreshToken = new Cookie("refresh_token", authResponse.accessToken());
        refreshToken.setPath("/");
        refreshToken.setHttpOnly(true);
        refreshToken.setMaxAge(604800);
        response.addCookie(refreshToken);
        response.sendRedirect("http://localhost:3000/profile");
    }
}
