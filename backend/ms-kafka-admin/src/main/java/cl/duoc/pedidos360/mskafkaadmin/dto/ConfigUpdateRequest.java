package cl.duoc.pedidos360.mskafkaadmin.dto;

import java.util.List;
import java.util.Map;

/** Cambio de configuraciones de un topico (sin tocar particiones). */
public record ConfigUpdateRequest(Map<String, String> configs, List<String> resetConfigs) {}
