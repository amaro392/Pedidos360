package cl.duoc.pedidos360.msproductos.controller;

import cl.duoc.pedidos360.msproductos.analitica.VentasAnaliticaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Expone la analitica calculada a partir del streaming de Kafka. Requiere JWT como el resto de la API. */
@RestController
@RequestMapping("/api/analitica")
public class AnaliticaController {

    private final VentasAnaliticaService service;

    public AnaliticaController(VentasAnaliticaService service) {
        this.service = service;
    }

    @GetMapping("/ventas")
    public Map<String, Object> ventas() {
        return Map.of("pedidosProcesados", service.pedidosProcesados(), "ranking", service.ranking());
    }
}
