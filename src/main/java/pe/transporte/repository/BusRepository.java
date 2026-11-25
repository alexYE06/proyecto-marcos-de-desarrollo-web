package pe.transporte.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.transporte.model.Bus;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusRepository extends JpaRepository<Bus, Integer> {
    List<Bus> findAllByOrderByIdBusAsc();
    boolean existsByPlacaIgnoreCase(String placa);
    Optional<Bus> findByPlacaIgnoreCase(String placa);
}
