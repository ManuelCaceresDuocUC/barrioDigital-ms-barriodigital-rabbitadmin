package cl.barriodigital.rabbitadmin.config;

import java.net.http.HttpClient;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Configuración del servicio. El {@code AmqpAdmin} lo aporta la autoconfiguración de Spring Boot
 * a partir de {@code spring.rabbitmq.*}; aquí solo se define el cliente HTTP del Management API.
 */
@Configuration
@EnableConfigurationProperties({ ManagementProperties.class, ProtectionProperties.class })
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
}
