package com.careerpilot.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI careerPilotOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CareerPilot AI API")
                        .description("Production-grade AI-powered job tracking and interview preparation platform")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("CareerPilot Team")
                                .email("support@careerpilot.ai"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://careerpilot.ai/terms"))
                )
                .servers(List.of(
                        new Server().url("http://localhost:8080/api").description("Local Development"),
                        new Server().url("https://api.careerpilot.ai").description("Production")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token obtained from /api/auth/login")
                        )
                );
    }
}
