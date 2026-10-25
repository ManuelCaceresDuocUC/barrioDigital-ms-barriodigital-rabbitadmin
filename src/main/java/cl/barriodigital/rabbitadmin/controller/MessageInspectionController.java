package cl.barriodigital.rabbitadmin.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.barriodigital.rabbitadmin.dto.DlqMessageResponse;
import cl.barriodigital.rabbitadmin.service.MessageInspectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** Lectura de los mensajes fallidos. El acceso (solo Admin) lo exige la configuración de seguridad. */
@Tag(name = "Mensajes", description = "Leer los mensajes de las colas de mensajes fallidos (DLQ)")
@RestController
@RequestMapping("/api/rabbit/queues")
@RequiredArgsConstructor
public class MessageInspectionController {

    private final MessageInspectionService service;

    @Operation(summary = "Leer los mensajes de una DLQ sin consumirlos",
            description = "Los mensajes se devuelven a la cola, pero RabbitMQ los marca como reentregados. "
                    + "Solo se permite en colas *.dlq. El correo y el RUT se entregan enmascarados.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Mensajes de la cola (lista vacía si no hay)"),
                    @ApiResponse(responseCode = "400", description = "El parámetro count no es válido"),
                    @ApiResponse(responseCode = "403", description = "La cola no es una DLQ"),
                    @ApiResponse(responseCode = "404", description = "La cola no existe") })
    @GetMapping("/{name}/messages")
    public List<DlqMessageResponse> peek(
            @PathVariable String name,
            @Parameter(description = "Cantidad de mensajes a leer (por defecto 20, máximo 50)")
            @RequestParam(required = false) Integer count) {
        return service.peek(name, count);
    }
}
