package cl.barriodigital.rabbitadmin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
import cl.barriodigital.rabbitadmin.dto.DeathInfo;
import cl.barriodigital.rabbitadmin.dto.DlqMessageResponse;
import cl.barriodigital.rabbitadmin.exception.GlobalExceptionHandler;
import cl.barriodigital.rabbitadmin.exception.InspectionNotAllowedException;
import cl.barriodigital.rabbitadmin.exception.InvalidTopologyException;
import cl.barriodigital.rabbitadmin.exception.QueueNotFoundException;
import cl.barriodigital.rabbitadmin.security.RestSecurityErrorHandler;
import cl.barriodigital.rabbitadmin.service.MessageInspectionService;

@SpringJUnitWebConfig(classes = MessageInspectionControllerTest.TestConfig.class)
class MessageInspectionControllerTest {

    private static final String URL = "/api/rabbit/queues/q.cmd.email.dlq/messages";

    @Configuration
    @EnableWebMvc
    @Import({ SecurityConfig.class, RestSecurityErrorHandler.class, GlobalExceptionHandler.class,
            MessageInspectionController.class })
    static class TestConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                throw new BadJwtException("decoder de prueba");
            };
        }

        @Bean
        MessageInspectionService messageInspectionService() {
            return mock(MessageInspectionService.class);
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MessageInspectionService service;

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
    void devuelveLosMensajesDeLaDlq() throws Exception {
        DeathInfo death = new DeathInfo("q.cmd.email", "cmd.direct", List.of("email.send"), "rejected", 1,
                Instant.parse("2026-10-25T12:00:00Z"));
        when(service.peek("q.cmd.email.dlq", null)).thenReturn(List.of(new DlqMessageResponse(
                "cmd.dead.dlx", "email.dlq", true, "{\"to\":\"m***@gmail.com\"}", false, 30, false, death)));

        mockMvc.perform(get(URL).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].routingKey").value("email.dlq"))
                .andExpect(jsonPath("$[0].redelivered").value(true))
                .andExpect(jsonPath("$[0].payload").value("{\"to\":\"m***@gmail.com\"}"))
                .andExpect(jsonPath("$[0].death.queue").value("q.cmd.email"))
                .andExpect(jsonPath("$[0].death.reason").value("rejected"))
                .andExpect(jsonPath("$[0].death.routingKeys[0]").value("email.send"));
    }

    @Test
    void pasaElCountAlServicio() throws Exception {
        when(service.peek(eq("q.cmd.email.dlq"), eq(5))).thenReturn(List.of());

        mockMvc.perform(get(URL).param("count", "5").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(service).peek("q.cmd.email.dlq", 5);
    }

    @Test
    void unCountFueraDeRangoResponde400() throws Exception {
        when(service.peek(any(), any())).thenThrow(new InvalidTopologyException("El parámetro 'count' debe estar entre 1 y 50"));

        mockMvc.perform(get(URL).param("count", "99").with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El parámetro 'count' debe estar entre 1 y 50"));
    }

    @Test
    void unCountQueNoEsNumeroResponde400() throws Exception {
        mockMvc.perform(get(URL).param("count", "abc").with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El parámetro 'count' no tiene un valor válido"));

        verify(service, never()).peek(any(), any());
    }

    @Test
    void unaColaQueNoEsDlqResponde403() throws Exception {
        when(service.peek(eq("q.cmd.email"), any())).thenThrow(new InspectionNotAllowedException("q.cmd.email"));

        mockMvc.perform(get("/api/rabbit/queues/q.cmd.email/messages").with(admin()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void unaColaInexistenteResponde404() throws Exception {
        when(service.peek(eq("q.otra.dlq"), any())).thenThrow(new QueueNotFoundException("q.otra.dlq"));

        mockMvc.perform(get("/api/rabbit/queues/q.otra.dlq/messages").with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La cola 'q.otra.dlq' no existe"));
    }

    @Test
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        verify(service, never()).peek(any(), any());
    }

    @Test
    void unOperadorNoPuedeLeerMensajes() throws Exception {
        mockMvc.perform(get(URL).with(jwt().authorities(new SimpleGrantedAuthority("Operador"))))
                .andExpect(status().isForbidden());
        verify(service, never()).peek(any(), any());
    }
}
