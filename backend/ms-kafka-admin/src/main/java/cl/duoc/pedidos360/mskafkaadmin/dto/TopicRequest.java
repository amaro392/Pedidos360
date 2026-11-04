package cl.duoc.pedidos360.mskafkaadmin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

/** Datos para crear un topico. partitions y replicationFactor son opcionales (si faltan, usa los valores por defecto del broker). */
public record TopicRequest(
    @NotBlank(message = "El nombre del topico es obligatorio")
    @Size(max = 249, message = "Maximo 249 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Solo letras, numeros, '.', '_' y '-'")
    String name,
    @Min(value = 1, message = "partitions debe ser >= 1") Integer partitions,
    @Min(value = 1, message = "replicationFactor debe ser >= 1") Short replicationFactor,
    Map<String, String> configs) {}
