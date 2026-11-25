package pe.transporte.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.transporte.model.Usuario;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByCorreoIgnoreCase(String correo);
    Optional<Usuario> findByCorreo(String correo);
    boolean existsByCorreoIgnoreCase(String correo);
    boolean existsByNumeroDocumento(String numeroDocumento);
    List<Usuario> findAllByOrderByIdUsuarioDesc();
    List<Usuario> findByRolIgnoreCaseOrderByIdUsuarioDesc(String rol);
    long countByRolIgnoreCase(String rol);
}
