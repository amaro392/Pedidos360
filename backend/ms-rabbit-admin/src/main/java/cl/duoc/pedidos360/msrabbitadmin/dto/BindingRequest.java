package cl.duoc.pedidos360.msrabbitadmin.dto;

import jakarta.validation.constraints.*;

public record BindingRequest(
    @NotBlank(message = "exchange es obligatorio")
    @Size(max = 255, message = "Maximo 255 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "exchange: solo letras, numeros, '.', '_' y '-'")
    String exchange,
    @NotBlank(message = "queue es obligatorio")
    @Size(max = 255, message = "Maximo 255 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "queue: solo letras, numeros, '.', '_' y '-'")
    String queue,
    @NotNull(message = "routingKey es obligatorio (puede ser vacio solo en fanout)")
    @Size(max = 255, message = "Maximo 255 caracteres")
    String routingKey) {}
