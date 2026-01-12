package com.bsu.cvbuilder.util;

import com.bsu.cvbuilder.entity.user.UserProfile;
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

    public static OAuth2AuthenticationToken getOAuth2AuthenticationToken(String login) {
        Map<String, Object> attributes = Map.of(
                "login", login,
                "sub", login
        );
        OAuth2User oAuth2User = new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + UserProfile.Role.USER.name())),
                attributes,
                "login"
        );
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oAuth2User,
                oAuth2User.getAuthorities(),
                REGISTRATION_ID
        );
        return authentication;
    }
}
