package cl.duoc.pedidos360.mspedidos.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PedidoTest {

    @Test
    void agregarItem_vinculaElItemConElPedido() {
        Pedido pedido = new Pedido();
        pedido.setClienteEmail("cliente@duocuc.cl");
        pedido.setEstado("PENDIENTE");
        pedido.setTotal(19990.0);

        DetallePedido item = new DetallePedido();
        item.setNombreProducto("Teclado Mecanico");
        item.setCantidad(1);
        item.setPrecioUnitario(19990.0);

        pedido.agregarItem(item);

        assertEquals(1, pedido.getItems().size());
        assertSame(pedido, item.getPedido());
    }

    @Test
    void setItems_relinkeaCadaItemConElPedido() {
        Pedido pedido = new Pedido();

        DetallePedido item1 = new DetallePedido();
        DetallePedido item2 = new DetallePedido();

        pedido.setItems(java.util.List.of(item1, item2));

        assertSame(pedido, item1.getPedido());
        assertSame(pedido, item2.getPedido());
    }

    @Test
    void prePersist_asignaFechaDeCreacion() {
        Pedido pedido = new Pedido();

        pedido.prePersist();

        assertEquals(java.time.LocalDate.now(), pedido.getFechaCreacion().toLocalDate());
    }
}