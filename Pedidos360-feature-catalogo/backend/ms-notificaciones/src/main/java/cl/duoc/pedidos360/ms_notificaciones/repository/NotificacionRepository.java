package cl.duoc.pedidos360.ms_notificaciones.repository;

import cl.duoc.pedidos360.ms_notificaciones.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findByDestinatarioEmail(String destinatarioEmail);
    List<Notificacion> findByPedidoId(Long pedidoId);
}
