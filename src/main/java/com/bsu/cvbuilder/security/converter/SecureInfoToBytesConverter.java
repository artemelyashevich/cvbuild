package com.bsu.cvbuilder.security.converter;

import com.bsu.cvbuilder.entity.security.SecureData;
import com.bsu.cvbuilder.exception.AppException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.stereotype.Component;

@Component
@WritingConverter
public class SecureInfoToBytesConverter implements Converter<SecureData.SecureInfo, byte[]> {

    private final ObjectMapper objectMapper;

    public SecureInfoToBytesConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] convert(@NonNull SecureData.SecureInfo source) {
        try {
            return objectMapper.writeValueAsBytes(source);
        } catch (JsonProcessingException e) {
            throw new AppException("Error serializing SecureInfo to bytes", e, 500);
        }
    }
}
