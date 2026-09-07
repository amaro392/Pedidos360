package cl.duoc.pedidos360.ms_notificaciones.service;

import cl.duoc.pedidos360.ms_notificaciones.entity.Notificacion;
import cl.duoc.pedidos360.ms_notificaciones.repository.NotificacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificacionService {

    @Autowired
    private NotificacionRepository repository;

    public List<Notificacion> listar() {
        return repository.findAll();
    }

    public Notificacion buscarPorId(Long id) {
        return repository.findById(id).orElse(null);
    }

    public List<Notificacion> buscarPorDestinatario(String email) {
        return repository.findByDestinatarioEmail(email);
    }

    public List<Notificacion> buscarPorPedido(Long pedidoId) {
        return repository.findByPedidoId(pedidoId);
    }

    // Simula el envio: en esta etapa solo persiste el registro con estado ENVIADA.
    // Mas adelante aqui se puede integrar un proveedor real (SES, SMTP, etc).
    public Notificacion enviar(Notificacion notificacion) {
        if (notificacion.getEstado() == null) {
            notificacion.setEstado("ENVIADA");
        }
        return repository.save(notificacion);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}
