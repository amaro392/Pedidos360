package cl.duoc.pedidos360.msclientes.service;

import cl.duoc.pedidos360.msclientes.entity.Cliente;
import cl.duoc.pedidos360.msclientes.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService {
    @Autowired
    private ClienteRepository repository;

    public List<Cliente> listar() { return repository.findAll(); }

    public Cliente buscarPorId(Long id) { return repository.findById(id).orElse(null); }

    public Cliente buscarPorEmail(String email) { return repository.findByEmail(email).orElse(null); }

    public Cliente guardar(Cliente c) { return repository.save(c); }

    public Cliente actualizar(Long id, Cliente datos) {
        Cliente existente = repository.findById(id).orElse(null);
        if (existente == null) return null;
        existente.setNombre(datos.getNombre());
        existente.setEmail(datos.getEmail());
        existente.setTelefono(datos.getTelefono());
        existente.setDireccion(datos.getDireccion());
        return repository.save(existente);
    }

    public boolean eliminar(Long id) {
        if (!repository.existsById(id)) return false;
        repository.deleteById(id);
        return true;
    }
}
