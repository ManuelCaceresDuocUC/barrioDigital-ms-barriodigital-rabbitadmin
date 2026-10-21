package cl.barriodigital.rabbitadmin.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import cl.barriodigital.rabbitadmin.dto.ApiError;
import lombok.extern.slf4j.Slf4j;

/** Traduce las excepciones de dominio y de entrada a respuestas JSON con un {@code message} legible. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ QueueNotFoundException.class, ExchangeNotFoundException.class,
            BindingNotFoundException.class })
    public ResponseEntity<ApiError> handleNotFound(RabbitAdminException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler({ QueueAlreadyExistsException.class, ExchangeAlreadyExistsException.class,
            ProtectedResourceException.class })
    public ResponseEntity<ApiError> handleConflict(RabbitAdminException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(InvalidTopologyException.class)
    public ResponseEntity<ApiError> handleInvalidTopology(InvalidTopologyException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    /** RabbitMQ o su API de gestión no respondieron: es un fallo aguas arriba, no del cliente. */
    @ExceptionHandler({ BrokerOperationException.class, ManagementApiException.class })
    public ResponseEntity<ApiError> handleUpstream(RabbitAdminException ex) {
        log.error("Falla al hablar con RabbitMQ: {}", ex.getMessage(), ex);
        return build(HttpStatus.BAD_GATEWAY, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, String.join("; ", fieldErrors.values()), fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es un JSON válido", null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, Map<String, String> fieldErrors) {
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
