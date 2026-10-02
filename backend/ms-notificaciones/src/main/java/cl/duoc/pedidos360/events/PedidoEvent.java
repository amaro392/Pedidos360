package cl.duoc.pedidos360.events;

public record PedidoEvent(Long pedidoId, String clienteEmail, Double total,
                          String estado, String ocurridoEn) {}