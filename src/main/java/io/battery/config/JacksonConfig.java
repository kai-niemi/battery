package io.battery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

@Configuration
public class JacksonConfig {
    @Bean
    @Primary
    public JsonMapper primaryObjectMapper() {
        return JsonMapper.builder()
                .enable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // strict
                .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                .build();
    }

    @Bean
    public YAMLMapper yamlObjectMapper() {
        return YAMLMapper.builder()
                .enable(YAMLWriteFeature.ALLOW_LONG_KEYS)
                .enable(YAMLWriteFeature.LITERAL_BLOCK_STYLE)
                .enable(YAMLWriteFeature.SPLIT_LINES)
                .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
                .disable(YAMLWriteFeature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS)

                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // strict
                .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                .enable(SerializationFeature.INDENT_OUTPUT)

                .changeDefaultPropertyInclusion(include -> include.withValueInclusion(JsonInclude.Include.NON_NULL))
                .changeDefaultPropertyInclusion(include -> include.withValueInclusion(JsonInclude.Include.NON_EMPTY))

                .build();
    }

}
