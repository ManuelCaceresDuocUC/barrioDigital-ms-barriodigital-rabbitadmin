package cl.barriodigital.rabbitadmin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import cl.barriodigital.rabbitadmin.config.SecurityConfig;
import cl.barriodigital.rabbitadmin.dto.PublishedMessageResponse;
import cl.barriodigital.rabbitadmin.exception.BrokerOperationException;
import cl.barriodigital.rabbitadmin.exception.GlobalExceptionHandler;
import cl.barriodigital.rabbitadmin.exception.InvalidMessageException;
import cl.barriodigital.rabbitadmin.security.RestSecurityErrorHandler;
import cl.barriodigital.rabbitadmin.service.MessagePublisherService;

@SpringJUnitWebConfig(classes = MessagePublisherControllerTest.TestConfig.class)
class MessagePublisherControllerTest {

    private static final String URL = "/internal/messages";
    private static final String VALID_BODY = """
            {"routingKey":"crew.ticket","type":"CREW_TICKET","correlationId":"15","traceId":"t-1",
             "payload":{"ticketId":"15","category":"Bache"}}""";

    @Configuration
    @EnableWebMvc
    @Import({ SecurityConfig.class, RestSecurityErrorHandler.class, GlobalExceptionHandler.class,
            MessagePublisherController.class })
    static class TestConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                throw new BadJwtException("decoder de prueba");
            };
        }

        @Bean
        MessagePublisherService messagePublisherService() {
            return mock(MessagePublisherService.class);
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MessagePublisherService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(service);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void publicaSinTokenYResponde202() throws Exception {
        when(service.publish(any())).thenReturn(new PublishedMessageResponse("evt-1", "cmd.direct", "crew.ticket"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.eventId").value("evt-1"))
                .andExpect(jsonPath("$.exchange").value("cmd.direct"))
                .andExpect(jsonPath("$.routingKey").value("crew.ticket"));
    }

    @Test
    void unCuerpoSinPayloadResponde400() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"routingKey\":\"crew.ticket\",\"type\":\"CREW_TICKET\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.payload").value("El payload es obligatorio"));

        verify(service, never()).publish(any());
    }

    @Test
    void unaRoutingKeyVaciaResponde400() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"routingKey\":\" \",\"type\":\"CREW_TICKET\",\"payload\":{}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.routingKey").value("La routing key es obligatoria"));
    }

    @Test
    void unaRoutingKeyNoPermitidaResponde400() throws Exception {
        when(service.publish(any())).thenThrow(new InvalidMessageException("La routing key 'x' no está permitida"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La routing key 'x' no está permitida"));
    }

    @Test
    void siRabbitMqNoRespondeResponde502() throws Exception {
        when(service.publish(any())).thenThrow(new BrokerOperationException("No se pudo publicar", new RuntimeException()));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadGateway());
    }

    @Test
    void unJsonInvalidoResponde400() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void lasDemasRutasInternasSiguenProtegidas() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }
}
