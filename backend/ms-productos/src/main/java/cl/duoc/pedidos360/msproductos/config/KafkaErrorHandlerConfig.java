package cl.duoc.pedidos360.msproductos.config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaErrorHandlerConfig {
    private static final Logger log = LoggerFactory.getLogger(KafkaErrorHandlerConfig.class);

    @Bean
    DefaultErrorHandler kafkaErrorHandler() {
        DefaultErrorHandler h = new DefaultErrorHandler(
            (record, ex) -> log.error("Evento descartado tras reintentos topic={} particion={} offset={} causa={}",
                    record.topic(), record.partition(), record.offset(), ex.getMessage()),
            new FixedBackOff(1000L, 3L));
        h.addNotRetryableExceptions(IllegalArgumentException.class);
        return h;
    }
}