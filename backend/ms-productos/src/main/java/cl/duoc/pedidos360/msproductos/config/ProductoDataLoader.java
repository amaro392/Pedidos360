package cl.duoc.pedidos360.msproductos.config;

import cl.duoc.pedidos360.msproductos.entity.Producto;
import cl.duoc.pedidos360.msproductos.repository.ProductoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Carga productos de ejemplo solo si la tabla está vacía,
 * para que el catálogo funcione en cualquier entorno nuevo.
 */
@Configuration
public class ProductoDataLoader {

    @Bean
    CommandLineRunner seedProductos(ProductoRepository repo) {
        return args -> {
            if (repo.count() > 0) {
                return;
            }
            repo.saveAll(List.of(
                producto("Teclado Mecánico RGB", 45990.0, 25, "Periféricos"),
                producto("Mouse Gamer 16000 DPI", 29990.0, 40, "Periféricos"),
                producto("Audífonos 7.1", 39990.0, 30, "Audio"),
                producto("Monitor 24\" 144Hz", 159990.0, 12, "Monitores"),
                producto("Silla Gamer", 189990.0, 8, "Mobiliario")
            ));
        };
    }

    private Producto producto(String nombre, Double precio, Integer stock, String categoria) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setPrecio(precio);
        p.setStock(stock);
        p.setCategoria(categoria);
        p.setImagen("");
        return p;
    }
}
