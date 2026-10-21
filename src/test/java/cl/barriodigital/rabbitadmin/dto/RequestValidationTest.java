package cl.barriodigital.rabbitadmin.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/** Reglas de Bean Validation de los DTO de entrada, sin levantar Spring. */
class RequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    private static Set<String> invalidFields(Object dto) {
        return validator.validate(dto).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private static CreateQueueRequest queue(String name) {
        return new CreateQueueRequest(name, null, null, null, null, null);
    }

    // ---------------------------------------------------------------- nombres

    @ParameterizedTest
    @ValueSource(strings = { "q.prueba", "q.cmd.email.dlq", "COLA_1", "a", "mi-cola.v2", "amqp.cola" })
    void aceptaNombresValidos(String name) {
        assertThat(invalidFields(queue(name))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ", "   ", "con espacios", " q.prueba", "q.prueba ", "ñandú", "cola/uno",
            "cola#1", "amq.cola", "amq." })
    void rechazaNombresInvalidos(String name) {
        assertThat(invalidFields(queue(name))).containsExactly("name");
    }

    @Test
    void elLargoDelNombreTieneUnMaximoDe255() {
        assertThat(invalidFields(queue("x".repeat(255)))).isEmpty();
        assertThat(invalidFields(queue("x".repeat(256)))).containsExactly("name");
    }

    @Test
    void rechazaUnNombreAusente() {
        assertThat(invalidFields(queue(null))).containsExactly("name");
    }

    // ------------------------------------------------------------------- TTL

    @Test
    void aceptaUnTtlPositivoOAusente() {
        assertThat(invalidFields(new CreateQueueRequest("q.a", null, null, null, null, 60000))).isEmpty();
        assertThat(invalidFields(new CreateQueueRequest("q.a", null, null, null, null, null))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = { -1, -60000, 0 })
    void rechazaUnTtlNoPositivo(int ttl) {
        assertThat(invalidFields(new CreateQueueRequest("q.a", null, null, null, null, ttl)))
                .containsExactly("messageTtlMs");
    }

    // ------------------------------------------------------------ dead-letter

    @Test
    void validaElExchangeYLaRoutingKeyDeDeadLetter() {
        assertThat(invalidFields(new CreateQueueRequest("q.a", null, null, "cmd.dead.dlx", "email.dlq", null)))
                .isEmpty();
        assertThat(invalidFields(new CreateQueueRequest("q.a", null, null, "con espacios", null, null)))
                .containsExactly("deadLetterExchange");
        assertThat(invalidFields(new CreateQueueRequest("q.a", null, null, null, "k".repeat(256), null)))
                .containsExactly("deadLetterRoutingKey");
    }

    // ------------------------------------------------------------- exchanges

    @Test
    void validaElNombreYElTipoDelExchange() {
        assertThat(invalidFields(new CreateExchangeRequest("ex.a", ExchangeType.TOPIC, null, null))).isEmpty();
        assertThat(invalidFields(new CreateExchangeRequest("ex a", ExchangeType.TOPIC, null, null)))
                .containsExactly("name");
        assertThat(invalidFields(new CreateExchangeRequest("amq.topic", ExchangeType.TOPIC, null, null)))
                .containsExactly("name");
        assertThat(invalidFields(new CreateExchangeRequest("ex.a", null, null, null))).containsExactly("type");
        assertThat(invalidFields(new CreateExchangeRequest(null, null, null, null)))
                .containsExactlyInAnyOrder("name", "type");
    }

    // -------------------------------------------------------------- bindings

    @Test
    void validaExchangeColaYRoutingKeyDelBinding() {
        assertThat(invalidFields(new BindingRequest("ex.a", "q.a", "crew.#"))).isEmpty();
        assertThat(invalidFields(new BindingRequest("ex.a", "q.a", null))).isEmpty();
        assertThat(invalidFields(new BindingRequest("ex.a", "q.a", ""))).isEmpty();
        assertThat(invalidFields(new BindingRequest("", "q.a", "k"))).containsExactly("exchange");
        assertThat(invalidFields(new BindingRequest("ex.a", "q a", "k"))).containsExactly("queue");
        assertThat(invalidFields(new BindingRequest("ex.a", "q.a", "k".repeat(256)))).containsExactly("routingKey");
        assertThat(invalidFields(new BindingRequest(null, null, "k"))).containsExactlyInAnyOrder("exchange", "queue");
    }
}
