package com.momo.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenApiConfigTest {

    @Test
    void registersBearerJwtSecurityScheme() {
        SecurityScheme scheme = OpenApiConfig.class.getAnnotation(SecurityScheme.class);
        OpenAPIDefinition definition = OpenApiConfig.class.getAnnotation(OpenAPIDefinition.class);

        assertEquals(OpenApiConfig.BEARER_AUTH, scheme.name());
        assertEquals(SecuritySchemeType.HTTP, scheme.type());
        assertEquals("bearer", scheme.scheme());
        assertEquals("JWT", scheme.bearerFormat());
        assertEquals(OpenApiConfig.BEARER_AUTH, definition.security()[0].name());
    }
}
