package cl.barriodigital.rabbitadmin.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class InspectionPropertiesTest {

    @Test
    void sinValoresUsaLosPorDefectoYNoPermiteNingunaCola() {
        InspectionProperties properties = new InspectionProperties(null, null, null, null);

        assertThat(properties.queues()).isEmpty();
        assertThat(properties.defaultMessages()).isEqualTo(20);
        assertThat(properties.maxMessages()).isEqualTo(50);
        assertThat(properties.maxPayloadBytes()).isEqualTo(20_000);
    }

    @Test
    void conservaLosValoresIndicados() {
        InspectionProperties properties = new InspectionProperties(List.of("*.dlq"), 10, 30, 5_000);

        assertThat(properties.queues()).containsExactly("*.dlq");
        assertThat(properties.defaultMessages()).isEqualTo(10);
        assertThat(properties.maxMessages()).isEqualTo(30);
        assertThat(properties.maxPayloadBytes()).isEqualTo(5_000);
    }

    @Test
    void rechazaUnMaximoMenorQueElValorPorDefecto() {
        assertThatThrownBy(() -> new InspectionProperties(List.of("*.dlq"), 20, 10, 1_000))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaCantidadesNoPositivas() {
        assertThatThrownBy(() -> new InspectionProperties(List.of("*.dlq"), 0, 50, 1_000))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new InspectionProperties(List.of("*.dlq"), 20, 50, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
