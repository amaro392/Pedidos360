package cl.duoc.pedidos360.ms_notificaciones.messaging.dlq;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

// Apagado por defecto (app.rabbitmq.dlq-listener-enabled=false) para poder ver
// los mensajes acumulados en el dashboard. Si se activa, los registra en log y los vacia.
@Component
@ConditionalOnProperty(name = "app.rabbitmq.dlq-listener-enabled", havingValue = "true")
public class DeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterConsumer.class);

    @RabbitListener(queues = {"${app.rabbitmq.queues.notificaciones-dlq}", "${app.rabbitmq.queues.tickets-dlq}"})
    public void onDead(Message message, Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        log.error("[DLQ] mensaje no entregado. x-death={} body={}",
                message.getMessageProperties().getHeaders().get("x-death"),
                new String(message.getBody()));
        channel.basicAck(tag, false);
    }
}