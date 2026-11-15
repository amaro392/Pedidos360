package cl.duoc.pedidos360.mskafkaadmin.service;

import cl.duoc.pedidos360.mskafkaadmin.dto.TopicRequest;
import cl.duoc.pedidos360.mskafkaadmin.dto.TopicUpdateRequest;
import cl.duoc.pedidos360.mskafkaadmin.exception.KafkaAdminException;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

class KafkaAdminServiceTest {

    private final Admin admin = mock(Admin.class);
    private final KafkaAdminService service = new KafkaAdminService(admin, 2);

    private static <T> KafkaFuture<T> fallo(Throwable t) {
        KafkaFutureImpl<T> f = new KafkaFutureImpl<>();
        f.completeExceptionally(t);
        return f;
    }

    private void describeTopicoConParticiones(String nombre, int particiones) {
        Node nodo = new Node(1, "kafka1", 29092);
        List<TopicPartitionInfo> infos = new java.util.ArrayList<>();
        for (int i = 0; i < particiones; i++) {
            infos.add(new TopicPartitionInfo(i, nodo, List.of(nodo), List.of(nodo)));
        }
        DescribeTopicsResult res = mock(DescribeTopicsResult.class);
        when(res.allTopicNames()).thenReturn(
                KafkaFuture.completedFuture(Map.of(nombre, new TopicDescription(nombre, false, infos))));
        when(admin.describeTopics(anyCollection())).thenReturn(res);
    }

    @Test
    void crear_envia_el_NewTopic_con_sus_configuraciones() {
        CreateTopicsResult res = mock(CreateTopicsResult.class);
        when(res.all()).thenReturn(KafkaFuture.<Void>completedFuture(null));
        when(admin.createTopics(anyCollection())).thenReturn(res);

        service.crear(new TopicRequest("demo.topic", 2, (short) 1, Map.of("retention.ms", "60000")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<NewTopic>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(admin).createTopics(captor.capture());
        NewTopic nt = captor.getValue().iterator().next();
        assertEquals("demo.topic", nt.name());
        assertEquals(2, nt.numPartitions());
        assertEquals(1, nt.replicationFactor());
        assertEquals("60000", nt.configs().get("retention.ms"));
    }

    @Test
    void crear_topico_existente_lanza_IllegalState() {
        CreateTopicsResult res = mock(CreateTopicsResult.class);
        when(res.all()).thenReturn(fallo(new TopicExistsException("ya existe")));
        when(admin.createTopics(anyCollection())).thenReturn(res);

        assertThrows(IllegalStateException.class, () -> service.crear(new TopicRequest("demo", 1, (short) 1, null)));
    }

    @Test
    void crear_con_nombre_invalido_no_llama_a_kafka() {
        assertThrows(IllegalArgumentException.class, () -> service.crear(new TopicRequest("mi topico", 1, (short) 1, null)));
        verifyNoInteractions(admin);
    }

    @Test
    void eliminar_topico_inexistente_lanza_NoSuchElement() {
        DeleteTopicsResult res = mock(DeleteTopicsResult.class);
        when(res.all()).thenReturn(fallo(new UnknownTopicOrPartitionException("no existe")));
        when(admin.deleteTopics(anyCollection())).thenReturn(res);

        assertThrows(NoSuchElementException.class, () -> service.eliminar("nope"));
    }

    @Test
    void eliminar_topico_interno_se_rechaza_sin_llamar_a_kafka() {
        assertThrows(IllegalArgumentException.class, () -> service.eliminar("__consumer_offsets"));
        verifyNoInteractions(admin);
    }

    @Test
    void actualizar_sin_cambios_es_invalido() {
        assertThrows(IllegalArgumentException.class, () -> service.actualizar("demo", new TopicUpdateRequest(null, null, null)));
        verifyNoInteractions(admin);
    }

    @Test
    void actualizar_no_permite_reducir_particiones() {
        describeTopicoConParticiones("demo", 3);
        assertThrows(IllegalArgumentException.class, () -> service.actualizar("demo", new TopicUpdateRequest(2, null, null)));
        verify(admin, never()).createPartitions(anyMap());
    }

    @Test
    void actualizar_aumenta_particiones() {
        describeTopicoConParticiones("demo", 2);
        CreatePartitionsResult res = mock(CreatePartitionsResult.class);
        when(res.all()).thenReturn(KafkaFuture.<Void>completedFuture(null));
        when(admin.createPartitions(anyMap())).thenReturn(res);

        service.actualizar("demo", new TopicUpdateRequest(4, null, null));

        verify(admin).createPartitions(anyMap());
    }

    @Test
    void si_kafka_no_responde_lanza_KafkaAdminException() {
        CreateTopicsResult res = mock(CreateTopicsResult.class);
        when(res.all()).thenReturn(new KafkaFutureImpl<>()); // nunca se completa -> timeout
        when(admin.createTopics(anyCollection())).thenReturn(res);

        KafkaAdminService corto = new KafkaAdminService(admin, 1);
        assertThrows(KafkaAdminException.class, () -> corto.crear(new TopicRequest("demo", 1, (short) 1, null)));
    }
}
