package cl.duoc.pedidos360.ms_notificaciones.messaging;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Politica unica de errores para TODOS los consumidores (notificaciones y tickets).
 *
 * <pre>
 *  Error                         Accion                         Resultado
 *  ----------------------------  -----------------------------  -----------------------------
 *  IllegalArgumentException      NACK requeue=false             DLQ de inmediato (no recuperable)
 *  Otra excepcion, 1ra vez       NACK requeue=true              Un reintento
 *  Otra excepcion, ya reentregado NACK requeue=false            DLQ (reintento agotado)
 * </pre>
 *
 * Con requeue=false la cola (declarada con dead-letter-exchange) reenvia el
 * mensaje al DLX y de ahi a la DLQ. Cada decision queda registrada en el log.
 */
@Component
public class ConsumerErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ConsumerErrorHandler.class);

    public void manejar(Channel channel, long tag, boolean redelivered, Exception e,
            String consumidor, Object referencia) throws IOException {
        if (e instanceof IllegalArgumentException) {
            log.error("[{}] NACK->DLQ (no recuperable) ref={} motivo={}", consumidor, referencia, e.getMessage());
            channel.basicNack(tag, false, false);
        } else if (!redelivered) {
            log.warn("[{}] NACK requeue (reintento 1/1) ref={} motivo={}", consumidor, referencia, e.getMessage());
            channel.basicNack(tag, false, true);
        } else {
            log.error("[{}] NACK->DLQ (reintento agotado) ref={} motivo={}", consumidor, referencia, e.getMessage());
            channel.basicNack(tag, false, false);
        }
    }
}
