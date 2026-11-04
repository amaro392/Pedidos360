package cl.duoc.pedidos360.events;

/**
 * Mensaje JSON del topico Kafka "clientes.registro".
 * Lo publica ms-clientes al registrar un cliente y lo consume ms-notificaciones (bienvenida).
 */
public record ClienteEvent(Long clienteId, String nombre, String email, String tipo, String ocurridoEn) {}
