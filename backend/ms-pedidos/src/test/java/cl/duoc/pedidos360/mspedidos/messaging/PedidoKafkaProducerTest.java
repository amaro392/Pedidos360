package cl.duoc.pedidos360.mspedidos.messaging;

import cl.duoc.pedidos360.events.PedidoVentaEvent;
import cl.duoc.pedidos360.mspedidos.entity.DetallePedido;
import cl.duoc.pedidos360.mspedidos.entity.Pedido;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PedidoKafkaProducerTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> template = mock(KafkaTemplate.class);
    private final PedidoKafkaProducer producer = new PedidoKafkaProducer(template, "pedidos.ventas");

    private Pedido pedido() {
        Pedido p = new Pedido();
        p.setId(7L);
        p.setClienteEmail("cliente@duocuc.cl");
        p.setEstado("PENDIENTE");
        p.setTotal(25980.0);
        DetallePedido d = new DetallePedido();
        d.setNombreProducto("Teclado");
        d.setCantidad(2);
        d.setPrecioUnitario(12990.0);
        p.agregarItem(d);
        return p;
    }

    @Test
    void publicarVenta_envia_el_evento_al_topico_con_el_id_como_clave() {
        when(template.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

        producer.publicarVenta(pedido());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(template).send(eq("pedidos.ventas"), eq("7"), captor.capture());
        PedidoVentaEvent ev = (PedidoVentaEvent) captor.getValue();
        assertEquals(7L, ev.pedidoId());
        assertEquals("cliente@duocuc.cl", ev.clienteEmail());
        assertEquals(1, ev.items().size());
        assertEquals("Teclado", ev.items().get(0).nombreProducto());
        assertEquals(2, ev.items().get(0).cantidad());
    }

    @Test
    void si_kafka_falla_no_se_propaga_la_excepcion() {
        when(template.send(anyString(), anyString(), any())).thenThrow(new IllegalStateException("sin broker"));
        assertDoesNotThrow(() -> producer.publicarVenta(pedido()));
    }

    @Test
    void si_el_envio_falla_de_forma_asincrona_no_se_propaga_la_excepcion() {
        when(template.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("timeout")));
        assertDoesNotThrow(() -> producer.publicarVenta(pedido()));
    }
}
