package cl.barriodigital.rabbitadmin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.barriodigital.rabbitadmin.dto.PublishMessageRequest;
import cl.barriodigital.rabbitadmin.dto.PublishedMessageResponse;
import cl.barriodigital.rabbitadmin.service.MessagePublisherService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Publicación de mensajes para los demás microservicios (uso interno, no del frontend). No exige
 * token: solo se alcanza por la red de Docker, porque el BFF únicamente reenvía las rutas
 * {@code /api/**}. Responde 202 cuando el mensaje se entregó al broker, 400 si los datos o la
 * routing key no son válidos y 502 si RabbitMQ no responde.
 */
@Hidden
@RestController
@RequestMapping("/internal/messages")
@RequiredArgsConstructor
public class MessagePublisherController {

    private final MessagePublisherService service;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PublishedMessageResponse publish(@Valid @RequestBody PublishMessageRequest request) {
        return service.publish(request);
    }
}
