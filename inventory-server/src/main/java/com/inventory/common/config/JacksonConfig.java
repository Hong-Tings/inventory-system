package com.inventory.common.config;

import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            // LocalDateTime serialization/deserialization
            builder.serializerByType(LocalDateTime.class,
                    new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
            builder.deserializerByType(LocalDateTime.class,
                    new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));

            // LocalDate serialization/deserialization
            builder.serializerByType(LocalDate.class,
                    new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));
            builder.deserializerByType(LocalDate.class,
                    new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));
        };
    }
}
