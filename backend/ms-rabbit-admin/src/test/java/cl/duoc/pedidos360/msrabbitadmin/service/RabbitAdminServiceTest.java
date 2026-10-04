package cl.duoc.pedidos360.msrabbitadmin.service;

import cl.duoc.pedidos360.msrabbitadmin.dto.BindingRequest;
import cl.duoc.pedidos360.msrabbitadmin.dto.ExchangeRequest;
import cl.duoc.pedidos360.msrabbitadmin.dto.QueueRequest;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.web.client.RestClient;


import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RabbitAdminServiceTest {

    private final AmqpAdmin admin = mock(AmqpAdmin.class);
       private final RabbitAdminService service =
            new RabbitAdminService(admin, mock(RestClient.class));

    @Test
    void crearCola_declara_la_cola() {
        service.crearCola(new QueueRequest("demo.queue", true, "quorum", null, null, null));
        verify(admin).declareQueue(any(Queue.class));
    }

    @Test
    void nombre_reservado_amq_es_rechazado() {
        assertThrows(IllegalArgumentException.class,
                () -> service.crearCola(new QueueRequest("amq.algo", true, "quorum", null, null, null)));
    }

    @Test
    void cola_quorum_no_durable_es_rechazada() {
        assertThrows(IllegalArgumentException.class,
                () -> service.crearCola(new QueueRequest("demo", false, "quorum", null, null, null)));
        verify(admin, never()).declareQueue(any());
    }

    @Test
    void routingKeyDeadLetter_sin_exchange_es_rechazada() {
        assertThrows(IllegalArgumentException.class,
                () -> service.crearCola(new QueueRequest("demo", true, "quorum", null, "dead", null)));
    }

    @Test
    void eliminarCola_inexistente_lanza_NoSuchElement() {
        when(admin.getQueueInfo("nope")).thenReturn(null);
        assertThrows(NoSuchElementException.class, () -> service.eliminarCola("nope"));
        verify(admin, never()).deleteQueue(any());
    }

    @Test
    void purgarCola_inexistente_lanza_NoSuchElement() {
        when(admin.getQueueInfo("nope")).thenReturn(null);
        assertThrows(NoSuchElementException.class, () -> service.purgarCola("nope"));
    }

    @Test
    void crearExchange_declara_el_exchange() {
        service.crearExchange(new ExchangeRequest("demo.exchange", "topic", true));
        verify(admin).declareExchange(any(Exchange.class));
    }

    @Test
    void crearExchange_tipo_invalido_es_rechazado() {
        assertThrows(IllegalArgumentException.class,
                () -> service.crearExchange(new ExchangeRequest("demo", "raro", true)));
    }

    @Test
    void crearBinding_y_eliminarBinding_delegan_en_amqpAdmin() {
        BindingRequest r = new BindingRequest("demo.exchange", "demo.queue", "demo.key");
        service.crearBinding(r);
        service.eliminarBinding(r);
        verify(admin).declareBinding(any(Binding.class));
        verify(admin).removeBinding(any(Binding.class));
    }
}
