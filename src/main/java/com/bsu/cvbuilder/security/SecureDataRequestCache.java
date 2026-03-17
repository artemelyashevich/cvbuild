package com.bsu.cvbuilder.security;

import com.bsu.cvbuilder.domain.entity.SecureData;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Getter
@Setter
@Component
@RequestScope
public class SecureDataRequestCache {

    private SecureData secureData;
}