package cl.duoc.pedidos360.mskafkaadmin.exception;

/** Error al comunicarse con Kafka (broker caido, timeout, etc). Se responde como 502. */
public class KafkaAdminException extends RuntimeException {
    public KafkaAdminException(String message, Throwable cause) {
        super(message, cause);
    }
}
