package cl.barriodigital.rabbitadmin.dto;

/**
 * Reglas de validación compartidas por los DTO de entrada. Las constantes se usan en anotaciones
 * de Bean Validation, por eso son literales y los mensajes no contienen llaves ni signos de dólar
 * (el interpolador los trataría como expresiones).
 */
public final class ValidationRules {

    /** Largo máximo de nombres y routing keys que admite RabbitMQ (255 bytes). */
    public static final int MAX_LENGTH = 255;

    /** Nombre propio: caracteres seguros, 1 a 255, y sin el prefijo {@code amq.} reservado del broker. */
    public static final String RESOURCE_NAME_REGEX = "^(?!amq\\.)[A-Za-z0-9._-]{1,255}$";

    public static final String RESOURCE_NAME_MESSAGE =
            "El nombre debe tener entre 1 y 255 caracteres (letras, números, '.', '_' o '-'), "
                    + "sin espacios y sin empezar con 'amq.'";

    /** Nombre de un recurso que ya existe (por ejemplo, el exchange de dead-letter): sin la regla del prefijo. */
    public static final String EXISTING_NAME_REGEX = "^[A-Za-z0-9._-]{1,255}$";

    public static final String EXISTING_NAME_MESSAGE =
            "El nombre debe tener entre 1 y 255 caracteres (letras, números, '.', '_' o '-') y sin espacios";

    public static final String NAME_REQUIRED_MESSAGE = "El nombre es obligatorio";

    public static final String ROUTING_KEY_MESSAGE = "La routing key no puede superar los 255 caracteres";

    private ValidationRules() {
    }
}
