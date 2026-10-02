package cl.duoc.pedidos360.ms_notificaciones.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitCommonConfig {

    @Bean
    public TopicExchange pedidosExchange(@Value("${app.rabbitmq.exchanges.pedidos}") String n) {
        return new TopicExchange(n, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange(@Value("${app.rabbitmq.exchanges.dlx}") String n) {
        return new DirectExchange(n, true, false);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter("cl.duoc.pedidos360.events");
    }
}