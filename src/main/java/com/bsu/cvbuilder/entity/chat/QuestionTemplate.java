package com.bsu.cvbuilder.entity.chat;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "question_templates")
public class QuestionTemplate {

    private String id;
    private String name;
    private Difficulty difficulty;
    private Integer cost;
    private Integer regenerationCost;
    private List<ChatQuestion> questions;
}
