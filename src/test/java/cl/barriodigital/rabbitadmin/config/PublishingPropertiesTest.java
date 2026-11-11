package cl.barriodigital.rabbitadmin.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class PublishingPropertiesTest {

    @Test
    void conservaElExchangeYLasClavesPermitidas() {
        PublishingProperties properties = new PublishingProperties("cmd.direct", List.of("email.send", "crew.ticket"));

        assertThat(properties.exchange()).isEqualTo("cmd.direct");
        assertThat(properties.routingKeys()).containsExactly("email.send", "crew.ticket");
    }

    @Test
    void soloPermiteLasClavesConfiguradas() {
        PublishingProperties properties = new PublishingProperties("cmd.direct", List.of("email.send"));

        assertThat(properties.allows("email.send")).isTrue();
        assertThat(properties.allows("otra.clave")).isFalse();
        assertThat(properties.allows(null)).isFalse();
    }

    @Test
    void rechazaUnExchangeVacio() {
        assertThatThrownBy(() -> new PublishingProperties(" ", List.of("email.send")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaNoTenerNingunaClave() {
        assertThatThrownBy(() -> new PublishingProperties("cmd.direct", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PublishingProperties("cmd.direct", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
