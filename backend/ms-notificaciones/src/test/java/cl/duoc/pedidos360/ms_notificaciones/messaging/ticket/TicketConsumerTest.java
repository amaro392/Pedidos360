package cl.duoc.pedidos360.ms_notificaciones.messaging.ticket;

import cl.duoc.pedidos360.events.PedidoEvent;
import cl.duoc.pedidos360.ms_notificaciones.entity.Notificacion;
import cl.duoc.pedidos360.ms_notificaciones.messaging.ConsumerErrorHandler;
import cl.duoc.pedidos360.ms_notificaciones.service.NotificacionService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TicketConsumerTest {

    private final NotificacionService service = mock(NotificacionService.class);
    private final Channel channel = mock(Channel.class);
    private final TicketConsumer consumer = new TicketConsumer(service, new ConsumerErrorHandler());

    @Test
    void pedidoValido_genera_ticket_y_hace_ack() throws Exception {
        consumer.onPedidoCreado(new PedidoEvent(7L, "a@b.cl", 50.0, "CREADO", "x"), channel, 1L, false);
        verify(service).enviar(any(Notificacion.class));
        verify(channel).basicAck(1L, false);
    }

    @Test
    void pedidoIdNulo_va_a_dlq_sin_requeue() throws Exception {
        consumer.onPedidoCreado(new PedidoEvent(null, "a@b.cl", 50.0, "CREADO", "x"), channel, 2L, false);
        verify(channel).basicNack(2L, false, false);
    }

    @Test
    void errorRecuperable_reentregado_va_a_dlq() throws Exception {
        when(service.enviar(any(Notificacion.class))).thenThrow(new RuntimeException("BD caida"));
        consumer.onPedidoCreado(new PedidoEvent(7L, "a@b.cl", 50.0, "CREADO", "x"), channel, 3L, true);
        verify(channel).basicNack(3L, false, false);
    }
}
