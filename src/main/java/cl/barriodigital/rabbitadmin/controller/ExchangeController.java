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

import cl.barriodigital.rabbitadmin.dto.CreateExchangeRequest;
import cl.barriodigital.rabbitadmin.dto.ExchangeResponse;
import cl.barriodigital.rabbitadmin.service.RabbitAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * QUÉ ES: controlador REST de exchanges (/api/rabbit/exchanges). Un exchange recibe mensajes y los reparte a colas.
 * Administración de exchanges. El acceso (solo Admin) lo exige la configuración de seguridad.
 */
@Tag(name = "Exchanges", description = "Crear, listar y eliminar exchanges")
@RestController
@RequestMapping("/api/rabbit/exchanges")
@RequiredArgsConstructor
public class ExchangeController {

    // Servicio con la lógica de administración.
    private final RabbitAdminService service;

    @Operation(summary = "Crear un exchange (direct, topic, fanout o headers)", responses = {
            @ApiResponse(responseCode = "201", description = "Exchange creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o tipo inexistente"),
            @ApiResponse(responseCode = "409", description = "El exchange ya existe") })
    @PostMapping
    public ResponseEntity<ExchangeResponse> create(@Valid @RequestBody CreateExchangeRequest request) {
        ExchangeResponse created = service.createExchange(request);
        // Cabecera Location con la URL del recurso creado (respuesta 201).
        URI location = UriComponentsBuilder.fromPath("/api/rabbit/exchanges/{name}")
                .buildAndExpand(created.name()).encode().toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Listar los exchanges", responses = {
            @ApiResponse(responseCode = "200", description = "Lista de exchanges") })
    // GET: lista todos los exchanges.
    @GetMapping
    public List<ExchangeResponse> list() {
        return service.listExchanges();
    }

    @Operation(summary = "Eliminar un exchange", responses = {
            @ApiResponse(responseCode = "204", description = "Exchange eliminado"),
            @ApiResponse(responseCode = "400", description = "Exchange del broker (amq.* o por defecto)"),
            @ApiResponse(responseCode = "404", description = "El exchange no existe"),
            @ApiResponse(responseCode = "409", description = "Exchange de la topología del sistema (cmd.*)") })
    // DELETE: elimina el exchange por nombre (404 si no existe, 409 si es del sistema).
    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String name) {
        service.deleteExchange(name);
    }
}
