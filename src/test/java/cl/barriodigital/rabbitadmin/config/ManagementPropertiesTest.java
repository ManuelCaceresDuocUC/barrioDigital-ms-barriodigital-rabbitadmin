package cl.barriodigital.rabbitadmin.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class ManagementPropertiesTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Test
    void aceptaValoresValidosYQuitaLaBarraFinal() {
        var properties = new ManagementProperties("http://rabbit:15672/", "barrio", "secreto", TIMEOUT);

        assertThat(properties.url()).isEqualTo("http://rabbit:15672");
    }

    @Test
    void exigeUrlUsuarioYContrasena() {
        assertThatThrownBy(() -> new ManagementProperties(" ", "barrio", "secreto", TIMEOUT))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("url");
        assertThatThrownBy(() -> new ManagementProperties("http://rabbit:15672", "", "secreto", TIMEOUT))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("username");
        assertThatThrownBy(() -> new ManagementProperties("http://rabbit:15672", "barrio", null, TIMEOUT))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("password");
    }

    @Test
    void exigeUnTimeoutPositivo() {
        assertThatThrownBy(() -> new ManagementProperties("http://rabbit:15672", "barrio", "secreto", Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("timeout");
    }

    @Test
    void elToStringNoRevelaLaContrasena() {
        var properties = new ManagementProperties("http://rabbit:15672", "barrio", "secreto", TIMEOUT);

        assertThat(properties.toString()).doesNotContain("secreto").contains("barrio");
    }
}
