package cl.barriodigital.rabbitadmin.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import cl.barriodigital.rabbitadmin.config.ProtectionProperties;

class ProtectedResourcesTest {

    private final ProtectedResources protectedResources = new ProtectedResources(
            new ProtectionProperties(List.of("q.cmd.*"), List.of("cmd.*", "amq.*")));

    @Test
    void protegeLasColasDelSistema() {
        assertThat(protectedResources.isProtectedQueue("q.cmd.email")).isTrue();
        assertThat(protectedResources.isProtectedQueue("q.cmd.email.dlq")).isTrue();
        assertThat(protectedResources.isProtectedQueue("q.prueba")).isFalse();
    }

    @Test
    void protegeLosExchangesDelSistemaYElPorDefecto() {
        assertThat(protectedResources.isProtectedExchange("cmd.direct")).isTrue();
        assertThat(protectedResources.isProtectedExchange("cmd.dead.dlx")).isTrue();
        assertThat(protectedResources.isProtectedExchange("amq.topic")).isTrue();
        assertThat(protectedResources.isProtectedExchange("")).isTrue();
        assertThat(protectedResources.isProtectedExchange("ex.prueba")).isFalse();
    }

    @Test
    void sinPatronesNoProtegeNada() {
        var open = new ProtectedResources(new ProtectionProperties(null, null));

        assertThat(open.isProtectedQueue("q.cmd.email")).isFalse();
        assertThat(open.isProtectedExchange("cmd.direct")).isFalse();
    }
}
