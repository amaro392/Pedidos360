package cl.duoc.pedidos360.msrabbitadmin.dto;

import jakarta.validation.constraints.*;

public record BindingRequest(
    @NotBlank(message = "exchange es obligatorio") String exchange,
    @NotBlank(message = "queue es obligatorio") String queue,
    @NotNull(message = "routingKey es obligatorio (puede ser vacio solo en fanout)") String routingKey) {}