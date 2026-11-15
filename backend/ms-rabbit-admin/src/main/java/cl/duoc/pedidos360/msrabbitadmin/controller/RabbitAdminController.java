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
 *   GET    /api/v1/queues                   listar colas
 *   POST   /api/v1/queues                   crear cola
 *   DELETE /api/v1/queues/{name}            eliminar cola
 *   DELETE /api/v1/queues/{name}/messages   purgar mensajes de la cola
 *   GET    /api/v1/exchanges                listar exchanges
 *   POST   /api/v1/exchanges                crear exchange
 *   DELETE /api/v1/exchanges/{name}         eliminar exchange
 *   GET    /api/v1/bindings                 listar bindings
 *   POST   /api/v1/bindings                 crear binding
 *   DELETE /api/v1/bindings                 eliminar binding (datos en el body)
 */
@RestController
@RequestMapping("/api/v1")
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

    @PutMapping("/queues/{name}")
    public Map<String, String> actualizarCola(@PathVariable String name, @Valid @RequestBody QueueRequest r) {
        service.actualizarCola(name, r);
        return Map.of("message", "Cola actualizada", "name", name);
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
