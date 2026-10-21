package cl.barriodigital.rabbitadmin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import cl.barriodigital.rabbitadmin.config.SecurityConfig;
import cl.barriodigital.rabbitadmin.dto.BindingRequest;
import cl.barriodigital.rabbitadmin.dto.BindingResponse;
import cl.barriodigital.rabbitadmin.dto.CreateExchangeRequest;
import cl.barriodigital.rabbitadmin.dto.ExchangeResponse;
import cl.barriodigital.rabbitadmin.dto.ExchangeType;
import cl.barriodigital.rabbitadmin.exception.*;
import cl.barriodigital.rabbitadmin.security.RestSecurityErrorHandler;
import cl.barriodigital.rabbitadmin.service.RabbitAdminService;

@SpringJUnitWebConfig(classes = ExchangeControllerTest.TestConfig.class)
class ExchangeControllerTest {

    @Configuration
    @EnableWebMvc
    @Import({ SecurityConfig.class, RestSecurityErrorHandler.class, GlobalExceptionHandler.class,
            ExchangeController.class })
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

    private static ExchangeResponse exchange(String name) {
        return new ExchangeResponse(name, "topic", true, false, false);
    }

    @Test
    void crearExchangeDevuelve201ConLocationRelativa() throws Exception {
        when(service.createExchange(any())).thenReturn(exchange("ex.nuevo"));

        mockMvc.perform(post("/api/rabbit/exchanges").with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ex.nuevo\",\"type\":\"TOPIC\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/rabbit/exchanges/ex.nuevo"))
                .andExpect(jsonPath("$.name").value("ex.nuevo"))
                .andExpect(jsonPath("$.type").value("topic"));

        verify(service).createExchange(new CreateExchangeRequest("ex.nuevo", ExchangeType.TOPIC, null, null));
    }

    @Test
    void tipoDeExchangeInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/rabbit/exchanges").with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ex.nuevo\",\"type\":\"RARO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(service, never()).createExchange(any());
    }

    @Test
    void exchangeExistenteDevuelve409() throws Exception {
        when(service.createExchange(any())).thenThrow(new ExchangeAlreadyExistsException("ex.nuevo"));

        mockMvc.perform(post("/api/rabbit/exchanges").with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ex.nuevo\",\"type\":\"DIRECT\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void nombreReservadoDevuelve400() throws Exception {
        when(service.createExchange(any())).thenThrow(new InvalidTopologyException("Nombre reservado"));

        mockMvc.perform(post("/api/rabbit/exchanges").with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"amq.x\",\"type\":\"DIRECT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Nombre reservado"));
    }

    @Test
    void listarExchangesDevuelve200() throws Exception {
        when(service.listExchanges()).thenReturn(List.of(exchange("a"), exchange("b")));

        mockMvc.perform(get("/api/rabbit/exchanges").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("a"));
    }

    @Test
    void eliminarExchangeDevuelve204() throws Exception {
        mockMvc.perform(delete("/api/rabbit/exchanges/ex.nuevo").with(admin()))
                .andExpect(status().isNoContent());

        verify(service).deleteExchange("ex.nuevo");
    }

    @Test
    void eliminarExchangeInexistenteDevuelve404() throws Exception {
        doThrow(new ExchangeNotFoundException("ex.nuevo")).when(service).deleteExchange("ex.nuevo");

        mockMvc.perform(delete("/api/rabbit/exchanges/ex.nuevo").with(admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarExchangeProtegidoDevuelve409() throws Exception {
        doThrow(new ProtectedResourceException("exchange", "cmd.direct")).when(service).deleteExchange("cmd.direct");

        mockMvc.perform(delete("/api/rabbit/exchanges/cmd.direct").with(admin()))
                .andExpect(status().isConflict());
    }

    @Test
    void brokerCaidoDevuelve502() throws Exception {
        when(service.listExchanges()).thenThrow(new ManagementApiException("sin respuesta", null));

        mockMvc.perform(get("/api/rabbit/exchanges").with(admin()))
                .andExpect(status().isBadGateway());
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/rabbit/exchanges")).andExpect(status().isUnauthorized());
    }

    @Test
    void sinRolAdminDevuelve403() throws Exception {
        mockMvc.perform(get("/api/rabbit/exchanges")
                .with(jwt().authorities(new SimpleGrantedAuthority("Operador"))))
                .andExpect(status().isForbidden());
    }
}
