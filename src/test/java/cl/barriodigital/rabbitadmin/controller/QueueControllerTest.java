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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
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
import cl.barriodigital.rabbitadmin.dto.CreateQueueRequest;
import cl.barriodigital.rabbitadmin.dto.QueueResponse;
import cl.barriodigital.rabbitadmin.exception.BrokerOperationException;
import cl.barriodigital.rabbitadmin.exception.GlobalExceptionHandler;
import cl.barriodigital.rabbitadmin.exception.InvalidTopologyException;
import cl.barriodigital.rabbitadmin.exception.ProtectedResourceException;
import cl.barriodigital.rabbitadmin.exception.QueueAlreadyExistsException;
import cl.barriodigital.rabbitadmin.exception.QueueNotFoundException;
import cl.barriodigital.rabbitadmin.security.RestSecurityErrorHandler;
import cl.barriodigital.rabbitadmin.service.RabbitAdminService;

@SpringJUnitWebConfig(classes = QueueControllerTest.TestConfig.class)
class QueueControllerTest {

    @Configuration
    @EnableWebMvc
    @Import({ SecurityConfig.class, RestSecurityErrorHandler.class, GlobalExceptionHandler.class,
            QueueController.class })
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

    private static QueueResponse queue(String name) {
        return new QueueResponse(name, true, false, null, 0, 0, null, false);
    }

    @BeforeEach
    void setUp() {
        reset(service);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    // ---------------------------------------------------------------- crear

    @Test
    void creaUnaColaYResponde201ConLocation() throws Exception {
        when(service.createQueue(any(CreateQueueRequest.class))).thenReturn(queue("q.prueba"));

        mockMvc.perform(post("/api/rabbit/queues").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"q.prueba\",\"durable\":true,\"messageTtlMs\":60000}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/rabbit/queues/q.prueba"))
                .andExpect(jsonPath("$.name").value("q.prueba"));

        verify(service).createQueue(new CreateQueueRequest("q.prueba", true, null, null, null, 60000));
    }

    @Test
    void crearUnaColaQueYaExisteResponde409() throws Exception {
        when(service.createQueue(any(CreateQueueRequest.class))).thenThrow(new QueueAlreadyExistsException("q.prueba"));

        mockMvc.perform(post("/api/rabbit/queues").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"q.prueba\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("La cola 'q.prueba' ya existe"));
    }

    @Test
    void unaTopologiaInvalidaResponde400() throws Exception {
        when(service.createQueue(any(CreateQueueRequest.class)))
                .thenThrow(new InvalidTopologyException("El nombre de la cola no puede estar vacío"));

        mockMvc.perform(post("/api/rabbit/queues").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El nombre de la cola no puede estar vacío"));
    }

    @Test
    void unCuerpoQueNoEsJsonResponde400() throws Exception {
        mockMvc.perform(post("/api/rabbit/queues").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
        verify(service, never()).createQueue(any());
    }

    @Test
    void unaFallaDelBrokerResponde502() throws Exception {
        when(service.createQueue(any(CreateQueueRequest.class))).thenThrow(new BrokerOperationException(
                "RabbitMQ no pudo crear la cola 'q.prueba'", new AmqpConnectException(new IOException("caído"))));

        mockMvc.perform(post("/api/rabbit/queues").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"q.prueba\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }

    // ---------------------------------------------------------------- leer

    @Test
    void listaLasColas() throws Exception {
        when(service.listQueues()).thenReturn(List.of(queue("q.a"), queue("q.b")));

        mockMvc.perform(get("/api/rabbit/queues").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("q.a"));
    }

    @Test
    void entregaElDetalleDeUnaColaConPuntosEnElNombre() throws Exception {
        when(service.getQueue("q.cmd.email")).thenReturn(
                new QueueResponse("q.cmd.email", true, false, null, 5, 1, "running", true));

        mockMvc.perform(get("/api/rabbit/queues/q.cmd.email").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("q.cmd.email"))
                .andExpect(jsonPath("$.messages").value(5))
                .andExpect(jsonPath("$.consumers").value(1))
                .andExpect(jsonPath("$.protectedResource").value(true));
    }

    @Test
    void unaColaInexistenteResponde404() throws Exception {
        when(service.getQueue("q.nada")).thenThrow(new QueueNotFoundException("q.nada"));

        mockMvc.perform(get("/api/rabbit/queues/q.nada").with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("La cola 'q.nada' no existe"));
    }

    // ------------------------------------------------------------- eliminar

    @Test
    void eliminaUnaColaYResponde204() throws Exception {
        mockMvc.perform(delete("/api/rabbit/queues/q.prueba").with(admin()))
                .andExpect(status().isNoContent());

        verify(service).deleteQueue("q.prueba");
    }

    @Test
    void eliminarUnaColaInexistenteResponde404() throws Exception {
        doThrow(new QueueNotFoundException("q.nada")).when(service).deleteQueue("q.nada");

        mockMvc.perform(delete("/api/rabbit/queues/q.nada").with(admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarUnaColaDelSistemaResponde409() throws Exception {
        doThrow(new ProtectedResourceException("cola", "q.cmd.email")).when(service).deleteQueue("q.cmd.email");

        mockMvc.perform(delete("/api/rabbit/queues/q.cmd.email").with(admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "'q.cmd.email' (cola) pertenece a la topología del sistema y no puede eliminarse"));
    }

    @Test
    void vaciaUnaColaYResponde204() throws Exception {
        mockMvc.perform(delete("/api/rabbit/queues/q.cmd.email.dlq/messages").with(admin()))
                .andExpect(status().isNoContent());

        verify(service).purgeQueue("q.cmd.email.dlq");
    }

    // ------------------------------------------------------------ seguridad

    @Test
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/rabbit/queues")).andExpect(status().isUnauthorized());
    }

    @Test
    void unOperadorNoPuedeAdministrarColas() throws Exception {
        mockMvc.perform(delete("/api/rabbit/queues/q.prueba")
                        .with(jwt().authorities(new SimpleGrantedAuthority("Operador"))))
                .andExpect(status().isForbidden());

        verify(service, never()).deleteQueue(any());
    }
}
