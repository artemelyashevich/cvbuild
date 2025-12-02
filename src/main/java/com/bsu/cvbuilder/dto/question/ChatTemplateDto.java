package com.bsu.cvbuilder.dto.question;

import com.bsu.cvbuilder.entity.chat.ChatQuestion;
import com.bsu.cvbuilder.entity.chat.Difficulty;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatTemplateDto {
    private String id;
    private String name;
    private Difficulty difficulty;
    private Integer cost;
    private Integer regenerationCost;
    private List<ChatQuestion> questions;
}
