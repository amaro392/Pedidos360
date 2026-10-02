package cl.duoc.pedidos360.msrabbitadmin.service;

import cl.duoc.pedidos360.msrabbitadmin.dto.*;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
public class RabbitAdminService {

    private final AmqpAdmin amqpAdmin;
    private final RestClient management;

    public RabbitAdminService(AmqpAdmin amqpAdmin,
            @Value("${app.rabbitmq.management-url}") String managementUrl,
            @Value("${spring.rabbitmq.username}") String user,
            @Value("${spring.rabbitmq.password}") String pass) {
        this.amqpAdmin = amqpAdmin;
        this.management = RestClient.builder().baseUrl(managementUrl)
                .defaultHeaders(h -> h.setBasicAuth(user, pass)).build();
    }

    public void crearCola(QueueRequest r) {
        validarNombre(r.name());
        boolean durable = r.durable() == null || r.durable();
        String type = r.type() == null ? "quorum" : r.type().toLowerCase();
        if (type.equals("quorum") && !durable) {
            throw new IllegalArgumentException("Las colas quorum deben ser durables");
        }
        boolean hasDlx = r.deadLetterExchange() != null && !r.deadLetterExchange().isBlank();
        boolean hasDlk = r.deadLetterRoutingKey() != null && !r.deadLetterRoutingKey().isBlank();
        if (hasDlk && !hasDlx) {
            throw new IllegalArgumentException("deadLetterRoutingKey requiere deadLetterExchange");
        }
        QueueBuilder b = durable ? QueueBuilder.durable(r.name()) : QueueBuilder.nonDurable(r.name());
        b.withArgument("x-queue-type", type);
        if (hasDlx) b.deadLetterExchange(r.deadLetterExchange());
        if (hasDlk) b.deadLetterRoutingKey(r.deadLetterRoutingKey());
        if (r.messageTtlMs() != null) b.ttl(r.messageTtlMs());
        amqpAdmin.declareQueue(b.build());
    }

    public void eliminarCola(String name) {
        validarNombre(name);
        if (amqpAdmin.getQueueInfo(name) == null) {
            throw new NoSuchElementException("La cola '" + name + "' no existe");
        }
        amqpAdmin.deleteQueue(name);
    }

    public void crearExchange(ExchangeRequest r) {
        validarNombre(r.name());
        boolean durable = r.durable() == null || r.durable();
        Exchange ex = switch (r.type().toLowerCase()) {
            case "direct" -> new DirectExchange(r.name(), durable, false);
            case "topic" -> new TopicExchange(r.name(), durable, false);
            case "fanout" -> new FanoutExchange(r.name(), durable, false);
            case "headers" -> new HeadersExchange(r.name(), durable, false);
            default -> throw new IllegalArgumentException("Tipo de exchange invalido");
        };
        amqpAdmin.declareExchange(ex);
    }

    public void eliminarExchange(String name) {
        validarNombre(name);
        amqpAdmin.deleteExchange(name);
    }

    public void crearBinding(BindingRequest r) {
        amqpAdmin.declareBinding(toBinding(r));
    }

    public void eliminarBinding(BindingRequest r) {
        amqpAdmin.removeBinding(toBinding(r));
    }

    public List<Map<String, Object>> listarColas() {
        List<Map<String, Object>> raw = management.get().uri("/api/queues")
                .retrieve().body(new ParameterizedTypeReference<>() {});
        List<Map<String, Object>> out = new ArrayList<>();
        if (raw != null) {
            for (Map<String, Object> q : raw) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", q.get("name"));
                m.put("messages", q.getOrDefault("messages", 0));
                m.put("consumers", q.getOrDefault("consumers", 0));
                m.put("state", q.get("state"));
                out.add(m);
            }
        }
        return out;
    }

    private Binding toBinding(BindingRequest r) {
        return new Binding(r.queue(), Binding.DestinationType.QUEUE, r.exchange(), r.routingKey(), null);
    }

    private void validarNombre(String name) {
        if (name.startsWith("amq.")) {
            throw new IllegalArgumentException("Los nombres que empiezan con 'amq.' estan reservados");
        }
    }
}