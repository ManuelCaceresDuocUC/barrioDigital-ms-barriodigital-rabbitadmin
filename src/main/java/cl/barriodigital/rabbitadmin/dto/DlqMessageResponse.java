package cl.barriodigital.rabbitadmin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Mensaje de una cola de mensajes fallidos. El cuerpo llega con el correo y el RUT ya enmascarados;
 * si el contenido es binario no se entrega.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DlqMessageResponse(
        String exchange,
        String routingKey,
        boolean redelivered,
        String payload,
        boolean binary,
        int payloadBytes,
        boolean payloadTruncated,
        DeathInfo death) {
}
