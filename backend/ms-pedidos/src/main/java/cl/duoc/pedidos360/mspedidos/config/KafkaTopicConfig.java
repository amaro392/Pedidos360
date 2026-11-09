package cl.duoc.pedidos360.mspedidos.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic pedidosVentasTopic(@Value("${app.kafka.topics.pedidos-ventas}") String name) {
        return TopicBuilder.name(name)
                .partitions(3).replicas(2)
                .config(TopicConfig.RETENTION_MS_CONFIG, "604800000")      // 7 días
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "1")
                .build();
    }
}