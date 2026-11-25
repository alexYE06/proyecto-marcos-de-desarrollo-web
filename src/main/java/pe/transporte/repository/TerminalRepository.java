package pe.transporte.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.transporte.model.Terminal;

import java.util.List;

@Repository
public interface TerminalRepository extends JpaRepository<Terminal, Integer> {
    List<Terminal> findAllByOrderByCiudadAscNombreAsc();
}
