package com.bsu.cvbuilder.configuration;

import com.bsu.cvbuilder.security.converter.BytesToSecureInfoConverter;
import com.bsu.cvbuilder.security.converter.SecureInfoToBytesConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class MongoConfiguration {

    @Bean
    public MongoCustomConversions customConversions(
            SecureInfoToBytesConverter secureInfoToBytesConverter,
            BytesToSecureInfoConverter bytesToSecureInfoConverter) {
        
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(secureInfoToBytesConverter);
        converters.add(bytesToSecureInfoConverter);
        
        return new MongoCustomConversions(converters);
    }
}