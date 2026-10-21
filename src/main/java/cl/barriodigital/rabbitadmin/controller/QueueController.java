package cl.barriodigital.rabbitadmin.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** Administración de colas. El acceso (solo Admin) lo exige la configuración de seguridad. */
@Tag(name = "Colas", description = "Crear, consultar, eliminar y vaciar colas")
@RestController
@RequestMapping("/api/rabbit/queues")
@RequiredArgsConstructor
public class QueueController {

    private final RabbitAdminService service;

    @Operation(summary = "Crear una cola", responses = {
            @ApiResponse(responseCode = "201", description = "Cola creada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "409", description = "La cola ya existe") })
    @PostMapping
    public ResponseEntity<QueueResponse> create(@Valid @RequestBody CreateQueueRequest request) {
        QueueResponse created = service.createQueue(request);
        // Ruta relativa: detrás del gateway no se filtra el host interno del servicio.
        URI location = UriComponentsBuilder.fromPath("/api/rabbit/queues/{name}")
                .buildAndExpand(created.name()).encode().toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Listar las colas", responses = {
            @ApiResponse(responseCode = "200", description = "Lista de colas con mensajes y consumidores") })
    @GetMapping
    public List<QueueResponse> list() {
        return service.listQueues();
    }

    @Operation(summary = "Consultar una cola", responses = {
            @ApiResponse(responseCode = "200", description = "Detalle de la cola con conteos en tiempo real"),
            @ApiResponse(responseCode = "404", description = "La cola no existe") })
    @GetMapping("/{name}")
    public QueueResponse get(@PathVariable String name) {
        return service.getQueue(name);
    }

    @Operation(summary = "Eliminar una cola", responses = {
            @ApiResponse(responseCode = "204", description = "Cola eliminada"),
            @ApiResponse(responseCode = "404", description = "La cola no existe"),
            @ApiResponse(responseCode = "409", description = "Cola de la topología del sistema (q.cmd.*)") })
    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String name) {
        service.deleteQueue(name);
    }

    @Operation(summary = "Vaciar una cola (también permitido en las DLQ)", responses = {
            @ApiResponse(responseCode = "204", description = "Mensajes eliminados"),
            @ApiResponse(responseCode = "404", description = "La cola no existe") })
    @DeleteMapping("/{name}/messages")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void purge(@PathVariable String name) {
        service.purgeQueue(name);
    }
}
