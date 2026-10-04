package cl.duoc.pedidos360.msrabbitadmin.dto;

import jakarta.validation.constraints.*;

public record QueueRequest(
    @NotBlank(message = "El nombre de la cola es obligatorio")
    @Size(max = 255, message = "Maximo 255 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Solo letras, numeros, '.', '_' y '-'")
    String name,
    Boolean durable,
    @Pattern(regexp = "classic|quorum", message = "type debe ser classic o quorum")
    String type,
    @Size(max = 255, message = "Maximo 255 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9._-]*$", message = "deadLetterExchange: solo letras, numeros, '.', '_' y '-'")
    String deadLetterExchange,
    @Size(max = 255, message = "Maximo 255 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9._-]*$", message = "deadLetterRoutingKey: solo letras, numeros, '.', '_' y '-'")
    String deadLetterRoutingKey,
    @Min(value = 1, message = "messageTtlMs debe ser >= 1") Integer messageTtlMs) {}