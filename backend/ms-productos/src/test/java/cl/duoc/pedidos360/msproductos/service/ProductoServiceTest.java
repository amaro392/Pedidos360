package cl.duoc.pedidos360.msproductos.service;

import cl.duoc.pedidos360.msproductos.entity.Producto;
import cl.duoc.pedidos360.msproductos.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository repository;

    @InjectMocks
    private ProductoService service;

    @Test
    void guardar_retornaElProductoGuardado() {
        Producto p = new Producto();
        p.setNombre("Mouse Inalambrico");
        p.setPrecio(9990.0);
        p.setStock(15);
        p.setCategoria("Accesorios");

        when(repository.save(p)).thenReturn(p);

        Producto resultado = service.guardar(p);

        assertEquals("Mouse Inalambrico", resultado.getNombre());
        verify(repository).save(p);
    }

    @Test
    void buscarPorId_retornaNullSiNoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        Producto resultado = service.buscarPorId(99L);

        assertNull(resultado);
    }

    @Test
    void listar_retornaTodosLosProductos() {
        Producto p1 = new Producto();
        p1.setNombre("Teclado");
        Producto p2 = new Producto();
        p2.setNombre("Monitor");

        when(repository.findAll()).thenReturn(List.of(p1, p2));

        List<Producto> resultado = service.listar();

        assertEquals(2, resultado.size());
        verify(repository).findAll();
    }
}