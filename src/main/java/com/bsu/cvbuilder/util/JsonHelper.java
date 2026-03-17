package com.bsu.cvbuilder.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

@Slf4j
@UtilityClass
public class JsonHelper {

    public static final ObjectMapper mapper = new ObjectMapper();

    private static final String FAILED = "Failed to convert object to json string: {}";

    static {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        mapper.setDateFormat(sdf);
        mapper.registerModule(new JavaTimeModule());
    }

    public static Object fromJson(String json, Class valueClass) {
        if (json == null) {
            return null;
        }
        try {
            Object result = mapper.readValue(json, valueClass);
            return result;
        } catch (Exception var4) {
            return null;
        }
    }

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