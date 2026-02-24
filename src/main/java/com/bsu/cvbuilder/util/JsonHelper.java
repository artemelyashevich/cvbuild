package com.bsu.cvbuilder.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class JsonHelper {

    public static final ObjectMapper mapper = new ObjectMapper();

    private static final String FAILED = "Failed to convert object to json string: {}";

    public static String toJson(Object o) {
        if (o == null) {
            return "null";
        }
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception ex) {
            log.warn(FAILED, ex.getMessage());
            return null;
        }
    }
}