package com.bsu.cvbuilder.domain.dto.template;

import java.util.List;
import java.util.Map;

public record CreateTemplateRequest(
        String name,
        Layout layout,
        Styles styles,
        Map<String, Object> defaultBlocks
) {

    public record Layout(
            List<String> sectionOrder,
            Integer columns
    ) {}

    public record Styles(
            String header,
            String sectionTitle,
            String text
    ) {}
}