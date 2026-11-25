package pe.transporte.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.transporte.model.Venta;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Integer> {
    List<Venta> findAllByOrderByFechaVentaDesc();
    List<Venta> findByCliente_IdUsuarioOrderByFechaVentaDesc(Integer idCliente);
    List<Venta> findByVendedor_IdUsuarioOrderByFechaVentaDesc(Integer idVendedor);
    List<Venta> findByEstadoPagoOrderByFechaVentaDesc(String estadoPago);
    List<Venta> findByEstadoPagoAndExpiraEnBefore(String estadoPago, LocalDateTime ahora);
    Optional<Venta> findByCodigoReservaIgnoreCase(String codigoReserva);
}
