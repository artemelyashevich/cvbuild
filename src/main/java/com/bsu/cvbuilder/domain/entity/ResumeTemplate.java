package com.bsu.cvbuilder.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "resume_templates")
public class ResumeTemplate {

    @Id
    private String id;

    private String name;

    private Layout layout;

    private Styles styles;

    private Map<String, Object> defaultBlocks;

    @CreatedDate
    private LocalDateTime createdAt;

    public static class Layout {
        private List<String> sectionOrder;
        private Integer columns;
    }

    public static class Styles {
        private String header;
        private String sectionTitle;
        private String text;
    }
}