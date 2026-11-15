package cl.duoc.pedidos360.msproductos.messaging;

import cl.duoc.pedidos360.events.PedidoVentaEvent;
import cl.duoc.pedidos360.msproductos.analitica.VentasAnaliticaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consumidor Kafka del topico "pedidos.ventas": alimenta la analitica de productos mas vendidos. */
@Component
public class PedidoVentaConsumer {

    private static final Logger log = LoggerFactory.getLogger(PedidoVentaConsumer.class);
    private final VentasAnaliticaService analitica;

    public PedidoVentaConsumer(VentasAnaliticaService analitica) {
        this.analitica = analitica;
    }

    @KafkaListener(topics = "${app.kafka.topics.pedidos-ventas}", groupId = "${spring.kafka.consumer.group-id}", concurrency = "3")
    public void onPedidoVenta(PedidoVentaEvent ev) {
        analitica.registrar(ev);
        log.info("Venta procesada pedidoId={} items={}", ev.pedidoId(), ev.items() == null ? 0 : ev.items().size());
    }
}
