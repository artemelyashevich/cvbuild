package com.bsu.cvbuilder.dto;

import lombok.Builder;

@Builder
public record EmailDto(
        String receiver,
        String activationCode,
        String sender,
        String template
) {
}
