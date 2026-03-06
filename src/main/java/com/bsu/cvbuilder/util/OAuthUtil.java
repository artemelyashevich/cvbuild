package com.bsu.cvbuilder.util;

import com.bsu.cvbuilder.domain.entity.UserProfile;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collections;
import java.util.Map;

import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.REGISTRATION_ID;

@UtilityClass
public class OAuthUtil {

    public static OAuth2AuthenticationToken getOAuth2AuthenticationToken(String login, UserProfile.Role role, String token) {
        Map<String, Object> attributes = new java.util.HashMap<>(Map.of(
                "login", login,
                "sub", login
        ));
        if (token != null) {
            attributes.put("token", token);
        }
        OAuth2User oAuth2User = new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name())),
                attributes,
                "login"
        );
        return new OAuth2AuthenticationToken(
                oAuth2User,
                oAuth2User.getAuthorities(),
                REGISTRATION_ID
        );
    }
}
