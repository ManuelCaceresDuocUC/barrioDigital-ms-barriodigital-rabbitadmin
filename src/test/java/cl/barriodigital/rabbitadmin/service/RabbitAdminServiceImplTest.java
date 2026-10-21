package cl.barriodigital.rabbitadmin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;

import cl.barriodigital.rabbitadmin.client.ManagementBinding;
import cl.barriodigital.rabbitadmin.client.ManagementExchange;
import cl.barriodigital.rabbitadmin.client.ManagementQueue;
import cl.barriodigital.rabbitadmin.client.RabbitManagementClient;
import cl.barriodigital.rabbitadmin.config.ProtectionProperties;
import cl.barriodigital.rabbitadmin.dto.BindingRequest;
import cl.barriodigital.rabbitadmin.dto.CreateExchangeRequest;
import cl.barriodigital.rabbitadmin.dto.CreateQueueRequest;
import cl.barriodigital.rabbitadmin.dto.ExchangeType;
import cl.barriodigital.rabbitadmin.exception.BindingNotFoundException;
import cl.barriodigital.rabbitadmin.exception.BrokerOperationException;
import cl.barriodigital.rabbitadmin.exception.ExchangeAlreadyExistsException;
import cl.barriodigital.rabbitadmin.exception.ExchangeNotFoundException;
import cl.barriodigital.rabbitadmin.exception.InvalidTopologyException;
import cl.barriodigital.rabbitadmin.exception.ProtectedResourceException;
import cl.barriodigital.rabbitadmin.exception.QueueAlreadyExistsException;
import cl.barriodigital.rabbitadmin.exception.QueueNotFoundException;

class RabbitAdminServiceImplTest {

    private final AmqpAdmin amqpAdmin = mock(AmqpAdmin.class);
    private final RabbitManagementClient management = mock(RabbitManagementClient.class);
    private final ProtectedResources protectedResources = new ProtectedResources(
            new ProtectionProperties(List.of("q.cmd.*"), List.of("cmd.*", "amq.*")));
    private final RabbitAdminServiceImpl service =
            new RabbitAdminServiceImpl(amqpAdmin, management, protectedResources);

    private void queueExists(String name) {
        when(amqpAdmin.getQueueInfo(name)).thenReturn(new QueueInformation(name, 0, 0));
    }

    // ---------------------------------------------------------------- colas

    @Test
    void creaUnaColaConSusArgumentos() {
        var response = service.createQueue(
                new CreateQueueRequest("q.prueba", true, false, "cmd.dead.dlx", "prueba.dlq", 60000));

        ArgumentCaptor<Queue> captor = ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin).declareQueue(captor.capture());
        Queue queue = captor.getValue();
        assertThat(queue.getName()).isEqualTo("q.prueba");
        assertThat(queue.isDurable()).isTrue();
        assertThat(queue.isAutoDelete()).isFalse();
        assertThat(queue.getArguments())
                .containsEntry("x-dead-letter-exchange", "cmd.dead.dlx")
                .containsEntry("x-dead-letter-routing-key", "prueba.dlq")
                .containsEntry("x-message-ttl", 60000);
        assertThat(response.name()).isEqualTo("q.prueba");
        assertThat(response.protectedResource()).isFalse();
    }

    @Test
    void laColaSeCreaDuradoraPorDefecto() {
        service.createQueue(new CreateQueueRequest("q.prueba", null, null, null, null, null));

        ArgumentCaptor<Queue> captor = ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin).declareQueue(captor.capture());
        assertThat(captor.getValue().isDurable()).isTrue();
        assertThat(captor.getValue().isAutoDelete()).isFalse();
    }

    @Test
    void crearUnaColaQueYaExisteEsUnConflicto() {
        queueExists("q.prueba");

        assertThatThrownBy(() -> service.createQueue(new CreateQueueRequest("q.prueba", null, null, null, null, null)))
                .isInstanceOf(QueueAlreadyExistsException.class);
        verify(amqpAdmin, never()).declareQueue(any(Queue.class));
    }

    @Test
    void rechazaNombresVaciosYReservados() {
        assertThatThrownBy(() -> service.createQueue(new CreateQueueRequest(" ", null, null, null, null, null)))
                .isInstanceOf(InvalidTopologyException.class);
        assertThatThrownBy(() -> service.createQueue(new CreateQueueRequest("amq.cola", null, null, null, null, null)))
                .isInstanceOf(InvalidTopologyException.class);
    }

    @Test
    void laRoutingKeyDeLaDlxRequiereElExchange() {
        assertThatThrownBy(() -> service.createQueue(
                new CreateQueueRequest("q.prueba", null, null, null, "prueba.dlq", null)))
                .isInstanceOf(InvalidTopologyException.class);
    }

    @Test
    void unaFallaDelBrokerSeTraduceAUnaExcepcionDeDominio() {
        when(amqpAdmin.getQueueInfo("q.prueba")).thenThrow(new AmqpConnectException(new IOException("caído")));

        assertThatThrownBy(() -> service.createQueue(new CreateQueueRequest("q.prueba", null, null, null, null, null)))
                .isInstanceOf(BrokerOperationException.class);
    }

    @Test
    void eliminaUnaColaPropia() {
        queueExists("q.prueba");

        service.deleteQueue("q.prueba");

        verify(amqpAdmin).deleteQueue("q.prueba");
    }

    @Test
    void noEliminaLasColasDelSistema() {
        assertThatThrownBy(() -> service.deleteQueue("q.cmd.email"))
                .isInstanceOf(ProtectedResourceException.class);
        verify(amqpAdmin, never()).deleteQueue(anyString());
    }

    @Test
    void eliminarUnaColaInexistenteEsNoEncontrado() {
        assertThatThrownBy(() -> service.deleteQueue("q.nada")).isInstanceOf(QueueNotFoundException.class);
    }

    @Test
    void vaciarUnaColaDelSistemaEstaPermitido() {
        queueExists("q.cmd.email.dlq");

        service.purgeQueue("q.cmd.email.dlq");

        verify(amqpAdmin).purgeQueue("q.cmd.email.dlq");
    }

    @Test
    void vaciarUnaColaInexistenteEsNoEncontrado() {
        assertThatThrownBy(() -> service.purgeQueue("q.nada")).isInstanceOf(QueueNotFoundException.class);
    }

    @Test
    void elDetalleCombinaTiempoRealConElManagementApi() {
        when(amqpAdmin.getQueueInfo("q.cmd.email")).thenReturn(new QueueInformation("q.cmd.email", 5, 1));
        when(management.findQueue("q.cmd.email")).thenReturn(Optional.of(
                new ManagementQueue("q.cmd.email", true, false, null, "running", 99, 9)));

        var response = service.getQueue("q.cmd.email");

        assertThat(response.messages()).isEqualTo(5);
        assertThat(response.consumers()).isEqualTo(1);
        assertThat(response.durable()).isTrue();
        assertThat(response.state()).isEqualTo("running");
        assertThat(response.protectedResource()).isTrue();
    }

    @Test
    void elDetalleFuncionaAunqueElManagementApiNoLaConozca() {
        when(amqpAdmin.getQueueInfo("q.nueva")).thenReturn(new QueueInformation("q.nueva", 0, 0));
        when(management.findQueue("q.nueva")).thenReturn(Optional.empty());

        var response = service.getQueue("q.nueva");

        assertThat(response.name()).isEqualTo("q.nueva");
        assertThat(response.durable()).isNull();
    }

    @Test
    void listaLasColasOrdenadasPorNombre() {
        when(management.listQueues()).thenReturn(List.of(
                new ManagementQueue("q.b", true, false, null, "running", 1, 0),
                new ManagementQueue("q.a", true, false, null, null, null, null)));

        var queues = service.listQueues();

        assertThat(queues).extracting("name").containsExactly("q.a", "q.b");
        assertThat(queues.get(0).messages()).isZero();
    }

    // ------------------------------------------------------------ exchanges

    @Test
    void creaUnExchangeDelTipoPedido() {
        var response = service.createExchange(new CreateExchangeRequest("ex.prueba", ExchangeType.TOPIC, null, null));

        ArgumentCaptor<Exchange> captor = ArgumentCaptor.forClass(Exchange.class);
        verify(amqpAdmin).declareExchange(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("ex.prueba");
        assertThat(captor.getValue().getType()).isEqualTo("topic");
        assertThat(captor.getValue().isDurable()).isTrue();
        assertThat(response.type()).isEqualTo("topic");
    }

    @Test
    void elTipoDeExchangeEsObligatorio() {
        assertThatThrownBy(() -> service.createExchange(new CreateExchangeRequest("ex.prueba", null, null, null)))
                .isInstanceOf(InvalidTopologyException.class);
    }

    @Test
    void crearUnExchangeQueYaExisteEsUnConflicto() {
        when(management.exchangeExists("ex.prueba")).thenReturn(true);

        assertThatThrownBy(() -> service.createExchange(
                new CreateExchangeRequest("ex.prueba", ExchangeType.DIRECT, null, null)))
                .isInstanceOf(ExchangeAlreadyExistsException.class);
    }

    @Test
    void eliminaUnExchangePropioPeroNoLosDelSistema() {
        when(management.exchangeExists("ex.prueba")).thenReturn(true);

        service.deleteExchange("ex.prueba");

        verify(amqpAdmin).deleteExchange("ex.prueba");
        assertThatThrownBy(() -> service.deleteExchange("cmd.direct")).isInstanceOf(ProtectedResourceException.class);
        assertThatThrownBy(() -> service.deleteExchange("amq.topic")).isInstanceOf(ProtectedResourceException.class);
        assertThatThrownBy(() -> service.deleteExchange("")).isInstanceOf(ProtectedResourceException.class);
        verify(amqpAdmin, never()).deleteExchange("cmd.direct");
    }

    @Test
    void eliminarUnExchangeInexistenteEsNoEncontrado() {
        assertThatThrownBy(() -> service.deleteExchange("ex.nada")).isInstanceOf(ExchangeNotFoundException.class);
    }

    @Test
    void listaLosExchangesSinElPorDefecto() {
        when(management.listExchanges()).thenReturn(List.of(
                new ManagementExchange("", "direct", true, false),
                new ManagementExchange("cmd.direct", "direct", true, false)));

        var exchanges = service.listExchanges();

        assertThat(exchanges).singleElement().satisfies(exchange -> {
            assertThat(exchange.name()).isEqualTo("cmd.direct");
            assertThat(exchange.protectedResource()).isTrue();
        });
    }

    // ------------------------------------------------------------- bindings

    @Test
    void creaUnBindingEntreUnaColaYUnExchangeExistentes() {
        queueExists("q.prueba");
        when(management.exchangeExists("cmd.direct")).thenReturn(true);

        var response = service.createBinding(new BindingRequest("cmd.direct", "q.prueba", "prueba.key"));

        ArgumentCaptor<Binding> captor = ArgumentCaptor.forClass(Binding.class);
        verify(amqpAdmin).declareBinding(captor.capture());
        assertThat(captor.getValue().getDestination()).isEqualTo("q.prueba");
        assertThat(captor.getValue().getExchange()).isEqualTo("cmd.direct");
        assertThat(captor.getValue().getRoutingKey()).isEqualTo("prueba.key");
        assertThat(response.routingKey()).isEqualTo("prueba.key");
    }

    @Test
    void noCreaUnBindingSiFaltaLaColaOElExchange() {
        assertThatThrownBy(() -> service.createBinding(new BindingRequest("cmd.direct", "q.nada", "k")))
                .isInstanceOf(QueueNotFoundException.class);

        queueExists("q.prueba");
        assertThatThrownBy(() -> service.createBinding(new BindingRequest("ex.nada", "q.prueba", "k")))
                .isInstanceOf(ExchangeNotFoundException.class);
        verify(amqpAdmin, never()).declareBinding(any(Binding.class));
    }

    @Test
    void listaSoloLosBindingsHaciaColas() {
        when(management.listBindings()).thenReturn(List.of(
                new ManagementBinding("", "q.a", "queue", "q.a"),
                new ManagementBinding("cmd.direct", "q.cmd.email", "queue", "email.send"),
                new ManagementBinding("ex.a", "ex.b", "exchange", "k")));

        var bindings = service.listBindings(null);

        assertThat(bindings).singleElement().satisfies(binding -> {
            assertThat(binding.exchange()).isEqualTo("cmd.direct");
            assertThat(binding.queue()).isEqualTo("q.cmd.email");
            assertThat(binding.routingKey()).isEqualTo("email.send");
        });
    }

    @Test
    void listarLosBindingsDeUnaColaInexistenteEsNoEncontrado() {
        assertThatThrownBy(() -> service.listBindings("q.nada")).isInstanceOf(QueueNotFoundException.class);
    }

    @Test
    void eliminaUnBindingDePrueba() {
        queueExists("q.prueba");
        when(management.listQueueBindings("q.prueba")).thenReturn(List.of(
                new ManagementBinding("cmd.direct", "q.prueba", "queue", "prueba.key")));

        service.deleteBinding(new BindingRequest("cmd.direct", "q.prueba", "prueba.key"));

        verify(amqpAdmin).removeBinding(any(Binding.class));
    }

    @Test
    void noEliminaUnBindingEntreDosElementosDelSistema() {
        assertThatThrownBy(() -> service.deleteBinding(new BindingRequest("cmd.direct", "q.cmd.email", "email.send")))
                .isInstanceOf(ProtectedResourceException.class);
        verify(amqpAdmin, never()).removeBinding(any(Binding.class));
    }

    @Test
    void eliminarUnBindingInexistenteEsNoEncontrado() {
        queueExists("q.prueba");
        when(management.listQueueBindings("q.prueba")).thenReturn(List.of());

        assertThatThrownBy(() -> service.deleteBinding(new BindingRequest("cmd.direct", "q.prueba", "otra")))
                .isInstanceOf(BindingNotFoundException.class);
        verify(amqpAdmin, never()).removeBinding(any(Binding.class));
    }
}
