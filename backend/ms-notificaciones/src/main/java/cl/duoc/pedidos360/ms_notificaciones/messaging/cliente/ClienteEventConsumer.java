package cl.duoc.pedidos360.ms_notificaciones.messaging.cliente;

import cl.duoc.pedidos360.events.ClienteEvent;
import cl.duoc.pedidos360.ms_notificaciones.entity.Notificacion;
import cl.duoc.pedidos360.ms_notificaciones.service.NotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor Kafka del topico "clientes.registro": genera la notificacion de bienvenida.
 * Es independiente de los consumidores RabbitMQ (pedidos, tickets, documentos).
 */
@Component
public class ClienteEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ClienteEventConsumer.class);
    private final NotificacionService service;

    public ClienteEventConsumer(NotificacionService service) {
        this.service = service;
    }

    @KafkaListener(topics = "${app.kafka.topics.clientes-registro}", groupId = "${spring.kafka.consumer.group-id}")
    public void onClienteRegistrado(ClienteEvent ev) {
        if (ev.email() == null || ev.email().isBlank()) {
            throw new IllegalArgumentException("email vacio en evento de cliente");
        }
        String nombre = (ev.nombre() == null || ev.nombre().isBlank()) ? "cliente" : ev.nombre();
        Notificacion n = new Notificacion();
        n.setDestinatarioEmail(ev.email());
        n.setTipo("CLIENTE_REGISTRADO");
        n.setAsunto("Bienvenido a Pedidos360");
        n.setMensaje("Hola " + nombre + ", tu cuenta fue registrada correctamente en Pedidos360.");
        service.enviar(n);
        log.info("Notificacion de bienvenida generada clienteId={} email={}", ev.clienteId(), ev.email());
    }
}
