package cl.barriodigital.rabbitadmin.dto;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * QUÉ ES: JSON uniforme de errores (lo arma GlobalExceptionHandler). Los campos null no se envían;
 * fieldErrors solo aparece en errores de validación (campo -> mensaje).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors) {
}
