package pe.transporte.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.transporte.model.Terminal;

@Repository
public interface TerminalRepository extends JpaRepository<Terminal, Integer> {
}