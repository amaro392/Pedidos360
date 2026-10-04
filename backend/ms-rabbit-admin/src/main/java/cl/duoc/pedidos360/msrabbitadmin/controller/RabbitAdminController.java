package cl.duoc.pedidos360.msrabbitadmin.controller;

import cl.duoc.pedidos360.msrabbitadmin.dto.*;
import cl.duoc.pedidos360.msrabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API REST de administracion de RabbitMQ.
 *
 *   GET    /api/queues                   listar colas
 *   POST   /api/queues                   crear cola
 *   DELETE /api/queues/{name}            eliminar cola
 *   DELETE /api/queues/{name}/messages   purgar mensajes de la cola
 *   GET    /api/exchanges                listar exchanges
 *   POST   /api/exchanges                crear exchange
 *   DELETE /api/exchanges/{name}         eliminar exchange
 *   GET    /api/bindings                 listar bindings
 *   POST   /api/bindings                 crear binding
 *   DELETE /api/bindings                 eliminar binding (datos en el body)
 */
@RestController
@RequestMapping("/api")
public class RabbitAdminController {

    private final RabbitAdminService service;

    public RabbitAdminController(RabbitAdminService service) {
        this.service = service;
    }

    // ---- colas
    @PostMapping("/queues")
    public ResponseEntity<Map<String, String>> crearCola(@Valid @RequestBody QueueRequest r) {
        service.crearCola(r);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Cola creada", "name", r.name()));
    }

    @GetMapping("/queues")
    public List<Map<String, Object>> listarColas() {
        return service.listarColas();
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Void> eliminarCola(@PathVariable String name) {
        service.eliminarCola(name);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/queues/{name}/messages")
    public Map<String, Object> purgarCola(@PathVariable String name) {
        int purgados = service.purgarCola(name);
        return Map.of("message", "Cola purgada", "name", name, "purged", purgados);
    }

    // ---- exchanges
    @PostMapping("/exchanges")
    public ResponseEntity<Map<String, String>> crearExchange(@Valid @RequestBody ExchangeRequest r) {
        service.crearExchange(r);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Exchange creado", "name", r.name()));
    }

    @GetMapping("/exchanges")
    public List<Map<String, Object>> listarExchanges() {
        return service.listarExchanges();
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Void> eliminarExchange(@PathVariable String name) {
        service.eliminarExchange(name);
        return ResponseEntity.noContent().build();
    }

    // ---- bindings
    @PostMapping("/bindings")
    public ResponseEntity<Map<String, String>> crearBinding(@Valid @RequestBody BindingRequest r) {
        service.crearBinding(r);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Binding creado"));
    }

    @GetMapping("/bindings")
    public List<Map<String, Object>> listarBindings() {
        return service.listarBindings();
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> eliminarBinding(@Valid @RequestBody BindingRequest r) {
        service.eliminarBinding(r);
        return ResponseEntity.noContent().build();
    }
}
