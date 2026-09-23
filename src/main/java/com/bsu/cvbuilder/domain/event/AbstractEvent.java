package com.bsu.cvbuilder.domain.event;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public abstract class AbstractEvent implements Serializable {

    @Getter
    @ToString.Include
    private final String userId;

    @ToString.Exclude
    private transient volatile Object data;

    public void setData(@NonNull Object value) {
        data = value;
    }

    public Map<String, Object> getData() {
        Map<String, Object> map = new HashMap<>();
        map.put("event", this.getClass().getSimpleName());
        Object value = data;
        if (value != null) {
            map.put("data", value);
        }
        return map;
    }

    public AbstractEvent(String userId) { // NOSONAR
        this.userId = userId;
    }
}
