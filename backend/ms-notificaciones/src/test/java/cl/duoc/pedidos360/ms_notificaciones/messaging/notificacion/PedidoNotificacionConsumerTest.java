package cl.duoc.pedidos360.ms_notificaciones.messaging.notificacion;

import cl.duoc.pedidos360.events.PedidoEvent;
import cl.duoc.pedidos360.ms_notificaciones.service.NotificacionService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class PedidoNotificacionConsumerTest {

    private final NotificacionService service = mock(NotificacionService.class);
    private final Channel channel = mock(Channel.class);
    private final PedidoNotificacionConsumer consumer = new PedidoNotificacionConsumer(service);

    @Test
    void eventoValido_hace_ack() throws Exception {
        consumer.onPedidoEvent(new PedidoEvent(1L, "a@b.cl", 100.0, "CREADO", "x"),
                channel, 1L, "pedido.creado", false);
        verify(channel).basicAck(1L, false);
    }

    @Test
    void emailVacio_hace_nack_sin_requeue() throws Exception {
        consumer.onPedidoEvent(new PedidoEvent(1L, "", 100.0, "CREADO", "x"),
                channel, 2L, "pedido.creado", false);
        verify(channel).basicNack(2L, false, false);
    }
}