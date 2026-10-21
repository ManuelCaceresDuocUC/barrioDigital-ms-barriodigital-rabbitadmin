package cl.barriodigital.rabbitadmin.dto;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Cuerpo de error con los mismos nombres de campo que el de requests y catalog. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors) {
}
