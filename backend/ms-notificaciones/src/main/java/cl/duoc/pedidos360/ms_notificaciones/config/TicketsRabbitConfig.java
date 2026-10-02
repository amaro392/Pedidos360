package cl.duoc.pedidos360.ms_notificaciones.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TicketsRabbitConfig {

    @Value("${app.rabbitmq.exchanges.dlx}") private String dlx;
    @Value("${app.rabbitmq.routing-keys.tickets-dead}") private String deadKey;

    @Bean
    public Queue ticketsQueue(@Value("${app.rabbitmq.queues.tickets}") String name) {
        return QueueBuilder.durable(name).quorum()
                .deadLetterExchange(dlx).deadLetterRoutingKey(deadKey).build();
    }

    @Bean
    public Queue ticketsDlq(@Value("${app.rabbitmq.queues.tickets-dlq}") String name) {
        return QueueBuilder.durable(name).quorum().build();
    }

    @Bean
    public Binding ticketsBinding(@Qualifier("ticketsQueue") Queue q, TopicExchange pedidosExchange,
            @Value("${app.rabbitmq.routing-keys.pedido-creado}") String key) {
        return BindingBuilder.bind(q).to(pedidosExchange).with(key);
    }

    @Bean
    public Binding ticketsDlqBinding(@Qualifier("ticketsDlq") Queue q, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(q).to(deadLetterExchange).with(deadKey);
    }
}