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

@SpringJUnitWebConfig(classes = BindingControllerTest.TestConfig.class)
class BindingControllerTest {

    @Configuration
    @EnableWebMvc
    @Import({ SecurityConfig.class, RestSecurityErrorHandler.class, GlobalExceptionHandler.class,
            BindingController.class })
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

    @Test
    void crearBindingDevuelve201() throws Exception {
        when(service.createBinding(any())).thenReturn(new BindingResponse("ex", "q", "k.1"));

        mockMvc.perform(post("/api/rabbit/bindings").with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"exchange\":\"ex\",\"queue\":\"q\",\"routingKey\":\"k.1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.routingKey").value("k.1"));

        verify(service).createBinding(new BindingRequest("ex", "q", "k.1"));
    }

    @Test
    void crearBindingConColaInexistenteDevuelve404() throws Exception {
        when(service.createBinding(any())).thenThrow(new QueueNotFoundException("q"));

        mockMvc.perform(post("/api/rabbit/bindings").with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"exchange\":\"ex\",\"queue\":\"q\",\"routingKey\":\"k\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void jsonMalformadoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/rabbit/bindings").with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{no es json"))
                .andExpect(status().isBadRequest());

        verify(service, never()).createBinding(any());
    }

    @Test
    void listarSinFiltroPideTodos() throws Exception {
        when(service.listBindings(null)).thenReturn(List.of(new BindingResponse("ex", "q", "k")));

        mockMvc.perform(get("/api/rabbit/bindings").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listarConFiltroDeCola() throws Exception {
        when(service.listBindings("q")).thenReturn(List.of());

        mockMvc.perform(get("/api/rabbit/bindings").param("queue", "q").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(service).listBindings("q");
    }

    @Test
    void eliminarBindingDevuelve204() throws Exception {
        mockMvc.perform(delete("/api/rabbit/bindings").with(admin())
                .param("exchange", "ex").param("queue", "q").param("routingKey", "crew.#"))
                .andExpect(status().isNoContent());

        verify(service).deleteBinding(new BindingRequest("ex", "q", "crew.#"));
    }

    @Test
    void eliminarBindingSinRoutingKeyUsaVacia() throws Exception {
        mockMvc.perform(delete("/api/rabbit/bindings").with(admin())
                .param("exchange", "ex").param("queue", "q"))
                .andExpect(status().isNoContent());

        verify(service).deleteBinding(new BindingRequest("ex", "q", ""));
    }

    @Test
    void eliminarBindingSinParametroObligatorioDevuelve400() throws Exception {
        mockMvc.perform(delete("/api/rabbit/bindings").with(admin()).param("exchange", "ex"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Falta el parámetro obligatorio 'queue'"));

        verify(service, never()).deleteBinding(any());
    }

    @Test
    void eliminarBindingInexistenteDevuelve404() throws Exception {
        doThrow(new BindingNotFoundException("ex", "q", "k")).when(service).deleteBinding(any());

        mockMvc.perform(delete("/api/rabbit/bindings").with(admin())
                .param("exchange", "ex").param("queue", "q").param("routingKey", "k"))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarBindingProtegidoDevuelve409() throws Exception {
        doThrow(new ProtectedResourceException("binding", "cmd.direct->q.cmd.email")).when(service).deleteBinding(any());

        mockMvc.perform(delete("/api/rabbit/bindings").with(admin())
                .param("exchange", "cmd.direct").param("queue", "q.cmd.email").param("routingKey", "email.send"))
                .andExpect(status().isConflict());
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/rabbit/bindings")).andExpect(status().isUnauthorized());
    }

    @Test
    void sinRolAdminDevuelve403() throws Exception {
        mockMvc.perform(delete("/api/rabbit/bindings")
                .param("exchange", "e").param("queue", "q")
                .with(jwt().authorities(new SimpleGrantedAuthority("Operador"))))
                .andExpect(status().isForbidden());
    }
}
