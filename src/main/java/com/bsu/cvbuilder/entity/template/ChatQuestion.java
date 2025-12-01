package com.bsu.cvbuilder.entity.template;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatQuestion {

    private String question;

    private String locale;

    private LocalDateTime createdAt;
}
