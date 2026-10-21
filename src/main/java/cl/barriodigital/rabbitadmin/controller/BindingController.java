package cl.barriodigital.rabbitadmin.controller;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.barriodigital.rabbitadmin.dto.BindingRequest;
import cl.barriodigital.rabbitadmin.dto.BindingResponse;
import cl.barriodigital.rabbitadmin.service.RabbitAdminService;
import lombok.RequiredArgsConstructor;

/**
 * Administración de bindings. Un binding no tiene identificador propio: queda definido por
 * exchange + cola + routing key, por eso el borrado los recibe como parámetros de la URL
 * (una routing key con {@code #} debe enviarse como {@code %23}).
 */
@RestController
@RequestMapping("/api/rabbit/bindings")
@RequiredArgsConstructor
public class BindingController {

    private final RabbitAdminService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BindingResponse create(@Valid @RequestBody BindingRequest request) {
        return service.createBinding(request);
    }

    @GetMapping
    public List<BindingResponse> list(@RequestParam(required = false) String queue) {
        return service.listBindings(queue);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestParam String exchange, @RequestParam String queue,
            @RequestParam(defaultValue = "") String routingKey) {
        service.deleteBinding(new BindingRequest(exchange, queue, routingKey));
    }
}
