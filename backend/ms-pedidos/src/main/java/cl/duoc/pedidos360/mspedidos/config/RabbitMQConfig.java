package cl.duoc.pedidos360.mspedidos.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange pedidosExchange(@Value("${app.rabbitmq.exchanges.pedidos}") String name) {
        return new TopicExchange(name, true, false);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter("cl.duoc.pedidos360.events");
    }
}