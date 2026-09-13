package cl.duoc.pedidos360.ms_notificaciones.controller;

import cl.duoc.pedidos360.ms_notificaciones.entity.Notificacion;
import cl.duoc.pedidos360.ms_notificaciones.service.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    @Autowired
    private NotificacionService service;

    @GetMapping
    public List<Notificacion> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Notificacion> buscar(@PathVariable Long id) {
        Notificacion n = service.buscarPorId(id);
        return n != null ? ResponseEntity.ok(n) : ResponseEntity.notFound().build();
    }

    @GetMapping("/destinatario/{email}")
    public List<Notificacion> porDestinatario(@PathVariable String email) {
        return service.buscarPorDestinatario(email);
    }

    @GetMapping("/pedido/{pedidoId}")
    public List<Notificacion> porPedido(@PathVariable Long pedidoId) {
        return service.buscarPorPedido(pedidoId);
    }

    @PostMapping
    public ResponseEntity<Notificacion> crear(@RequestBody Notificacion notificacion) {
        Notificacion creada = service.enviar(notificacion);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
