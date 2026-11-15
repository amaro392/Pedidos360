package cl.duoc.pedidos360.msproductos.analitica;

import cl.duoc.pedidos360.events.PedidoVentaEvent;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Analitica de ventas en tiempo real: acumula, por producto, las unidades vendidas y los ingresos
 * a medida que llegan eventos del topico Kafka "pedidos.ventas".
 * Los acumulados viven en memoria (se reconstruyen al reiniciar porque el consumidor lee desde el inicio del topico).
 */
@Service
public class VentasAnaliticaService {

    private final Map<String, Acumulado> porProducto = new ConcurrentHashMap<>();
    private final AtomicLong pedidosProcesados = new AtomicLong();

    public void registrar(PedidoVentaEvent ev) {
        if (ev == null || ev.pedidoId() == null) {
            throw new IllegalArgumentException("pedidoId nulo");
        }
        pedidosProcesados.incrementAndGet();
        if (ev.items() == null) {
            return;
        }
        for (PedidoVentaEvent.ItemVenta item : ev.items()) {
            if (item == null || item.nombreProducto() == null || item.cantidad() == null) {
                continue;
            }
            double precio = item.precioUnitario() == null ? 0.0 : item.precioUnitario();
            porProducto.merge(item.nombreProducto(),
                    new Acumulado(item.cantidad(), item.cantidad() * precio), Acumulado::sumar);
        }
    }

    /** Productos ordenados de mas a menos vendidos. */
    public List<ProductoVendido> ranking() {
        return porProducto.entrySet().stream()
                .map(e -> new ProductoVendido(e.getKey(), e.getValue().unidades(), e.getValue().ingresos()))
                .sorted(Comparator.comparingLong(ProductoVendido::unidadesVendidas).reversed()
                        .thenComparing(ProductoVendido::producto))
                .toList();
    }

    public long pedidosProcesados() {
        return pedidosProcesados.get();
    }

    private record Acumulado(long unidades, double ingresos) {
        Acumulado sumar(Acumulado otro) {
            return new Acumulado(unidades + otro.unidades, ingresos + otro.ingresos);
        }
    }

    public record ProductoVendido(String producto, long unidadesVendidas, double ingresos) {}
}
