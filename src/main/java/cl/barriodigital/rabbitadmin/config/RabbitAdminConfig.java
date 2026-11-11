package cl.barriodigital.rabbitadmin.config;

import java.net.http.HttpClient;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuración del servicio. El {@code AmqpAdmin} lo aporta la autoconfiguración de Spring Boot
 * a partir de {@code spring.rabbitmq.*}; aquí se definen el cliente HTTP del Management API y la
 * plantilla con la que se publican los mensajes de los demás microservicios.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({ ManagementProperties.class, ProtectionProperties.class, InspectionProperties.class,
        PublishingProperties.class })
public class RabbitAdminConfig {

    @Bean
    RestClient managementRestClient(ManagementProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.timeout());

        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeaders(headers -> headers.setBasicAuth(properties.username(), properties.password()))
                .build();
    }

    @Bean
    MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    /**
     * Plantilla con la que se publican los mensajes de los demás servicios. {@code mandatory} y los
     * callbacks dejan en el log lo que el broker no pudo enrutar o rechazó.
     */
    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true);
        template.setReturnsCallback(returned -> log.error(
                "Mensaje sin ruta: exchange={}, routingKey={}, motivo={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
        template.setConfirmCallback((correlation, ack, cause) -> {
            if (!ack) {
                log.error("El broker rechazó el mensaje {}: {}", correlation, cause);
            }
        });
        return template;
    }
}
