package pe.transporte.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.transporte.model.Viaje;
import java.util.List;

@Repository
public interface ViajeRepository extends JpaRepository<Viaje, Integer> {
    
    @Query("SELECT v FROM Viaje v WHERE v.ruta.origen.idTerminal = :origenId AND v.ruta.destino.idTerminal = :destinoId")
    List<Viaje> buscarViajesPorRuta(@Param("origenId") Integer origenId, @Param("destinoId") Integer destinoId);
}