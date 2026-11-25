package pe.transporte.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.transporte.model.Ruta;

import java.util.List;

@Repository
public interface RutaRepository extends JpaRepository<Ruta, Integer> {
    List<Ruta> findAllByOrderByIdRutaAsc();
    boolean existsByOrigen_IdTerminalAndDestino_IdTerminal(Integer idOrigen, Integer idDestino);
}
