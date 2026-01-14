package com.bsu.cvbuilder.domain.dto.chat;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnswerRequest {
    private String value;
}
