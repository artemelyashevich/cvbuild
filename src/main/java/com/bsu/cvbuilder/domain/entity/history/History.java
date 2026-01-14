package com.bsu.cvbuilder.domain.entity.history;

import com.bsu.cvbuilder.domain.event.AbstractEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "history")
public class History {

    @Id
    private String id;

    private String userId;

    @Builder.Default
    private Map<LocalDateTime, AbstractEvent> history = new HashMap<>();
}
