package com.shishir.ecommerce.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_SECURITY_SCHEME = "Bearer Authentication";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("E-Commerce REST API")
                        .version("1.0.0")
                        .description("Complete REST API for e-commerce platform with JWT authentication")
                        .contact(new Contact()
                                .name("Shishir")
                                .email("softdevshishir@gmail.com")
                                .url("https://github.com/softDevShishir"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development server"),
                        new Server().url("https://ecommerce-api-9236.onrender.com").description("Production server")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SECURITY_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token for API authentication")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SECURITY_SCHEME));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public-apis")
                .displayName("Public Endpoints")
                .pathsToMatch(Routes.AUTH + "/**", Routes.PRODUCTS, Routes.PRODUCT_BY_ID)
                .build();
    }

    @Bean
    public GroupedOpenApi securedApi() {
        return GroupedOpenApi.builder()
                .group("secured-apis")
                .displayName("Protected Endpoints (Require JWT)")
                .pathsToMatch(Routes.USERS + "/**", Routes.ORDERS + "/**", Routes.CART + "/**")
                .addOpenApiCustomizer(openApi -> openApi.addSecurityItem(
                        new SecurityRequirement().addList(BEARER_SECURITY_SCHEME)))
                .build();
    }
}
