package com.bsu.cvbuilder.security;

import com.bsu.cvbuilder.domain.entity.UserProfile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {

    private final String id;
    private final String login;
    private final String email;
    private final UserProfile.Role role;

    public UserPrincipal(UserProfile user) {
        this.id = user.getId();
        this.login = user.getLogin();
        this.email = user.getEmail();
        this.role = user.getRole();
    }

    public String getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return login;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getPassword() {
        return "";
    }
}