package cl.duoc.pedidos360.msclientes.messaging;

import cl.duoc.pedidos360.events.ClienteEvent;
import cl.duoc.pedidos360.msclientes.entity.Cliente;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Productor Kafka: publica en "clientes.registro" cada cliente nuevo. Un fallo de Kafka no interrumpe el registro. */
@Component
public class ClienteEventProducer {

    private static final Logger log = LoggerFactory.getLogger(ClienteEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public ClienteEventProducer(KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.clientes-registro}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publicarRegistrado(Cliente c) {
        try {
            ClienteEvent ev = new ClienteEvent(c.getId(), c.getNombre(), c.getEmail(),
                    "CLIENTE_REGISTRADO", LocalDateTime.now().toString());
            kafkaTemplate.send(topic, String.valueOf(c.getId()), ev).whenComplete((res, ex) -> {
                if (ex != null) {
                    log.error("No se pudo publicar en Kafka topic={} clienteId={}: {}", topic, c.getId(), ex.getMessage());
                } else if (res != null) {
                    log.info("Evento Kafka publicado topic={} particion={} offset={} clienteId={}", topic,
                            res.getRecordMetadata().partition(), res.getRecordMetadata().offset(), c.getId());
                }
            });
        } catch (Exception e) {
            log.error("No se pudo publicar en Kafka topic={} clienteId={}: {}", topic, c.getId(), e.getMessage());
        }
    }
}
