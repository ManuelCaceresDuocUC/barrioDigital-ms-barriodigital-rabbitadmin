package cl.barriodigital.rabbitadmin.exception;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import cl.barriodigital.rabbitadmin.dto.ApiError;
import cl.barriodigital.rabbitadmin.dto.ExchangeType;
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
        // TreeMap: el orden de los campos es estable y el mensaje no cambia entre ejecuciones.
        Map<String, String> fieldErrors = new TreeMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        String message = "Datos inválidos: " + fieldErrors.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        if (mentions(ex, ExchangeType.class.getSimpleName())) {
            String allowed = Arrays.stream(ExchangeType.values()).map(Enum::name).collect(Collectors.joining(", "));
            return build(HttpStatus.BAD_REQUEST, "Tipo de exchange no válido. Valores permitidos: " + allowed, null);
        }
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es un JSON válido", null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST, "Falta el parámetro obligatorio '" + ex.getParameterName() + "'", null);
    }

    private static boolean mentions(Throwable error, String text) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current.getMessage() != null && current.getMessage().contains(text)) {
                return true;
            }
        }
        return false;
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, Map<String, String> fieldErrors) {
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
