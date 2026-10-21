package cl.barriodigital.rabbitadmin.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import cl.barriodigital.rabbitadmin.config.SecurityConfig;
import cl.barriodigital.rabbitadmin.exception.GlobalExceptionHandler;
import cl.barriodigital.rabbitadmin.security.RestSecurityErrorHandler;
import cl.barriodigital.rabbitadmin.service.RabbitAdminService;

/** Una entrada inválida se rechaza con 400 y un {@code ApiError} legible, sin llegar al servicio. */
@SpringJUnitWebConfig(classes = ValidationErrorsTest.TestConfig.class)
class ValidationErrorsTest {

    @Configuration
    @EnableWebMvc
    @Import({ SecurityConfig.class, RestSecurityErrorHandler.class, GlobalExceptionHandler.class,
            QueueController.class, ExchangeController.class, BindingController.class })
    static class TestConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                throw new BadJwtException("decoder de prueba");
            };
        }

        @Bean
        RabbitAdminService rabbitAdminService() {
            return mock(RabbitAdminService.class);
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RabbitAdminService service;

    private MockMvc mockMvc;

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("Admin"));
    }

    @BeforeEach
    void setUp() {
        reset(service);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private ResultActions send(String path, String json) throws Exception {
        return mockMvc.perform(post(path).with(admin()).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    // ---------------------------------------------------------------- colas

    @Test
    void colaConNombreVacioSeRechaza() throws Exception {
        send("/api/rabbit/queues", "{\"name\":\"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.message").value(containsString("name:")));
        verifyNoInteractions(service);
    }

    @Test
    void colaSinNombreSeRechaza() throws Exception {
        send("/api/rabbit/queues", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").value("El nombre es obligatorio"));
        verifyNoInteractions(service);
    }

    @Test
    void colaConEspaciosEnElNombreSeRechaza() throws Exception {
        send("/api/rabbit/queues", "{\"name\":\"cola con espacios\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").value(containsString("sin espacios")));
        verifyNoInteractions(service);
    }

    @Test
    void colaConPrefijoReservadoSeRechaza() throws Exception {
        send("/api/rabbit/queues", "{\"name\":\"amq.cola\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").value(containsString("amq.")));
        verifyNoInteractions(service);
    }

    @Test
    void colaConTtlNegativoSeRechaza() throws Exception {
        send("/api/rabbit/queues", "{\"name\":\"q.prueba\",\"messageTtlMs\":-5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.messageTtlMs").value(containsString("positivo")));
        verifyNoInteractions(service);
    }

    @Test
    void colaConTtlCeroSeRechaza() throws Exception {
        send("/api/rabbit/queues", "{\"name\":\"q.prueba\",\"messageTtlMs\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.messageTtlMs").exists());
        verifyNoInteractions(service);
    }

    @Test
    void variosErroresSeInformanJuntos() throws Exception {
        send("/api/rabbit/queues", "{\"name\":\"mala cola\",\"messageTtlMs\":-1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.messageTtlMs").exists())
                .andExpect(jsonPath("$.message").value(containsString("messageTtlMs:")))
                .andExpect(jsonPath("$.message").value(containsString("name:")));
        verifyNoInteractions(service);
    }

    @Test
    void ttlQueNoEsNumeroSeRechazaComoJsonInvalido() throws Exception {
        send("/api/rabbit/queues", "{\"name\":\"q.prueba\",\"messageTtlMs\":\"mucho\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
        verifyNoInteractions(service);
    }

    // ------------------------------------------------------------ exchanges

    @Test
    void exchangeConTipoInexistenteSeRechaza() throws Exception {
        send("/api/rabbit/exchanges", "{\"name\":\"ex.nuevo\",\"type\":\"RARO\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("DIRECT, TOPIC, FANOUT, HEADERS")));
        verifyNoInteractions(service);
    }

    @Test
    void exchangeSinTipoSeRechaza() throws Exception {
        send("/api/rabbit/exchanges", "{\"name\":\"ex.nuevo\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.type").exists());
        verifyNoInteractions(service);
    }

    @Test
    void exchangeConNombreInvalidoSeRechaza() throws Exception {
        send("/api/rabbit/exchanges", "{\"name\":\"ex nuevo\",\"type\":\"DIRECT\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
        verifyNoInteractions(service);
    }

    // -------------------------------------------------------------- bindings

    @Test
    void bindingConColaVaciaSeRechaza() throws Exception {
        send("/api/rabbit/bindings", "{\"exchange\":\"ex.a\",\"queue\":\"\",\"routingKey\":\"k\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.queue").exists());
        verifyNoInteractions(service);
    }

    @Test
    void bindingConRoutingKeyDemasiadoLargaSeRechaza() throws Exception {
        String json = "{\"exchange\":\"ex.a\",\"queue\":\"q.a\",\"routingKey\":\"" + "k".repeat(256) + "\"}";
        send("/api/rabbit/bindings", json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.routingKey").value("La routing key no puede superar los 255 caracteres"));
        verifyNoInteractions(service);
    }

    @Test
    void bindingSinExchangeNiColaSeRechaza() throws Exception {
        send("/api/rabbit/bindings", "{\"routingKey\":\"k\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.exchange").exists())
                .andExpect(jsonPath("$.fieldErrors.queue").exists());
        verifyNoInteractions(service);
    }

    // ------------------------------------------------------------ camino feliz

    @Test
    void unaEntradaValidaLlegaAlServicio() throws Exception {
        org.mockito.Mockito.when(service.createQueue(any())).thenReturn(
                new cl.barriodigital.rabbitadmin.dto.QueueResponse("q.ok", true, false, null, 0, 0, null, false));

        send("/api/rabbit/queues", "{\"name\":\"q.ok\",\"messageTtlMs\":1000}")
                .andExpect(status().isCreated());
    }
}
