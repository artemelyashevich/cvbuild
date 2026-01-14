package com.bsu.cvbuilder.domain.dto;

import lombok.Builder;

@Builder
public record EmailDto(
        String receiver,
        String activationCode,
        String sender,
        String template
) {
}
