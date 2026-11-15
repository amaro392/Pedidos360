package cl.duoc.pedidos360.events;

import java.util.List;

/**
 * Mensaje JSON del topico Kafka "pedidos.ventas" (streaming de ventas para analitica).
 * Lo publica ms-pedidos y lo consume ms-productos.
 */
public record PedidoVentaEvent(Long pedidoId, String clienteEmail, Double total, String estado,
                               List<ItemVenta> items, String ocurridoEn) {

    public record ItemVenta(String nombreProducto, Integer cantidad, Double precioUnitario) {}
}
