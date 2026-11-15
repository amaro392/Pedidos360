package cl.duoc.pedidos360.mspedidos.messaging;

import cl.duoc.pedidos360.events.PedidoVentaEvent;
import cl.duoc.pedidos360.mspedidos.entity.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Productor Kafka: publica en el topico "pedidos.ventas" cada pedido creado, con sus items.
 * Convive con PedidoEventPublisher (RabbitMQ): no lo reemplaza ni lo modifica.
 * Igual que con RabbitMQ, un fallo de Kafka NO debe romper la creacion del pedido.
 */
@Component
public class PedidoKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(PedidoKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public PedidoKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.pedidos-ventas}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publicarVenta(Pedido p) {
        try {
            List<PedidoVentaEvent.ItemVenta> items = p.getItems() == null ? List.of()
                    : p.getItems().stream()
                            .map(i -> new PedidoVentaEvent.ItemVenta(i.getNombreProducto(), i.getCantidad(), i.getPrecioUnitario()))
                            .toList();
            PedidoVentaEvent ev = new PedidoVentaEvent(p.getId(), p.getClienteEmail(), p.getTotal(),
                    p.getEstado(), items, LocalDateTime.now().toString());

            // La clave (id del pedido) asegura que los eventos de un mismo pedido vayan a la misma particion.
            kafkaTemplate.send(topic, String.valueOf(p.getId()), ev).whenComplete((res, ex) -> {
                if (ex != null) {
                    log.error("No se pudo publicar en Kafka topic={} pedidoId={}: {}", topic, p.getId(), ex.getMessage());
                } else if (res != null) {
                    log.info("Evento Kafka publicado topic={} particion={} offset={} pedidoId={}", topic,
                            res.getRecordMetadata().partition(), res.getRecordMetadata().offset(), p.getId());
                }
            });
        } catch (Exception e) {
            log.error("No se pudo publicar en Kafka topic={} pedidoId={}: {}", topic, p.getId(), e.getMessage());
        }
    }
}
