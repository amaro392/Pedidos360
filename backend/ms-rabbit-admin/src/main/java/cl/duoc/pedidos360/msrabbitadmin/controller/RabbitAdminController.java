package cl.duoc.pedidos360.msrabbitadmin.controller;

import cl.duoc.pedidos360.msrabbitadmin.dto.*;
import cl.duoc.pedidos360.msrabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RabbitAdminController {

    private final RabbitAdminService service;

    public RabbitAdminController(RabbitAdminService service) {
        this.service = service;
    }

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

    @PostMapping("/exchanges")
    public ResponseEntity<Map<String, String>> crearExchange(@Valid @RequestBody ExchangeRequest r) {
        service.crearExchange(r);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Exchange creado", "name", r.name()));
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Void> eliminarExchange(@PathVariable String name) {
        service.eliminarExchange(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bindings")
    public ResponseEntity<Map<String, String>> crearBinding(@Valid @RequestBody BindingRequest r) {
        service.crearBinding(r);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Binding creado"));
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> eliminarBinding(@Valid @RequestBody BindingRequest r) {
        service.eliminarBinding(r);
        return ResponseEntity.noContent().build();
    }
}