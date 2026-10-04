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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Registra en el log cada mensaje no entregado que llega a una DLQ.
 *
 * Se activa con app.rabbitmq.dlq-listener-enabled=true (variable de entorno
 * DLQ_LISTENER_ENABLED). Esta APAGADO por defecto para que los mensajes se
 * queden en la DLQ y se vean en el dashboard de RabbitMQ; al activarlo, cada
 * mensaje se registra y se confirma (ACK), con lo que la DLQ queda vacia.
 */
@Component
@ConditionalOnProperty(name = "app.rabbitmq.dlq-listener-enabled", havingValue = "true")
public class DeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterConsumer.class);

    @RabbitListener(queues = {"${app.rabbitmq.queues.notificaciones-dlq}", "${app.rabbitmq.queues.tickets-dlq}","${app.rabbitmq.queues.documentos-dlq}" })
    public void onDead(Message message, Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        String origen = "desconocido";
        String motivo = "desconocido";
        Object intentos = "?";

        Object xDeath = message.getMessageProperties().getHeaders().get("x-death");
        if (xDeath instanceof List<?> lista && !lista.isEmpty() && lista.get(0) instanceof Map<?, ?> d) {
            origen = String.valueOf(d.get("queue"));
            motivo = String.valueOf(d.get("reason"));
            intentos = d.get("count");
        }

        log.error("[DLQ] mensaje no entregado | colaOrigen={} motivo={} veces={} body={}",
                origen, motivo, intentos, new String(message.getBody(), StandardCharsets.UTF_8));
        channel.basicAck(tag, false);
    }
}
