package cl.barriodigital.rabbitadmin.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * QUÉ ES: manejador de errores de seguridad. Implementa AuthenticationEntryPoint (401: sin token o token inválido)
 * y AccessDeniedHandler (403: token válido pero sin rol Admin).
 */
@Component
public class RestSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    static final String UNAUTHORIZED_MESSAGE = "Falta un token válido en el encabezado Authorization";
    static final String FORBIDDEN_MESSAGE = "Se requiere el rol Admin para administrar RabbitMQ";

    // 401: no autenticado. WWW-Authenticate indica al cliente que debe enviar un Bearer token.
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        write(response, HttpStatus.UNAUTHORIZED, UNAUTHORIZED_MESSAGE);
    }

    // 403: autenticado pero sin permiso.
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, HttpStatus.FORBIDDEN, FORBIDDEN_MESSAGE);
    }

    // Escribe la respuesta JSON a mano (aquí no hay controlador que lo haga por nosotros).
    private void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}"
                .formatted(Instant.now(), status.value(), status.getReasonPhrase(), message));
    }
}
