package cl.duoc.pedidos360.msproductos.analitica;

import cl.duoc.pedidos360.events.PedidoVentaEvent;
import cl.duoc.pedidos360.events.PedidoVentaEvent.ItemVenta;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VentasAnaliticaServiceTest {

    private final VentasAnaliticaService service = new VentasAnaliticaService();

    private PedidoVentaEvent venta(Long id, ItemVenta... items) {
        return new PedidoVentaEvent(id, "a@b.cl", 0.0, "PENDIENTE", List.of(items), "x");
    }

    @Test
    void acumula_unidades_e_ingresos_por_producto() {
        service.registrar(venta(1L, new ItemVenta("Teclado", 2, 10000.0), new ItemVenta("Mouse", 1, 5000.0)));
        service.registrar(venta(2L, new ItemVenta("Teclado", 3, 10000.0)));

        var ranking = service.ranking();
        assertEquals(2, ranking.size());
        assertEquals("Teclado", ranking.get(0).producto());
        assertEquals(5, ranking.get(0).unidadesVendidas());
        assertEquals(50000.0, ranking.get(0).ingresos());
        assertEquals("Mouse", ranking.get(1).producto());
        assertEquals(2, service.pedidosProcesados());
    }

    @Test
    void pedidoId_nulo_es_invalido() {
        assertThrows(IllegalArgumentException.class, () -> service.registrar(venta(null)));
        assertEquals(0, service.pedidosProcesados());
    }

    @Test
    void ignora_items_incompletos_sin_fallar() {
        service.registrar(venta(1L, new ItemVenta(null, 1, 100.0), new ItemVenta("Cable", null, 100.0)));
        assertTrue(service.ranking().isEmpty());
        assertEquals(1, service.pedidosProcesados());
    }

    @Test
    void items_nulos_no_fallan() {
        service.registrar(new PedidoVentaEvent(1L, "a@b.cl", 0.0, "PENDIENTE", null, "x"));
        assertTrue(service.ranking().isEmpty());
    }
}
