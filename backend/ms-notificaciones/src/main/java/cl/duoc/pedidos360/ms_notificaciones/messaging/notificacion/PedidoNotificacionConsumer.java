package cl.duoc.pedidos360.ms_notificaciones.messaging.notificacion;

import cl.duoc.pedidos360.events.PedidoEvent;
import cl.duoc.pedidos360.ms_notificaciones.entity.Notificacion;
import cl.duoc.pedidos360.ms_notificaciones.service.NotificacionService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PedidoNotificacionConsumer {

    private static final Logger log = LoggerFactory.getLogger(PedidoNotificacionConsumer.class);
    private final NotificacionService service;

    public PedidoNotificacionConsumer(NotificacionService service) {
        this.service = service;
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.notificaciones}")
    public void onPedidoEvent(PedidoEvent ev, Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag,
            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey,
            @Header(AmqpHeaders.REDELIVERED) boolean redelivered) throws IOException {
        try {
            if (ev.clienteEmail() == null || ev.clienteEmail().isBlank()) {
                throw new IllegalArgumentException("clienteEmail vacio");
            }
            Notificacion n = new Notificacion();
            n.setDestinatarioEmail(ev.clienteEmail());
            n.setTipo(routingKey.toUpperCase().replace('.', '_'));
            n.setAsunto("Pedido #" + ev.pedidoId() + " " + ev.estado());
            n.setMensaje("Tu pedido #" + ev.pedidoId() + " por $" + ev.total() + " esta " + ev.estado());
            n.setPedidoId(ev.pedidoId());
            service.enviar(n);
            channel.basicAck(tag, false);
            log.info("ACK notificacion pedidoId={} key={}", ev.pedidoId(), routingKey);
        } catch (IllegalArgumentException e) {
            // Error NO recuperable: directo a la DLQ (via DLX)
            log.error("NACK->DLQ (no recuperable) pedidoId={}: {}", ev.pedidoId(), e.getMessage());
            channel.basicNack(tag, false, false);
        } catch (Exception e) {
            // Error recuperable: un reintento y luego DLQ
            if (!redelivered) {
                log.warn("NACK requeue (reintento) pedidoId={}: {}", ev.pedidoId(), e.getMessage());
                channel.basicNack(tag, false, true);
            } else {
                log.error("NACK->DLQ (reintento agotado) pedidoId={}: {}", ev.pedidoId(), e.getMessage());
                channel.basicNack(tag, false, false);
            }
        }
    }
}