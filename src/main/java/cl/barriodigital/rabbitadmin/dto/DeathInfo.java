package cl.barriodigital.rabbitadmin.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Por qué y desde dónde llegó un mensaje a la DLQ (primer registro de la cabecera {@code x-death}). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeathInfo(
        String queue,
        String exchange,
        List<String> routingKeys,
        String reason,
        long count,
        Instant time) {
}
