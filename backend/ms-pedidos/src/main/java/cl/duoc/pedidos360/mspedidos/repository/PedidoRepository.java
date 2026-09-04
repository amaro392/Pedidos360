package cl.duoc.pedidos360.mspedidos.repository;

import cl.duoc.pedidos360.mspedidos.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
}
