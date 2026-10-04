package cl.duoc.pedidos360.ms_notificaciones.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentosRabbitConfig {

    @Value("${app.rabbitmq.exchanges.dlx}") private String dlx;
    @Value("${app.rabbitmq.routing-keys.documentos-dead}") private String deadKey;

    @Bean
    public Queue documentosQueue(@Value("${app.rabbitmq.queues.documentos}") String name) {
        return QueueBuilder.durable(name).quorum()
                .deadLetterExchange(dlx).deadLetterRoutingKey(deadKey).build();
    }

    @Bean
    public Queue documentosDlq(@Value("${app.rabbitmq.queues.documentos-dlq}") String name) {
        return QueueBuilder.durable(name).quorum().build();
    }

    @Bean
    public Binding documentosBinding(@Qualifier("documentosQueue") Queue q, TopicExchange pedidosExchange,
            @Value("${app.rabbitmq.routing-keys.pedido-creado}") String key) {
        return BindingBuilder.bind(q).to(pedidosExchange).with(key);
    }

    @Bean
    public Binding documentosDlqBinding(@Qualifier("documentosDlq") Queue q, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(q).to(deadLetterExchange).with(deadKey);
    }
}
