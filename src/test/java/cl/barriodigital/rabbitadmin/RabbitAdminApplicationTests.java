package cl.barriodigital.rabbitadmin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.rabbitmq.username=test",
        "spring.rabbitmq.password=test"
})
class RabbitAdminApplicationTests {

    @Test
    void contextLoads() {
    }
}
