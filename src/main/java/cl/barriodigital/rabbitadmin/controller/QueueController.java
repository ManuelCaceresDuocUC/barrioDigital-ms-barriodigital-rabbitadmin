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

import cl.barriodigital.rabbitadmin.dto.CreateQueueRequest;
import cl.barriodigital.rabbitadmin.dto.QueueResponse;
import cl.barriodigital.rabbitadmin.service.RabbitAdminService;
import lombok.RequiredArgsConstructor;

/** Administración de colas. El acceso (solo Admin) lo exige la configuración de seguridad. */
@RestController
@RequestMapping("/api/rabbit/queues")
@RequiredArgsConstructor
public class QueueController {

    private final RabbitAdminService service;

    @PostMapping
    public ResponseEntity<QueueResponse> create(@RequestBody CreateQueueRequest request) {
        QueueResponse created = service.createQueue(request);
        // Ruta relativa: detrás del gateway no se filtra el host interno del servicio.
        URI location = UriComponentsBuilder.fromPath("/api/rabbit/queues/{name}")
                .buildAndExpand(created.name()).encode().toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<QueueResponse> list() {
        return service.listQueues();
    }

    @GetMapping("/{name}")
    public QueueResponse get(@PathVariable String name) {
        return service.getQueue(name);
    }

    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String name) {
        service.deleteQueue(name);
    }

    @DeleteMapping("/{name}/messages")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void purge(@PathVariable String name) {
        service.purgeQueue(name);
    }
}
