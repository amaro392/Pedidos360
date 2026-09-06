package cl.duoc.pedidos360.ms_notificaciones.service;

import cl.duoc.pedidos360.ms_notificaciones.entity.Notificacion;
import cl.duoc.pedidos360.ms_notificaciones.repository.NotificacionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock
    private NotificacionRepository repository;

    @InjectMocks
    private NotificacionService service;

    @Test
    void enviar_asignaEstadoEnviadaPorDefecto() {
        Notificacion n = new Notificacion();
        n.setDestinatarioEmail("cliente@duocuc.cl");
        n.setAsunto("Pedido recibido");
        n.setMensaje("Tu pedido fue recibido.");
        n.setTipo("PEDIDO_CREADO");

        when(repository.save(n)).thenReturn(n);

        Notificacion resultado = service.enviar(n);

        assertEquals("ENVIADA", resultado.getEstado());
        verify(repository).save(n);
    }

    @Test
    void buscarPorId_retornaNullSiNoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        Notificacion resultado = service.buscarPorId(99L);

        assertEquals(null, resultado);
    }
}
