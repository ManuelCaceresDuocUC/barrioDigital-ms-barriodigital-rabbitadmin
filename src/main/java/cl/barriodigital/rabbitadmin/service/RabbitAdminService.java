package cl.barriodigital.rabbitadmin.service;

import java.util.List;

import cl.barriodigital.rabbitadmin.dto.BindingRequest;
import cl.barriodigital.rabbitadmin.dto.BindingResponse;
import cl.barriodigital.rabbitadmin.dto.CreateExchangeRequest;
import cl.barriodigital.rabbitadmin.dto.CreateQueueRequest;
import cl.barriodigital.rabbitadmin.dto.ExchangeResponse;
import cl.barriodigital.rabbitadmin.dto.QueueResponse;

/**
 * QUÉ ES: contrato (interfaz) de las operaciones de administración. La implementación es RabbitAdminServiceImpl.
 * Separar interfaz e implementación permite cambiar la librería sin tocar los controladores.
 * Administración de la topología de RabbitMQ. Es el único punto de entrada: los controladores no
 * conocen la librería de mensajería.
 */
public interface RabbitAdminService {

    // --- Colas ---
    QueueResponse createQueue(CreateQueueRequest request);

    List<QueueResponse> listQueues();

    QueueResponse getQueue(String name);

    void deleteQueue(String name);

    /** Vacía una cola; se permite también sobre colas del sistema (por ejemplo, las DLQ). */
    void purgeQueue(String name);

    // --- Exchanges ---
    ExchangeResponse createExchange(CreateExchangeRequest request);

    List<ExchangeResponse> listExchanges();

    void deleteExchange(String name);

    // --- Bindings ---
    BindingResponse createBinding(BindingRequest request);

    /** Lista los bindings hacia colas; si {@code queue} no es nulo, solo los de esa cola. */
    List<BindingResponse> listBindings(String queue);

    void deleteBinding(BindingRequest request);
}
