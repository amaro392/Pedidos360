package cl.duoc.pedidos360.msrabbitadmin.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RabbitManagementConfig {

    @Bean
    @Qualifier("rabbitManagement")
    public RestClient rabbitManagementClient(
            @Value("${app.rabbitmq.management-url}") String managementUrl,
            @Value("${spring.rabbitmq.username}") String user,
            @Value("${spring.rabbitmq.password}") String pass) {
        return RestClient.builder().baseUrl(managementUrl)
                .defaultHeaders(h -> h.setBasicAuth(user, pass)).build();
    }
}