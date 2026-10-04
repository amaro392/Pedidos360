package cl.duoc.pedidos360.msrabbitadmin.service;

import cl.duoc.pedidos360.msrabbitadmin.dto.*;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.*;

/**
 * Unico punto que habla con RabbitMQ: los controladores solo llaman a estos
 * metodos. Crear/eliminar/purgar usa AmqpAdmin (protocolo AMQP); listar usa la
 * API HTTP de management, porque AMQP no permite enumerar recursos.
 */
@Service
public class RabbitAdminService {

    private final AmqpAdmin amqpAdmin;
    private final RestClient management;

        public RabbitAdminService(AmqpAdmin amqpAdmin,
            @Qualifier("rabbitManagement") RestClient management) {
        this.amqpAdmin = amqpAdmin;
        this.management = management;
    }

    // ---------------------------------------------------------------- colas

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
        exigirCola(name);
        amqpAdmin.deleteQueue(name);
    }

    /** Vacia la cola sin eliminarla. Devuelve cuantos mensajes se purgaron. */
    public int purgarCola(String name) {
        validarNombre(name);
        exigirCola(name);
        return amqpAdmin.purgeQueue(name);
    }

    public List<Map<String, Object>> listarColas() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> q : consultar("/api/queues")) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", q.get("name"));
            m.put("messages", q.getOrDefault("messages", 0));
            m.put("consumers", q.getOrDefault("consumers", 0));
            m.put("state", q.get("state"));
            out.add(m);
        }
        return out;
    }

    // ------------------------------------------------------------ exchanges

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
        boolean existe = listarExchanges().stream().anyMatch(e -> name.equals(e.get("name")));
        if (!existe) {
            throw new NoSuchElementException("El exchange '" + name + "' no existe");
        }
        amqpAdmin.deleteExchange(name);
    }

    public List<Map<String, Object>> listarExchanges() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> e : consultar("/api/exchanges")) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", e.get("name"));
            m.put("type", e.get("type"));
            m.put("durable", e.get("durable"));
            out.add(m);
        }
        return out;
    }

    // ------------------------------------------------------------- bindings

    public void crearBinding(BindingRequest r) {
        amqpAdmin.declareBinding(toBinding(r));
    }

    public void eliminarBinding(BindingRequest r) {
        amqpAdmin.removeBinding(toBinding(r));
    }

    public List<Map<String, Object>> listarBindings() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> b : consultar("/api/bindings")) {
            // El binding implicito al default exchange ("") no aporta informacion
            if (b.get("source") == null || b.get("source").toString().isEmpty()) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("exchange", b.get("source"));
            m.put("destination", b.get("destination"));
            m.put("destinationType", b.get("destination_type"));
            m.put("routingKey", b.get("routing_key"));
            out.add(m);
        }
        return out;
    }

    // ------------------------------------------------------------- privados

    private List<Map<String, Object>> consultar(String path) {
        List<Map<String, Object>> raw = management.get().uri(path)
                .retrieve().body(new ParameterizedTypeReference<>() {});
        return raw == null ? List.of() : raw;
    }

    private void exigirCola(String name) {
        if (amqpAdmin.getQueueInfo(name) == null) {
            throw new NoSuchElementException("La cola '" + name + "' no existe");
        }
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
