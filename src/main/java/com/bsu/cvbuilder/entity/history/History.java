package com.bsu.cvbuilder.entity.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "chat")
public class History {

    @Id
    private String id;

    @DBRef
    private String sessionId;

    @DBRef
    private String userId;

    private Map<Object, Object> history;
}
