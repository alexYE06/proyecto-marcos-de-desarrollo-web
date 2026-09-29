package pe.transporte.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.transporte.model.Viaje;

@Repository
public interface ViajeRepository extends JpaRepository<Viaje, Integer> {
    // Al extender de JpaRepository ya tiene findAll(), findById(), save(), etc.
}