package cl.barriodigital.rabbitadmin.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;

class OpenApiConfigTest {

    private final OpenAPI openApi = new OpenApiConfig().rabbitAdminOpenApi();

    @Test
    void declaraElEsquemaBearerJwt() {
        SecurityScheme scheme = openApi.getComponents().getSecuritySchemes().get(OpenApiConfig.BEARER_SCHEME);

        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(scheme.getScheme()).isEqualTo("bearer");
        assertThat(scheme.getBearerFormat()).isEqualTo("JWT");
    }

    @Test
    void todaLaApiExigeElEsquemaBearer() {
        assertThat(openApi.getSecurity()).hasSize(1);
        assertThat(openApi.getSecurity().get(0)).containsKey(OpenApiConfig.BEARER_SCHEME);
        assertThat(openApi.getInfo().getTitle()).contains("RabbitMQ");
    }
}
