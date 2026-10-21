package cl.barriodigital.rabbitadmin.config;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import cl.barriodigital.rabbitadmin.security.RestSecurityErrorHandler;

/** La documentación OpenAPI se puede abrir sin token; el resto de la API sigue protegida. */
@SpringJUnitWebConfig(classes = OpenApiAccessTest.TestConfig.class)
class OpenApiAccessTest {

    @Configuration
    @EnableWebMvc
    @Import({ SecurityConfig.class, RestSecurityErrorHandler.class })
    static class TestConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                throw new BadJwtException("decoder de prueba");
            };
        }

        @Bean
        ProbeController probeController() {
            return new ProbeController();
        }
    }

    @RestController
    static class ProbeController {

        @GetMapping("/v3/api-docs")
        String docs() {
            return "{}";
        }

        @GetMapping("/swagger-ui/index.html")
        String ui() {
            return "<html></html>";
        }

        @GetMapping("/api/rabbit/otro")
        String protegido() {
            return "secreto";
        }
    }

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void laDefinicionOpenApiSeAbreSinToken() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void swaggerUiSeAbreSinToken() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void elRestoDeLaApiSigueExigiendoToken() throws Exception {
        mockMvc.perform(get("/api/rabbit/otro")).andExpect(status().isUnauthorized());
    }
}
