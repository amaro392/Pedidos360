package cl.duoc.pedidos360.msproductos.service;

import cl.duoc.pedidos360.msproductos.entity.Producto;
import cl.duoc.pedidos360.msproductos.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProductoService {
    @Autowired
    private ProductoRepository repository;

    public List<Producto> listar() { return repository.findAll(); }
    public Producto guardar(Producto p) { return repository.save(p); }
    public Producto buscarPorId(Long id) { return repository.findById(id).orElse(null); }
    public void eliminar(Long id) { repository.deleteById(id); }
}