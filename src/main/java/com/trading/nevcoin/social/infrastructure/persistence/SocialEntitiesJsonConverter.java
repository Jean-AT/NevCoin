package com.trading.nevcoin.social.infrastructure.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.nevcoin.social.domain.SocialEntity;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter
public class SocialEntitiesJsonConverter implements AttributeConverter<List<SocialEntity>, String> {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public String convertToDatabaseColumn(List<SocialEntity> value) {
        try {
            return value == null ? "[]" : mapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Could not serialize social entities", exception);
        }
    }

    @Override
    public List<SocialEntity> convertToEntityAttribute(String value) {
        try {
            return value == null || value.isBlank() ? List.of() : mapper.readValue(value, new TypeReference<>() { });
        } catch (Exception exception) {
            throw new IllegalArgumentException("Could not deserialize social entities", exception);
        }
    }
}
