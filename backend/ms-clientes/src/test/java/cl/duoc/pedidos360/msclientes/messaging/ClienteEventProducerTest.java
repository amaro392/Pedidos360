package cl.duoc.pedidos360.msclientes.messaging;

import cl.duoc.pedidos360.events.ClienteEvent;
import cl.duoc.pedidos360.msclientes.entity.Cliente;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ClienteEventProducerTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> template = mock(KafkaTemplate.class);
    private final ClienteEventProducer producer = new ClienteEventProducer(template, "clientes.registro");

    private Cliente cliente() {
        Cliente c = new Cliente();
        c.setId(3L);
        c.setNombre("Ana Perez");
        c.setEmail("ana@duocuc.cl");
        return c;
    }

    @Test
    void publicarRegistrado_envia_el_evento_al_topico() {
        when(template.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

        producer.publicarRegistrado(cliente());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(template).send(eq("clientes.registro"), eq("3"), captor.capture());
        ClienteEvent ev = (ClienteEvent) captor.getValue();
        assertEquals(3L, ev.clienteId());
        assertEquals("ana@duocuc.cl", ev.email());
        assertEquals("CLIENTE_REGISTRADO", ev.tipo());
    }

    @Test
    void si_kafka_falla_no_se_propaga_la_excepcion() {
        when(template.send(anyString(), anyString(), any())).thenThrow(new IllegalStateException("sin broker"));
        assertDoesNotThrow(() -> producer.publicarRegistrado(cliente()));
    }
}
