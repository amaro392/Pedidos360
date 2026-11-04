package cl.duoc.pedidos360.mskafkaadmin.dto;

import jakarta.validation.constraints.Min;

import java.util.List;
import java.util.Map;

/**
 * Modificacion de un topico: aumentar particiones (Kafka no permite reducirlas) y/o cambiar configuraciones.
 * configs = valores a fijar; resetConfigs = nombres de configuraciones que vuelven a su valor por defecto.
 */
public record TopicUpdateRequest(
    @Min(value = 1, message = "partitions debe ser >= 1") Integer partitions,
    Map<String, String> configs,
    List<String> resetConfigs) {}
