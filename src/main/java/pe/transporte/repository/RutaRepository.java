package pe.transporte.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.transporte.model.Ruta;

@Repository
public interface RutaRepository extends JpaRepository<Ruta, Integer> {
}
