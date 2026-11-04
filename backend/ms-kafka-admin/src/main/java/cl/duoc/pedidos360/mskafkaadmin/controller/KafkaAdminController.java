package cl.duoc.pedidos360.mskafkaadmin.controller;

import cl.duoc.pedidos360.mskafkaadmin.dto.ConfigUpdateRequest;
import cl.duoc.pedidos360.mskafkaadmin.dto.TopicRequest;
import cl.duoc.pedidos360.mskafkaadmin.dto.TopicUpdateRequest;
import cl.duoc.pedidos360.mskafkaadmin.service.KafkaAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API REST de administracion de Kafka.
 *
 *   GET    /api/cluster                  brokers y controlador del cluster
 *   GET    /api/topics                   listar topicos
 *   GET    /api/topics/{name}            detalle del topico (particiones, lider, ISR, configuraciones)
 *   POST   /api/topics                   crear topico (con configuraciones opcionales)
 *   PUT    /api/topics/{name}            modificar topico (aumentar particiones y/o configuraciones)
 *   DELETE /api/topics/{name}            eliminar topico
 *   GET    /api/topics/{name}/configs    listar configuraciones del topico
 *   PUT    /api/topics/{name}/configs    modificar / restablecer configuraciones del topico
 */
@RestController
@RequestMapping("/api")
public class KafkaAdminController {

    private final KafkaAdminService service;

    public KafkaAdminController(KafkaAdminService service) {
        this.service = service;
    }

    @GetMapping("/cluster")
    public Map<String, Object> cluster() {
        return service.cluster();
    }

    // ---- topicos
    @GetMapping("/topics")
    public List<Map<String, Object>> listarTopicos() {
        return service.listarTopicos();
    }

    @GetMapping("/topics/{name}")
    public Map<String, Object> describirTopico(@PathVariable String name) {
        return service.describirTopico(name);
    }

    @PostMapping("/topics")
    public ResponseEntity<Map<String, String>> crearTopico(@Valid @RequestBody TopicRequest r) {
        service.crear(r);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Topico creado", "name", r.name()));
    }

    @PutMapping("/topics/{name}")
    public Map<String, String> actualizarTopico(@PathVariable String name, @Valid @RequestBody TopicUpdateRequest r) {
        service.actualizar(name, r);
        return Map.of("message", "Topico actualizado", "name", name);
    }

    @DeleteMapping("/topics/{name}")
    public ResponseEntity<Void> eliminarTopico(@PathVariable String name) {
        service.eliminar(name);
        return ResponseEntity.noContent().build();
    }

    // ---- configuraciones
    @GetMapping("/topics/{name}/configs")
    public List<Map<String, Object>> configuraciones(@PathVariable String name) {
        return service.configuraciones(name);
    }

    @PutMapping("/topics/{name}/configs")
    public Map<String, String> actualizarConfiguraciones(@PathVariable String name,
            @Valid @RequestBody ConfigUpdateRequest r) {
        service.actualizarConfiguraciones(name, r);
        return Map.of("message", "Configuracion actualizada", "name", name);
    }
}
