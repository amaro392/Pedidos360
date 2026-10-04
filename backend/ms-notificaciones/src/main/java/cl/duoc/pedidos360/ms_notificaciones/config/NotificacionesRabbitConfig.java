package cl.duoc.pedidos360.ms_notificaciones.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificacionesRabbitConfig {

    @Value("${app.rabbitmq.exchanges.dlx}") private String dlx;
    @Value("${app.rabbitmq.routing-keys.notificaciones-dead}") private String deadKey;

    @Bean
    public Queue notificacionesQueue(@Value("${app.rabbitmq.queues.notificaciones}") String name) {
        return QueueBuilder.durable(name).quorum()
                .deadLetterExchange(dlx).deadLetterRoutingKey(deadKey).build();
    }

    @Bean
    public Queue notificacionesDlq(@Value("${app.rabbitmq.queues.notificaciones-dlq}") String name) {
        return QueueBuilder.durable(name).quorum().build();
    }

    @Bean
    public Binding notificacionesBinding(@Qualifier("notificacionesQueue") Queue q,
            TopicExchange pedidosExchange,
            @Value("${app.rabbitmq.routing-keys.pedido-todos}") String key) {
        return BindingBuilder.bind(q).to(pedidosExchange).with(key);
    }

    @Bean
    public Binding notificacionesDlqBinding(@Qualifier("notificacionesDlq") Queue q,
            DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(q).to(deadLetterExchange).with(deadKey);
    }
}