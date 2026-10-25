package cl.barriodigital.rabbitadmin.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SensitiveDataMaskerTest {

    @Test
    void ocultaElCorreoDejandoLaPrimeraLetraYElDominio() {
        assertThat(SensitiveDataMasker.mask("{\"to\":\"maria.perez@gmail.com\"}"))
                .isEqualTo("{\"to\":\"m***@gmail.com\"}");
    }

    @Test
    void reemplazaElRutConPuntosOSin() {
        assertThat(SensitiveDataMasker.mask("rut 12.345.678-9 y 9876543-K")).isEqualTo("rut [RUT] y [RUT]");
    }

    @Test
    void noTocaElRestoDelTexto() {
        assertThat(SensitiveDataMasker.mask("{\"ticketId\":\"T-100\",\"priority\":\"ALTA\"}"))
                .isEqualTo("{\"ticketId\":\"T-100\",\"priority\":\"ALTA\"}");
    }

    @Test
    void aceptaNulo() {
        assertThat(SensitiveDataMasker.mask(null)).isNull();
    }
}
