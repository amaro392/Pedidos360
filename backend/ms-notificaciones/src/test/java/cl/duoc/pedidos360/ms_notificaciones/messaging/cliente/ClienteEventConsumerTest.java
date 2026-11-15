package cl.duoc.pedidos360.ms_notificaciones.messaging.cliente;

import cl.duoc.pedidos360.events.ClienteEvent;
import cl.duoc.pedidos360.ms_notificaciones.entity.Notificacion;
import cl.duoc.pedidos360.ms_notificaciones.service.NotificacionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClienteEventConsumerTest {

    private final NotificacionService service = mock(NotificacionService.class);
    private final ClienteEventConsumer consumer = new ClienteEventConsumer(service);

    @Test
    void clienteRegistrado_genera_notificacion_de_bienvenida() {
        consumer.onClienteRegistrado(new ClienteEvent(1L, "Ana", "ana@duocuc.cl", "CLIENTE_REGISTRADO", "x"));

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(service).enviar(captor.capture());
        assertEquals("ana@duocuc.cl", captor.getValue().getDestinatarioEmail());
        assertEquals("CLIENTE_REGISTRADO", captor.getValue().getTipo());
    }

    @Test
    void emailVacio_lanza_excepcion_y_no_guarda() {
        assertThrows(IllegalArgumentException.class,
                () -> consumer.onClienteRegistrado(new ClienteEvent(1L, "Ana", " ", "CLIENTE_REGISTRADO", "x")));
        verify(service, never()).enviar(any(Notificacion.class));
    }
}
