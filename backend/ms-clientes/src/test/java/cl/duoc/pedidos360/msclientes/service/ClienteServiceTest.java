package cl.duoc.pedidos360.msclientes.service;

import cl.duoc.pedidos360.msclientes.entity.Cliente;
import cl.duoc.pedidos360.msclientes.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository repository;

    @InjectMocks
    private ClienteService service;

    @Test
    void actualizar_retornaNullSiElClienteNoExiste() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        Cliente resultado = service.actualizar(1L, new Cliente());

        assertNull(resultado);
    }

    @Test
    void actualizar_sobrescribeLosDatosDelClienteExistente() {
        Cliente existente = new Cliente();
        existente.setId(1L);
        existente.setNombre("Nombre Viejo");
        existente.setEmail("viejo@duocuc.cl");
        existente.setTelefono("111111");
        existente.setDireccion("Direccion Vieja");

        Cliente datosNuevos = new Cliente();
        datosNuevos.setNombre("Nombre Nuevo");
        datosNuevos.setEmail("nuevo@duocuc.cl");
        datosNuevos.setTelefono("222222");
        datosNuevos.setDireccion("Direccion Nueva");

        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        Cliente resultado = service.actualizar(1L, datosNuevos);

        assertEquals("Nombre Nuevo", resultado.getNombre());
        assertEquals("nuevo@duocuc.cl", resultado.getEmail());
        verify(repository).save(existente);
    }

    @Test
    void eliminar_retornaFalseSiElClienteNoExiste() {
        when(repository.existsById(99L)).thenReturn(false);

        boolean resultado = service.eliminar(99L);

        assertFalse(resultado);
    }

    @Test
    void eliminar_retornaTrueYBorraSiElClienteExiste() {
        when(repository.existsById(1L)).thenReturn(true);

        boolean resultado = service.eliminar(1L);

        assertTrue(resultado);
        verify(repository).deleteById(1L);
    }
}