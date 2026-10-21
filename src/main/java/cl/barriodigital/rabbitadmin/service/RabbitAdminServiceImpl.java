package cl.barriodigital.rabbitadmin.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import cl.barriodigital.rabbitadmin.client.ManagementBinding;
import cl.barriodigital.rabbitadmin.client.ManagementExchange;
import cl.barriodigital.rabbitadmin.client.ManagementQueue;
import cl.barriodigital.rabbitadmin.client.RabbitManagementClient;
import cl.barriodigital.rabbitadmin.dto.BindingRequest;
import cl.barriodigital.rabbitadmin.dto.BindingResponse;
import cl.barriodigital.rabbitadmin.dto.CreateExchangeRequest;
import cl.barriodigital.rabbitadmin.dto.CreateQueueRequest;
import cl.barriodigital.rabbitadmin.dto.ExchangeResponse;
import cl.barriodigital.rabbitadmin.dto.QueueResponse;
import cl.barriodigital.rabbitadmin.exception.BindingNotFoundException;
import cl.barriodigital.rabbitadmin.exception.BrokerOperationException;
import cl.barriodigital.rabbitadmin.exception.ExchangeAlreadyExistsException;
import cl.barriodigital.rabbitadmin.exception.ExchangeNotFoundException;
import cl.barriodigital.rabbitadmin.exception.InvalidTopologyException;
import cl.barriodigital.rabbitadmin.exception.ManagementApiException;
import cl.barriodigital.rabbitadmin.exception.ProtectedResourceException;
import cl.barriodigital.rabbitadmin.exception.QueueAlreadyExistsException;
import cl.barriodigital.rabbitadmin.exception.QueueNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Única clase que usa {@link AmqpAdmin}. Las operaciones que AMQP no ofrece (listados y propiedades)
 * se delegan en {@link RabbitManagementClient}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitAdminServiceImpl implements RabbitAdminService {

    private static final String SYSTEM_PREFIX = "amq.";

    private final AmqpAdmin amqpAdmin;
    private final RabbitManagementClient management;
    private final ProtectedResources protectedResources;

    // ---------------------------------------------------------------- colas

    @Override
    public QueueResponse createQueue(CreateQueueRequest request) {
        String name = requireName(request.name(), "cola");
        if (queueExists(name)) {
            throw new QueueAlreadyExistsException(name);
        }

        boolean durable = request.durable() == null || request.durable();
        boolean autoDelete = Boolean.TRUE.equals(request.autoDelete());
        boolean hasDlx = StringUtils.hasText(request.deadLetterExchange());
        if (StringUtils.hasText(request.deadLetterRoutingKey()) && !hasDlx) {
            throw new InvalidTopologyException("deadLetterRoutingKey requiere indicar deadLetterExchange");
        }

        QueueBuilder builder = durable ? QueueBuilder.durable(name) : QueueBuilder.nonDurable(name);
        if (autoDelete) {
            builder.autoDelete();
        }
        if (hasDlx) {
            builder.deadLetterExchange(request.deadLetterExchange());
        }
        if (StringUtils.hasText(request.deadLetterRoutingKey())) {
            builder.deadLetterRoutingKey(request.deadLetterRoutingKey());
        }
        if (request.messageTtlMs() != null) {
            builder.ttl(request.messageTtlMs());
        }
        Queue queue = builder.build();

        run("crear la cola '" + name + "'", () -> amqpAdmin.declareQueue(queue));
        log.info("Cola creada: {} (durable={}, autoDelete={})", name, durable, autoDelete);

        // Se responde con lo que se acaba de declarar: el Management API puede tardar en reflejarla.
        return new QueueResponse(name, durable, autoDelete, queue.getArguments(), 0, 0, null,
                protectedResources.isProtectedQueue(name));
    }

    @Override
    public List<QueueResponse> listQueues() {
        return management.listQueues().stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(QueueResponse::name))
                .toList();
    }

    @Override
    public QueueResponse getQueue(String name) {
        QueueInformation info = call("consultar la cola '" + name + "'", () -> amqpAdmin.getQueueInfo(name));
        if (info == null) {
            throw new QueueNotFoundException(name);
        }
        // Mensajes y consumidores salen de AMQP (tiempo real); el resto, del Management API si responde.
        Optional<ManagementQueue> details = findDetails(name);
        return new QueueResponse(
                name,
                details.map(ManagementQueue::durable).orElse(null),
                details.map(ManagementQueue::autoDelete).orElse(null),
                details.map(ManagementQueue::arguments).orElse(null),
                info.getMessageCount(),
                info.getConsumerCount(),
                details.map(ManagementQueue::state).orElse(null),
                protectedResources.isProtectedQueue(name));
    }

    @Override
    public void deleteQueue(String name) {
        if (protectedResources.isProtectedQueue(name)) {
            throw new ProtectedResourceException("cola", name);
        }
        requireQueue(name);
        run("eliminar la cola '" + name + "'", () -> amqpAdmin.deleteQueue(name));
        log.info("Cola eliminada: {}", name);
    }

    @Override
    public void purgeQueue(String name) {
        requireQueue(name);
        run("vaciar la cola '" + name + "'", () -> amqpAdmin.purgeQueue(name));
        log.info("Cola vaciada: {}", name);
    }

    // ------------------------------------------------------------ exchanges

    @Override
    public ExchangeResponse createExchange(CreateExchangeRequest request) {
        String name = requireName(request.name(), "exchange");
        if (request.type() == null) {
            throw new InvalidTopologyException("El tipo de exchange es obligatorio (direct, topic, fanout o headers)");
        }
        if (exchangeExists(name)) {
            throw new ExchangeAlreadyExistsException(name);
        }

        boolean durable = request.durable() == null || request.durable();
        boolean autoDelete = Boolean.TRUE.equals(request.autoDelete());
        Exchange exchange = switch (request.type()) {
            case DIRECT -> new DirectExchange(name, durable, autoDelete);
            case TOPIC -> new TopicExchange(name, durable, autoDelete);
            case FANOUT -> new FanoutExchange(name, durable, autoDelete);
            case HEADERS -> new HeadersExchange(name, durable, autoDelete);
        };

        run("crear el exchange '" + name + "'", () -> amqpAdmin.declareExchange(exchange));
        log.info("Exchange creado: {} (tipo={})", name, request.type().wireName());
        return new ExchangeResponse(name, request.type().wireName(), durable, autoDelete,
                protectedResources.isProtectedExchange(name));
    }

    @Override
    public List<ExchangeResponse> listExchanges() {
        return management.listExchanges().stream()
                .filter(exchange -> StringUtils.hasText(exchange.name())) // omite el exchange por defecto
                .map(this::toResponse)
                .sorted(Comparator.comparing(ExchangeResponse::name))
                .toList();
    }

    @Override
    public void deleteExchange(String name) {
        // Exchange por defecto (nombre vacío) y exchanges amq.*: son del broker, petición inválida (400).
        requireName(name, "exchange");
        // Exchanges de la topología del caso (cmd.*): existen pero están protegidos (409).
        if (protectedResources.isProtectedExchange(name)) {
            throw new ProtectedResourceException("exchange", name);
        }
        if (!exchangeExists(name)) {
            throw new ExchangeNotFoundException(name);
        }
        run("eliminar el exchange '" + name + "'", () -> amqpAdmin.deleteExchange(name));
        log.info("Exchange eliminado: {}", name);
    }

    // ------------------------------------------------------------- bindings

    @Override
    public BindingResponse createBinding(BindingRequest request) {
        String exchange = requireName(request.exchange(), "exchange");
        String queue = requireName(request.queue(), "cola");
        String routingKey = request.routingKey() == null ? "" : request.routingKey();
        requireQueue(queue);
        if (!exchangeExists(exchange)) {
            throw new ExchangeNotFoundException(exchange);
        }

        Binding binding = new Binding(queue, Binding.DestinationType.QUEUE, exchange, routingKey, null);
        run("crear el binding " + exchange + " -> " + queue, () -> amqpAdmin.declareBinding(binding));
        log.info("Binding creado: {} -> {} (routingKey='{}')", exchange, queue, routingKey);
        return new BindingResponse(exchange, queue, routingKey);
    }

    @Override
    public List<BindingResponse> listBindings(String queue) {
        List<ManagementBinding> bindings;
        if (StringUtils.hasText(queue)) {
            requireQueue(queue);
            bindings = management.listQueueBindings(queue);
        } else {
            bindings = management.listBindings();
        }
        return bindings.stream()
                .filter(binding -> StringUtils.hasText(binding.source())) // omite los implícitos del exchange por defecto
                .filter(binding -> "queue".equals(binding.destinationType()))
                .map(binding -> new BindingResponse(binding.source(), binding.destination(),
                        binding.routingKey() == null ? "" : binding.routingKey()))
                .toList();
    }

    @Override
    public void deleteBinding(BindingRequest request) {
        String exchange = requireName(request.exchange(), "exchange");
        String queue = requireName(request.queue(), "cola");
        String routingKey = request.routingKey() == null ? "" : request.routingKey();

        // Solo se protege el binding entre dos elementos del sistema; los de prueba sí se pueden quitar.
        if (protectedResources.isProtectedQueue(queue) && protectedResources.isProtectedExchange(exchange)) {
            throw new ProtectedResourceException("binding", exchange + " -> " + queue);
        }
        requireQueue(queue);
        boolean exists = management.listQueueBindings(queue).stream()
                .anyMatch(binding -> exchange.equals(binding.source())
                        && routingKey.equals(binding.routingKey() == null ? "" : binding.routingKey()));
        if (!exists) {
            throw new BindingNotFoundException(exchange, queue, routingKey);
        }

        Binding binding = new Binding(queue, Binding.DestinationType.QUEUE, exchange, routingKey, null);
        run("eliminar el binding " + exchange + " -> " + queue, () -> amqpAdmin.removeBinding(binding));
        log.info("Binding eliminado: {} -> {} (routingKey='{}')", exchange, queue, routingKey);
    }

    // -------------------------------------------------------------- helpers

    private String requireName(String name, String kind) {
        if (!StringUtils.hasText(name)) {
            throw new InvalidTopologyException("El nombre de la " + kind + " no puede estar vacío");
        }
        if (name.startsWith(SYSTEM_PREFIX)) {
            throw new InvalidTopologyException("Los nombres que empiezan con '" + SYSTEM_PREFIX + "' están reservados");
        }
        return name;
    }

    private void requireQueue(String name) {
        if (!queueExists(name)) {
            throw new QueueNotFoundException(name);
        }
    }

    private boolean queueExists(String name) {
        return call("consultar la cola '" + name + "'", () -> amqpAdmin.getQueueInfo(name)) != null;
    }

    private boolean exchangeExists(String name) {
        return management.exchangeExists(name);
    }

    private Optional<ManagementQueue> findDetails(String name) {
        try {
            return management.findQueue(name);
        } catch (ManagementApiException ex) {
            log.debug("No se pudieron leer los detalles de la cola {} en el Management API", name, ex);
            return Optional.empty();
        }
    }

    private QueueResponse toResponse(ManagementQueue queue) {
        return new QueueResponse(queue.name(), queue.durable(), queue.autoDelete(), queue.arguments(),
                queue.messages() == null ? 0 : queue.messages(),
                queue.consumers() == null ? 0 : queue.consumers(),
                queue.state(), protectedResources.isProtectedQueue(queue.name()));
    }

    private ExchangeResponse toResponse(ManagementExchange exchange) {
        return new ExchangeResponse(exchange.name(), exchange.type(),
                Boolean.TRUE.equals(exchange.durable()), Boolean.TRUE.equals(exchange.autoDelete()),
                protectedResources.isProtectedExchange(exchange.name()));
    }

    private void run(String action, Runnable operation) {
        try {
            operation.run();
        } catch (AmqpException ex) {
            throw new BrokerOperationException("RabbitMQ no pudo " + action, ex);
        }
    }

    private <T> T call(String action, Supplier<T> operation) {
        try {
            return operation.get();
        } catch (AmqpException ex) {
            throw new BrokerOperationException("RabbitMQ no pudo " + action, ex);
        }
    }
}
