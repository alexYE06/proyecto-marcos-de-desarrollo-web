package pe.transporte.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.transporte.model.AuditoriaLog;

import java.util.List;

@Repository
public interface AuditoriaLogRepository extends JpaRepository<AuditoriaLog, Integer> {
    List<AuditoriaLog> findTop200ByOrderByFechaHoraDescIdAuditoriaDesc();
}
