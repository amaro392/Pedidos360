package cl.duoc.pedidos360.ms_notificaciones.messaging.documento;

import cl.duoc.pedidos360.events.PedidoEvent;
import cl.duoc.pedidos360.ms_notificaciones.entity.Notificacion;
import cl.duoc.pedidos360.ms_notificaciones.messaging.ConsumerErrorHandler;
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
public class DocumentoConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocumentoConsumer.class);
    private final NotificacionService service;
    private final ConsumerErrorHandler errores;

    public DocumentoConsumer(NotificacionService service, ConsumerErrorHandler errores) {
        this.service = service;
        this.errores = errores;
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.documentos}")
    public void onPedidoCreado(PedidoEvent ev, Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag,
            @Header(AmqpHeaders.REDELIVERED) boolean redelivered) throws IOException {
        try {
            if (ev.pedidoId() == null) {
                throw new IllegalArgumentException("pedidoId nulo");
            }
            String codigo = "DOC-" + ev.pedidoId();
            Notificacion t = new Notificacion();
            t.setDestinatarioEmail(ev.clienteEmail() == null ? "sin-email" : ev.clienteEmail());
            t.setTipo("DOCUMENTO_GENERADO");
            t.setAsunto("Comprobante " + codigo);
            t.setMensaje("Comprobante " + codigo + " generado por $" + ev.total());
            t.setPedidoId(ev.pedidoId());
            service.enviar(t);
            channel.basicAck(tag, false);
            log.info("ACK documento {}", codigo);
        } catch (Exception e) {
            errores.manejar(channel, tag, redelivered, e, "documento", "pedidoId=" + ev.pedidoId());
        }
    }
}
