package com.transporte.sistema.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * Personaliza el ObjectMapper de Spring Boot SIN reemplazarlo.
 * Acepta LocalDateTime con o sin segundos (formato del input datetime-local de HTML).
 *
 * USA Jackson2ObjectMapperBuilderCustomizer — NO crea un @Bean ObjectMapper nuevo
 * para no romper los módulos automáticos de Spring Boot (Hibernate, etc).
 */
@Configuration
public class JacksonConfig {

    /** Formato flexible: segundos opcionales → cubre "HH:mm" y "HH:mm:ss" */
    private static final DateTimeFormatter FLEXIBLE_DT = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm")
            .optionalStart()
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .optionalEnd()
            .toFormatter();

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .serializers(new LocalDateTimeSerializer(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")))
                .deserializers(new LocalDateTimeDeserializer(FLEXIBLE_DT));
    }
}
