package cl.duoc.pedidos360.mskafkaadmin.service;

import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import java.util.HashMap;
import cl.duoc.pedidos360.mskafkaadmin.dto.ConfigUpdateRequest;
import cl.duoc.pedidos360.mskafkaadmin.dto.TopicRequest;
import cl.duoc.pedidos360.mskafkaadmin.dto.TopicUpdateRequest;
import cl.duoc.pedidos360.mskafkaadmin.exception.KafkaAdminException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;


/**
 * Administracion de Kafka: cluster, topicos y sus configuraciones.
 * Traduce los errores de Kafka a excepciones que GlobalExceptionHandler convierte en 400 / 404 / 409 / 502.
 */
@Service
public class KafkaAdminService {

    private static final Pattern NOMBRE_VALIDO = Pattern.compile("^[A-Za-z0-9._-]{1,249}$");

    private final Admin admin;
    private final long timeoutSeconds;

    public KafkaAdminService(Admin admin, @Value("${app.kafka.admin-timeout-seconds:10}") long timeoutSeconds) {
        this.admin = admin;
        this.timeoutSeconds = timeoutSeconds;
    }

    // ---------------------------------------------------------------- cluster
    public Map<String, Object> cluster() {
        DescribeClusterResult r = admin.describeCluster();
        List<Map<String, Object>> brokers = new ArrayList<>();
        for (Node n : esperar(r.nodes())) {
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("id", n.id());
            b.put("host", n.host());
            b.put("port", n.port());
            brokers.add(b);
        }
        brokers.sort(Comparator.comparingInt(b -> (Integer) b.get("id")));
        Node controller = esperar(r.controller());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("clusterId", esperar(r.clusterId()));
        out.put("controllerId", controller == null ? null : controller.id());
        out.put("brokers", brokers);
        return out;
    }

    // ---------------------------------------------------------------- topicos
    public List<Map<String, Object>> listarTopicos() {
        Collection<String> nombres = esperar(admin.listTopics().names());
        if (nombres.isEmpty()) {
            return List.of();
        }
        Map<String, TopicDescription> descripciones = esperar(admin.describeTopics(nombres).allTopicNames());
        List<Map<String, Object>> out = new ArrayList<>();
        descripciones.values().stream().sorted(Comparator.comparing(TopicDescription::name)).forEach(d -> {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("name", d.name());
            t.put("partitions", d.partitions().size());
            t.put("replicationFactor", factorReplicacion(d));
            t.put("internal", d.isInternal());
            out.add(t);
        });
        return out;
    }

    public Map<String, Object> describirTopico(String name) {
        validarNombre(name);
        TopicDescription d = descripcion(name);
        List<Map<String, Object>> particiones = new ArrayList<>();
        for (TopicPartitionInfo p : d.partitions()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("partition", p.partition());
            m.put("leader", p.leader() == null ? null : p.leader().id());
            m.put("replicas", p.replicas().stream().map(Node::id).toList());
            m.put("isr", p.isr().stream().map(Node::id).toList());
            particiones.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", d.name());
        out.put("internal", d.isInternal());
        out.put("partitions", particiones);
        out.put("configs", configuraciones(name));
        return out;
    }

    public void crear(TopicRequest r) {
        validarNombre(r.name());
        NewTopic topic = new NewTopic(r.name(), Optional.ofNullable(r.partitions()),
                Optional.ofNullable(r.replicationFactor()));
        if (r.configs() != null && !r.configs().isEmpty()) {
            topic.configs(r.configs());
        }
        esperar(admin.createTopics(List.of(topic)).all());
    }

    /** Aumenta particiones y/o cambia configuraciones. Debe venir al menos una de las dos cosas. */
    public void actualizar(String name, TopicUpdateRequest r) {
        validarNombre(name);
        boolean hayConfigs = hayCambios(r.configs(), r.resetConfigs());
        if (r.partitions() == null && !hayConfigs) {
            throw new IllegalArgumentException("Indique partitions, configs o resetConfigs");
        }
        if (r.partitions() != null) {
            int actuales = descripcion(name).partitions().size();
            if (r.partitions() <= actuales) {
                throw new IllegalArgumentException("Kafka solo permite aumentar particiones (actuales: " + actuales + ")");
            }
            esperar(admin.createPartitions(Map.of(name, NewPartitions.increaseTo(r.partitions()))).all());
        }
        if (hayConfigs) {
            alterarConfiguraciones(name, r.configs(), r.resetConfigs());
        }
    }

    public void eliminar(String name) {
        validarNombre(name);
        if (name.startsWith("__")) {
            throw new IllegalArgumentException("No se pueden eliminar topicos internos de Kafka");
        }
        esperar(admin.deleteTopics(List.of(name)).all());
    }

    // ---------------------------------------------------------------- configuraciones
    public List<Map<String, Object>> configuraciones(String name) {
        validarNombre(name);
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, name);
        Config config = esperar(admin.describeConfigs(List.of(recurso)).all()).get(recurso);
        List<Map<String, Object>> out = new ArrayList<>();
        if (config == null) {
            return out;
        }
        config.entries().stream().sorted(Comparator.comparing(ConfigEntry::name)).forEach(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", e.name());
            m.put("value", e.isSensitive() ? null : e.value());
            m.put("source", e.source().name());
            m.put("readOnly", e.isReadOnly());
            out.add(m);
        });
        return out;
    }

    public void actualizarConfiguraciones(String name, ConfigUpdateRequest r) {
        validarNombre(name);
        if (!hayCambios(r.configs(), r.resetConfigs())) {
            throw new IllegalArgumentException("Indique configs o resetConfigs");
        }
        alterarConfiguraciones(name, r.configs(), r.resetConfigs());
    }

    // ---------------------------------------------------------------- internos
    private void alterarConfiguraciones(String name, Map<String, String> fijar, List<String> restablecer) {
        List<AlterConfigOp> ops = new ArrayList<>();
        if (fijar != null) {
            fijar.forEach((k, v) -> ops.add(new AlterConfigOp(new ConfigEntry(k, v), AlterConfigOp.OpType.SET)));
        }
        if (restablecer != null) {
            restablecer.forEach(k -> ops.add(new AlterConfigOp(new ConfigEntry(k, null), AlterConfigOp.OpType.DELETE)));
        }
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, name);
        esperar(admin.incrementalAlterConfigs(Map.of(recurso, ops)).all());
    }

    private TopicDescription descripcion(String name) {
        Map<String, TopicDescription> m = esperar(admin.describeTopics(List.of(name)).allTopicNames());
        TopicDescription d = m.get(name);
        if (d == null) {
            throw new NoSuchElementException("El topico '" + name + "' no existe");
        }
        return d;
    }

    private static int factorReplicacion(TopicDescription d) {
        return d.partitions().isEmpty() ? 0 : d.partitions().get(0).replicas().size();
    }

    private static boolean hayCambios(Map<String, String> configs, List<String> reset) {
        return (configs != null && !configs.isEmpty()) || (reset != null && !reset.isEmpty());
    }

    private static void validarNombre(String name) {
        if (name == null || !NOMBRE_VALIDO.matcher(name).matches()) {
            throw new IllegalArgumentException("Nombre de topico invalido (solo letras, numeros, '.', '_' y '-')");
        }
    }

    private <T> T esperar(KafkaFuture<T> futuro) {
        try {
            return futuro.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw traducir(e.getCause() != null ? e.getCause() : e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaAdminException("Operacion interrumpida", e);
        } catch (TimeoutException e) {
            throw new KafkaAdminException("Kafka no respondio en " + timeoutSeconds + " segundos", e);
        }
    }

    private RuntimeException traducir(Throwable causa) {
        if (causa instanceof UnknownTopicOrPartitionException) {
            return new NoSuchElementException("El topico no existe");
        }
        if (causa instanceof TopicExistsException) {
            return new IllegalStateException("El topico ya existe");
        }
        if (causa instanceof InvalidTopicException || causa instanceof InvalidPartitionsException
                || causa instanceof InvalidReplicationFactorException || causa instanceof InvalidConfigurationException
                || causa instanceof InvalidRequestException) {
            return new IllegalArgumentException(causa.getMessage());
        }
        return new KafkaAdminException("Error comunicando con Kafka: " + causa.getMessage(), causa);
    }

    //metodos para kafka-admin, 

        public List<Map<String, Object>> listarGrupos() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (ConsumerGroupListing g : esperar(admin.listConsumerGroups().all())) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("groupId", g.groupId());
            m.put("estado", g.state().map(Object::toString).orElse("UNKNOWN"));
            out.add(m);
        }
        return out;
    }

    public Map<String, Object> describirGrupo(String id) {
        ConsumerGroupDescription g = esperar(admin.describeConsumerGroups(List.of(id)).all()).get(id);
        if (g == null || "DEAD".equalsIgnoreCase(String.valueOf(g.state()))) {
            throw new NoSuchElementException("El consumer group '" + id + "' no existe");
        }
        List<Map<String, Object>> miembros = new ArrayList<>();
        for (MemberDescription md : g.members()) {
            Map<String, Object> mm = new LinkedHashMap<>();
            mm.put("clientId", md.clientId());
            mm.put("host", md.host());
            mm.put("particiones", md.assignment().topicPartitions().stream()
                    .map(TopicPartition::toString).sorted().toList());
            miembros.add(mm);
        }
        Map<TopicPartition, OffsetAndMetadata> committed =
                esperar(admin.listConsumerGroupOffsets(id).partitionsToOffsetAndMetadata());
        Map<TopicPartition, OffsetSpec> latest = new HashMap<>();
        committed.keySet().forEach(tp -> latest.put(tp, OffsetSpec.latest()));
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> fin =
                latest.isEmpty() ? Map.of() : esperar(admin.listOffsets(latest).all());
        List<Map<String, Object>> offsets = new ArrayList<>();
        long lagTotal = 0;
        for (Map.Entry<TopicPartition, OffsetAndMetadata> e : committed.entrySet()) {
            long end = fin.get(e.getKey()).offset();
            long lag = Math.max(0, end - e.getValue().offset());
            lagTotal += lag;
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("particion", e.getKey().toString());
            x.put("offsetActual", e.getValue().offset());
            x.put("offsetFinal", end);
            x.put("lag", lag);
            offsets.add(x);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("groupId", id);
        out.put("estado", String.valueOf(g.state()));
        out.put("miembros", miembros);
        out.put("lagTotal", lagTotal);
        out.put("offsets", offsets);
        return out;
    }

    public void eliminarGrupo(String id) {
        esperar(admin.deleteConsumerGroups(List.of(id)).all());
    }
}
