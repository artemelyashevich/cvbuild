package com.bsu.cvbuilder.entity.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatQuestion {

    private Integer order;
    private String text;
    private String type;
    private Boolean required;
    private List<Condition> conditions;
    private List<String> options;
    private String hint;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Condition {
        private String questionId;
        private Operator operator;
        private String value;

        public enum Operator {
            EQUALS, NOT_EQUALS, CONTAINS
        }
    }
}
