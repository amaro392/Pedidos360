package cl.duoc.pedidos360.ms_notificaciones.messaging.notificacion;

import cl.duoc.pedidos360.events.PedidoEvent;
import cl.duoc.pedidos360.ms_notificaciones.entity.Notificacion;
import cl.duoc.pedidos360.ms_notificaciones.messaging.ConsumerErrorHandler;
import cl.duoc.pedidos360.ms_notificaciones.service.NotificacionService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PedidoNotificacionConsumerTest {

    private final NotificacionService service = mock(NotificacionService.class);
    private final Channel channel = mock(Channel.class);
    private final PedidoNotificacionConsumer consumer =
            new PedidoNotificacionConsumer(service, new ConsumerErrorHandler());

    private PedidoEvent evento(String email) {
        return new PedidoEvent(1L, email, 100.0, "CREADO", "x");
    }

    @Test
    void eventoValido_hace_ack() throws Exception {
        consumer.onPedidoEvent(evento("a@b.cl"), channel, 1L, "pedido.creado", false);
        verify(channel).basicAck(1L, false);
    }

    @Test
    void emailVacio_hace_nack_sin_requeue() throws Exception {
        consumer.onPedidoEvent(evento(""), channel, 2L, "pedido.creado", false);
        verify(channel).basicNack(2L, false, false);
    }

    @Test
    void errorRecuperable_primera_vez_hace_requeue() throws Exception {
        when(service.enviar(any(Notificacion.class))).thenThrow(new RuntimeException("BD caida"));
        consumer.onPedidoEvent(evento("a@b.cl"), channel, 3L, "pedido.creado", false);
        verify(channel).basicNack(3L, false, true);
    }

    @Test
    void errorRecuperable_reentregado_va_a_dlq() throws Exception {
        when(service.enviar(any(Notificacion.class))).thenThrow(new RuntimeException("BD caida"));
        consumer.onPedidoEvent(evento("a@b.cl"), channel, 4L, "pedido.creado", true);
        verify(channel).basicNack(4L, false, false);
    }
}
