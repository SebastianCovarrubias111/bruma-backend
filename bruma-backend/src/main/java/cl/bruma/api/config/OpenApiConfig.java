package cl.bruma.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "API Cervecería Bruma",
        version = "v1",
        description = "API REST de la tienda online Cervecería Bruma: catálogo, usuarios, órdenes (boletas) y autenticación JWT con roles ADMIN, VENDEDOR y CLIENTE."))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Pega aquí el accessToken obtenido en POST /api/v1/auth/login (sin la palabra Bearer).")
public class OpenApiConfig {
}
