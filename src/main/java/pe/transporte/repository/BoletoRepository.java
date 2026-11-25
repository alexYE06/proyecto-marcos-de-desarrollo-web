package pe.transporte.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.transporte.model.Boleto;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoletoRepository extends JpaRepository<Boleto, Integer> {
    List<Boleto> findByViaje_IdViajeOrderByNumeroAsientoAsc(Integer idViaje);
    Optional<Boleto> findByCodigoBoletoIgnoreCase(String codigoBoleto);
    List<Boleto> findByViaje_IdViajeAndPasajeroDocumento(Integer idViaje, String pasajeroDocumento);
}
