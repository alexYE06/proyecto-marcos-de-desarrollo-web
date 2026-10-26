package pe.transporte.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.transporte.model.VentaAdmin;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VentaAdminRepository extends JpaRepository<VentaAdmin, Integer> {
    List<VentaAdmin> findAllByOrderByFechaDescIdVentaAdminDesc();
    List<VentaAdmin> findByFecha(LocalDate fecha);
}
