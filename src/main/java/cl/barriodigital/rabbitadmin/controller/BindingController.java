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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * QUÉ ES: controlador REST de bindings (/api/rabbit/bindings). Solo recibe la petición y delega en RabbitAdminService.
 * Un binding es la regla que conecta un exchange con una cola según la routing key.
 * Administración de bindings. Un binding no tiene identificador propio: queda definido por
 * exchange + cola + routing key, por eso el borrado los recibe como parámetros de la URL
 * (una routing key con {@code #} debe enviarse como {@code %23}).
 */
@Tag(name = "Bindings", description = "Crear, listar y eliminar bindings entre exchanges y colas")
@RestController
@RequestMapping("/api/rabbit/bindings")
@RequiredArgsConstructor
public class BindingController {

    // Servicio con la lógica de administración.
    private final RabbitAdminService service;

    @Operation(summary = "Crear un binding exchange a cola", responses = {
            @ApiResponse(responseCode = "201", description = "Binding creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "La cola o el exchange no existen") })
    // POST: crea el binding. @Valid ejecuta las validaciones del DTO.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BindingResponse create(@Valid @RequestBody BindingRequest request) {
        return service.createBinding(request);
    }

    @Operation(summary = "Listar bindings, opcionalmente de una cola", responses = {
            @ApiResponse(responseCode = "200", description = "Lista de bindings") })
    // GET: lista bindings; con ?queue=nombre filtra por cola.
    @GetMapping
    public List<BindingResponse> list(@RequestParam(required = false) String queue) {
        return service.listBindings(queue);
    }

    @Operation(summary = "Eliminar un binding (exchange, queue y routingKey por parámetros; # como %23)", responses = {
            @ApiResponse(responseCode = "204", description = "Binding eliminado"),
            @ApiResponse(responseCode = "400", description = "Falta un parámetro obligatorio"),
            @ApiResponse(responseCode = "404", description = "El binding no existe"),
            @ApiResponse(responseCode = "409", description = "Binding de la topología del sistema") })
    // DELETE: elimina el binding identificado por exchange + cola + routing key.
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestParam String exchange, @RequestParam String queue,
            @RequestParam(defaultValue = "") String routingKey) {
        service.deleteBinding(new BindingRequest(exchange, queue, routingKey));
    }
}
