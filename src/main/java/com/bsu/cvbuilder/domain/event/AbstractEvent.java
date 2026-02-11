package com.bsu.cvbuilder.domain.event;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Optional;

@Slf4j
public abstract class AbstractEvent implements Serializable {

    @Getter
    @ToString.Include
    private final String userId;

    @ToString.Exclude
    private transient ThreadLocal<String> login;

    public void setLogin(@NonNull String value) {
        login.set(value);
    }

    public Optional<String> getLogin() {
        if (login == null) {
            login = new ThreadLocal<>();
        }
        Optional<String> value = Optional.of(login.get());
        login.remove();
        return value;
    }

    public AbstractEvent(String userId) {
        this.userId = userId;
        login = new ThreadLocal<>();
    }
}
