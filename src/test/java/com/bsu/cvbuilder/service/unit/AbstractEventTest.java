package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.domain.event.LogoutEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbstractEventTest {

    @Test
    @DisplayName("getData: data set by the publisher is visible to other threads and to repeated reads")
    void getData_OtherThreadAndRepeatedRead_SeesData() throws Exception {
        var event = LogoutEvent.builder().userId("user-1").build();
        event.setData(Map.of("status", "success"));

        Map<String, Object> fromOtherThread = CompletableFuture.supplyAsync(event::getData).get();

        assertEquals(Map.of("status", "success"), fromOtherThread.get("data"));
        assertEquals(Map.of("status", "success"), event.getData().get("data"));
        assertEquals("LogoutEvent", event.getData().get("event"));
    }
}
