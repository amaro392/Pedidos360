package cl.duoc.pedidos360.mspedidos.messaging;

import cl.duoc.pedidos360.events.PedidoEvent;
import cl.duoc.pedidos360.mspedidos.entity.Pedido;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PedidoEventPublisherTest {

    private final RabbitTemplate template = mock(RabbitTemplate.class);
    private final PedidoEventPublisher publisher =
            new PedidoEventPublisher(template, "pedidos.exchange", "pedido.creado", "pedido.cancelado");

    private Pedido pedido() {
        Pedido p = new Pedido();
        p.setId(5L);
        p.setClienteEmail("cliente@duocuc.cl");
        p.setEstado("PENDIENTE");
        p.setTotal(19990.0);
        return p;
    }

    @Test
    void publicarCreado_envia_evento_al_exchange_con_la_routing_key() {
        publisher.publicarCreado(pedido());

        ArgumentCaptor<PedidoEvent> captor = ArgumentCaptor.forClass(PedidoEvent.class);
        verify(template).convertAndSend(eq("pedidos.exchange"), eq("pedido.creado"), captor.capture());
        assertEquals(5L, captor.getValue().pedidoId());
        assertEquals("cliente@duocuc.cl", captor.getValue().clienteEmail());
    }

    @Test
    void publicarCancelado_usa_la_routing_key_de_cancelado() {
        publisher.publicarCancelado(pedido());
        verify(template).convertAndSend(eq("pedidos.exchange"), eq("pedido.cancelado"), any(PedidoEvent.class));
    }

    @Test
    void si_rabbit_falla_no_se_propaga_la_excepcion() {
        doThrow(new AmqpConnectException(new IOException("sin broker")))
                .when(template).convertAndSend(anyString(), anyString(), any(PedidoEvent.class));
        assertDoesNotThrow(() -> publisher.publicarCreado(pedido()));
    }
}
