package cl.barriodigital.rabbitadmin.service;

import java.util.List;

import cl.barriodigital.rabbitadmin.dto.BindingRequest;
import cl.barriodigital.rabbitadmin.dto.BindingResponse;
import cl.barriodigital.rabbitadmin.dto.CreateExchangeRequest;
import cl.barriodigital.rabbitadmin.dto.CreateQueueRequest;
import cl.barriodigital.rabbitadmin.dto.ExchangeResponse;
import cl.barriodigital.rabbitadmin.dto.QueueResponse;

/**
 * Administración de la topología de RabbitMQ. Es el único punto de entrada: los controladores no
 * conocen la librería de mensajería.
 */
public interface RabbitAdminService {

    QueueResponse createQueue(CreateQueueRequest request);

    List<QueueResponse> listQueues();

    QueueResponse getQueue(String name);

    void deleteQueue(String name);

    /** Vacía una cola; se permite también sobre colas del sistema (por ejemplo, las DLQ). */
    void purgeQueue(String name);

    ExchangeResponse createExchange(CreateExchangeRequest request);

    List<ExchangeResponse> listExchanges();

    void deleteExchange(String name);

    BindingResponse createBinding(BindingRequest request);

    /** Lista los bindings hacia colas; si {@code queue} no es nulo, solo los de esa cola. */
    List<BindingResponse> listBindings(String queue);

    void deleteBinding(BindingRequest request);
}
