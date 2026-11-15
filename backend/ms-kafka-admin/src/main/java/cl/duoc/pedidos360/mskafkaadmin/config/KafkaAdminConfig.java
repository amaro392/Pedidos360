package cl.duoc.pedidos360.mskafkaadmin.config;

import org.apache.kafka.clients.admin.Admin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class KafkaAdminConfig {

    /** Cliente de administracion de Kafka (crear/modificar/eliminar topicos y configuraciones). */
    @Bean(destroyMethod = "close")
    public Admin kafkaAdminClient(KafkaAdmin kafkaAdmin) {
        return Admin.create(kafkaAdmin.getConfigurationProperties());
    }
}
