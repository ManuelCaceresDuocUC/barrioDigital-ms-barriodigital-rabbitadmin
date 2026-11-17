package cl.barriodigital.rabbitadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * QUÉ ES: punto de entrada del microservicio rabbitadmin (administrador de RabbitMQ).
 * QUÉ HACE: arranca Spring Boot. Este servicio crea/consulta/elimina colas, exchanges y bindings, lee mensajes
 * de colas permitidas (p. ej. las DLQ) y publica mensajes en nombre de los demás microservicios.
 */
// @SpringBootApplication = configuración automática + escaneo de componentes del paquete.
@SpringBootApplication
public class RabbitAdminApplication {

    // Método que ejecuta la JVM al iniciar.
    public static void main(String[] args) {
        SpringApplication.run(RabbitAdminApplication.class, args);
    }
}
