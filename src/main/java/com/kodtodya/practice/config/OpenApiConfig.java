package com.kodtodya.practice.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 (Swagger) configuration for the REST API.
 *
 * Browse the interactive UI at: http://localhost:8080/swagger-ui.html
 * Raw JSON spec at:            http://localhost:8080/v3/api-docs
 *
 * OpenAPI is the standard way to document REST APIs — it describes
 * all endpoints, request/response schemas, and error codes.
 * GraphQL has GraphiQL (built-in). gRPC has protobuf as its spec.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring API Showcase — REST")
                        .description("""
                                Demonstrates REST alongside GraphQL and gRPC on the same Product domain.
                                
                                - **REST**    → this page (HTTP/1.1 + JSON)
                                - **GraphQL** → http://localhost:8080/graphiql
                                - **gRPC**    → localhost:9090 (use grpcurl)
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("API Showcase")
                                .url("https://github.com/your-org/spring-api-showcase"))
                        .license(new License().name("MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development")));
    }
}