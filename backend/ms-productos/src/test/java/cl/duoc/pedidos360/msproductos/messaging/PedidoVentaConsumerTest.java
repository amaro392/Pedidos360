package cl.duoc.pedidos360.msproductos.messaging;

import cl.duoc.pedidos360.events.PedidoVentaEvent;
import cl.duoc.pedidos360.msproductos.analitica.VentasAnaliticaService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PedidoVentaConsumerTest {

    private final VentasAnaliticaService analitica = new VentasAnaliticaService();
    private final PedidoVentaConsumer consumer = new PedidoVentaConsumer(analitica);

    @Test
    void eventoValido_alimenta_la_analitica() {
        consumer.onPedidoVenta(new PedidoVentaEvent(1L, "a@b.cl", 200.0, "PENDIENTE",
                List.of(new PedidoVentaEvent.ItemVenta("Teclado", 2, 100.0)), "x"));
        assertEquals(1, analitica.pedidosProcesados());
        assertEquals(2, analitica.ranking().get(0).unidadesVendidas());
    }

    @Test
    void eventoInvalido_lanza_excepcion_para_que_Kafka_lo_registre() {
        assertThrows(IllegalArgumentException.class, () -> consumer.onPedidoVenta(
                new PedidoVentaEvent(null, "a@b.cl", 0.0, "PENDIENTE", List.of(), "x")));
    }
}
