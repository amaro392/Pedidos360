package cl.duoc.pedidos360.ms_notificaciones.messaging.ticket;

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
public class TicketConsumer {

    private static final Logger log = LoggerFactory.getLogger(TicketConsumer.class);
    private final NotificacionService service;

    public TicketConsumer(NotificacionService service) {
        this.service = service;
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.tickets}")
    public void onPedidoCreado(PedidoEvent ev, Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag,
            @Header(AmqpHeaders.REDELIVERED) boolean redelivered) throws IOException {
        try {
            if (ev.pedidoId() == null) {
                throw new IllegalArgumentException("pedidoId nulo");
            }
            String codigo = "TCK-" + ev.pedidoId();
            Notificacion t = new Notificacion();
            t.setDestinatarioEmail(ev.clienteEmail() == null ? "sin-email" : ev.clienteEmail());
            t.setTipo("TICKET_GENERADO");
            t.setAsunto("Ticket " + codigo);
            t.setMensaje("Ticket " + codigo + " generado por $" + ev.total());
            t.setPedidoId(ev.pedidoId());
            service.enviar(t);
            channel.basicAck(tag, false);
            log.info("ACK ticket {}", codigo);
        } catch (IllegalArgumentException e) {
            log.error("NACK->DLQ ticket (no recuperable): {}", e.getMessage());
            channel.basicNack(tag, false, false);
        } catch (Exception e) {
            boolean requeue = !redelivered;
            log.warn("NACK ticket requeue={} : {}", requeue, e.getMessage());
            channel.basicNack(tag, false, requeue);
        }
    }
}