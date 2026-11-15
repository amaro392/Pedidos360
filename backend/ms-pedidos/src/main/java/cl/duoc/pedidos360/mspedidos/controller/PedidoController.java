package cl.duoc.pedidos360.mspedidos.controller;

import cl.duoc.pedidos360.mspedidos.entity.Pedido;
import cl.duoc.pedidos360.mspedidos.messaging.PedidoEventPublisher;
import cl.duoc.pedidos360.mspedidos.messaging.PedidoKafkaProducer;
import cl.duoc.pedidos360.mspedidos.repository.PedidoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class PedidoController {

    private final PedidoRepository pedidoRepository;
    private final PedidoEventPublisher publisher;
    private final PedidoKafkaProducer kafkaProducer;

    public PedidoController(PedidoRepository pedidoRepository, PedidoEventPublisher publisher,
            PedidoKafkaProducer kafkaProducer) {
        this.pedidoRepository = pedidoRepository;
        this.publisher = publisher;
        this.kafkaProducer = kafkaProducer;
    }

    @GetMapping
    public ResponseEntity<List<Pedido>> listar() {
        return ResponseEntity.ok(pedidoRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Pedido> crear(@RequestBody Pedido pedido) {
        Pedido guardado = pedidoRepository.save(pedido);
        publisher.publicarCreado(guardado);      // RabbitMQ (notificaciones, tickets, documentos)
        kafkaProducer.publicarVenta(guardado);   // Kafka (streaming para analitica)
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }
}