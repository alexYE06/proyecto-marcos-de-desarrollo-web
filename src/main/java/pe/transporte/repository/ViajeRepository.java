package pe.transporte.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.transporte.model.Viaje;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ViajeRepository extends JpaRepository<Viaje, Integer> {
    List<Viaje> findAllByOrderByFechaHoraSalidaAsc();
    List<Viaje> findByFechaHoraSalidaBetweenOrderByFechaHoraSalidaAsc(LocalDateTime inicio, LocalDateTime fin);
}
