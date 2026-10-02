package cl.duoc.pedidos360.msrabbitadmin.service;

import cl.duoc.pedidos360.msrabbitadmin.dto.QueueRequest;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitAdminServiceTest {

    private final AmqpAdmin admin = mock(AmqpAdmin.class);
    private final RabbitAdminService service =
            new RabbitAdminService(admin, "http://localhost:15672", "u", "p");

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
}