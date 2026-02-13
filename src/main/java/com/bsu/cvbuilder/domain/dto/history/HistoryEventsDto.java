package com.bsu.cvbuilder.domain.dto.history;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HistoryEventsDto {
    private String id;
    private Map<String, String> events;
}