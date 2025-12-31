package com.bsu.cvbuilder.util;

import com.bsu.cvbuilder.exception.AppException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
@UtilityClass
public class JsonHelper {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String FAILED = "Failed to convert object to json string: {}";

    public static String toBeautifulJson(Object o) {
        if (o == null) {
            return "null";
        } else {
            try {
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(o);
            } catch (Exception ex) {
                log.warn(FAILED, ex.getMessage());
                return null;
            }
        }
    }

    public static String toJson(Object o) {
        if (o == null) {
            return "null";
        } else {
            try {
                return mapper.writeValueAsString(o);
            } catch (Exception ex) {
                log.warn(FAILED, ex.getMessage());
                return null;
            }
        }
    }

    public static Object fromJson(String json, Class valueClass) {
        if (json == null) {
            return null;
        } else {
            try {
                return mapper.readValue(json, valueClass);
            } catch (Exception ex) {
                log.warn(FAILED, ex.getMessage());
                return null;
            }
        }
    }

    public static <T> T parseString(String json, Class<T> valueClass) {
        try {
            Objects.requireNonNull(json, "json is null");
            return mapper.readValue(json, valueClass);
        } catch (Exception e) {
            throw new AppException("Can't parse string '" + json + "'", 500);
        }
    }
}