package cl.barriodigital.rabbitadmin.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import cl.barriodigital.rabbitadmin.dto.CreateExchangeRequest;
import cl.barriodigital.rabbitadmin.dto.ExchangeResponse;
import cl.barriodigital.rabbitadmin.service.RabbitAdminService;
import lombok.RequiredArgsConstructor;

/** Administración de exchanges. El acceso (solo Admin) lo exige la configuración de seguridad. */
@RestController
@RequestMapping("/api/rabbit/exchanges")
@RequiredArgsConstructor
public class ExchangeController {

    private final RabbitAdminService service;

    @PostMapping
    public ResponseEntity<ExchangeResponse> create(@RequestBody CreateExchangeRequest request) {
        ExchangeResponse created = service.createExchange(request);
        URI location = UriComponentsBuilder.fromPath("/api/rabbit/exchanges/{name}")
                .buildAndExpand(created.name()).encode().toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<ExchangeResponse> list() {
        return service.listExchanges();
    }

    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String name) {
        service.deleteExchange(name);
    }
}
