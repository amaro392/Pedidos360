package cl.duoc.pedidos360.mspedidos.messaging;

import cl.duoc.pedidos360.events.PedidoEvent;
import cl.duoc.pedidos360.mspedidos.entity.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PedidoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String keyCreado;
    private final String keyCancelado;

    public PedidoEventPublisher(RabbitTemplate rabbitTemplate,
            @Value("${app.rabbitmq.exchanges.pedidos}") String exchange,
            @Value("${app.rabbitmq.routing-keys.pedido-creado}") String keyCreado,
            @Value("${app.rabbitmq.routing-keys.pedido-cancelado}") String keyCancelado) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.keyCreado = keyCreado;
        this.keyCancelado = keyCancelado;
    }

    public void publicarCreado(Pedido p) { publicar(keyCreado, p); }

    public void publicarCancelado(Pedido p) { publicar(keyCancelado, p); }

    // Un fallo de RabbitMQ NO debe romper la creacion del pedido.
    private void publicar(String routingKey, Pedido p) {
        try {
            PedidoEvent ev = new PedidoEvent(p.getId(), p.getClienteEmail(), p.getTotal(),
                    p.getEstado(), LocalDateTime.now().toString());
            rabbitTemplate.convertAndSend(exchange, routingKey, ev);
            log.info("Evento publicado exchange={} key={} pedidoId={}", exchange, routingKey, p.getId());
        } catch (AmqpException e) {
            log.error("No se pudo publicar evento key={} pedidoId={}: {}", routingKey, p.getId(), e.getMessage());
        }
    }
}