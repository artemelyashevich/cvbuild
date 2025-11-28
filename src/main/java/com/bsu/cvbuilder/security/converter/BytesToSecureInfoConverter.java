package com.bsu.cvbuilder.security.converter;

import com.bsu.cvbuilder.entity.security.SecureData;
import com.bsu.cvbuilder.exception.AppException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.lang.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@ReadingConverter
public class BytesToSecureInfoConverter implements Converter<byte[], SecureData.SecureInfo> {

    private final ObjectMapper objectMapper;

    public BytesToSecureInfoConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public SecureData.SecureInfo convert(@NonNull byte[] source) {
        try {
            return objectMapper.readValue(source, SecureData.SecureInfo.class);
        } catch (IOException e) {
            throw new AppException("Error deserializing bytes to SecureInfo", e, 500);
        }
    }
}