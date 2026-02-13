package com.bsu.cvbuilder.domain.event;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public abstract class AbstractEvent implements Serializable {

    @Getter
    @ToString.Include
    private final String userId;

    @ToString.Exclude
    private final transient ThreadLocal<Object> data;

    public void setData(@NonNull Object value) {
        data.set(value);
    }

    public Map<String, Object> getData() {
        Map<String, Object> map = new HashMap<>();
        map.put("event", this.getClass().getSimpleName());
        if (data == null) {
            return map;
        }
        Optional<Object> result = Optional.ofNullable(data.get());
        result.ifPresent(o -> map.put("data", o));
        data.remove();
        return map;
    }

    public AbstractEvent(String userId) {
        this.userId = userId;
        data = new ThreadLocal<>();
    }
}
