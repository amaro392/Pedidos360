package cl.duoc.pedidos360.msrabbitadmin.dto;

import jakarta.validation.constraints.*;

public record ExchangeRequest(
    @NotBlank(message = "El nombre del exchange es obligatorio")
    @Size(max = 255, message = "Maximo 255 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Solo letras, numeros, '.', '_' y '-'")
    String name,
    @NotBlank(message = "type es obligatorio")
    @Pattern(regexp = "direct|topic|fanout|headers", message = "type debe ser direct, topic, fanout o headers")
    String type,
    Boolean durable) {}