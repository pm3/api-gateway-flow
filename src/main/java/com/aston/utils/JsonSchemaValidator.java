package com.aston.utils;

import java.io.IOException;
import java.io.Reader;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

public class JsonSchemaValidator {

    private final ObjectMapper objectMapper;

    public JsonSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String validate(Reader data, String schema) throws IOException {
        JsonNode dataNode = objectMapper.readTree(data);
        JsonNode schemaNode = objectMapper.readTree(schema);

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        JsonSchema jsonSchema = factory.getSchema(schemaNode);
        jsonSchema.initializeValidators();
        Set<ValidationMessage> result = jsonSchema.validate(dataNode);
        return result.stream().map(ValidationMessage::getMessage).collect(Collectors.joining(", "));
    }
}
