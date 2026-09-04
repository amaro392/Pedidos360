package cl.duoc.pedidos360.msproductos.controller;

import cl.duoc.pedidos360.msproductos.entity.Producto;
import cl.duoc.pedidos360.msproductos.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {
    @Autowired
    private ProductoService service;

    @GetMapping
    public List<Producto> listar() { return service.listar(); }

    @GetMapping("/{id}")
    public Producto buscar(@PathVariable Long id) { return service.buscarPorId(id); }

    @PostMapping
    public Producto crear(@RequestBody Producto p) { return service.guardar(p); }

    @PutMapping("/{id}")
    public Producto actualizar(@PathVariable Long id, @RequestBody Producto p) {
        p.setId(id);
        return service.guardar(p);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) { service.eliminar(id); }
}