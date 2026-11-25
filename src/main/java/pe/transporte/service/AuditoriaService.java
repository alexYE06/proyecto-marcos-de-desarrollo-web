package pe.transporte.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.transporte.model.AuditoriaLog;
import pe.transporte.repository.AuditoriaLogRepository;
import pe.transporte.security.SesionUsuario;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de bitácora inmutable de auditoría.
 * Registra toda acción crítica (anulaciones, desactivaciones, cambios de rol, cancelaciones de viajes, abordajes).
 * Por diseño de seguridad, NO expone ningún método de borrado ni edición de registros de auditoría.
 */
@Service
public class AuditoriaService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final AuditoriaLogRepository auditoriaLogRepository;

    public AuditoriaService(AuditoriaLogRepository auditoriaLogRepository) {
        this.auditoriaLogRepository = auditoriaLogRepository;
    }

    @Transactional
    public void registrar(SesionUsuario actor,
                          String modulo,
                          String accion,
                          String entidad,
                          Object idEntidad,
                          String descripcion,
                          String motivo) {
        AuditoriaLog log = new AuditoriaLog();
        log.setFechaHora(LocalDateTime.now());
        if (actor != null) {
            log.setIdUsuarioActor(actor.idUsuario());
            log.setActorNombre(actor.nombreCompleto());
            log.setActorRol(actor.rol().name());
        } else {
            log.setActorNombre("Sistema Automático");
            log.setActorRol("SISTEMA");
        }
        log.setModulo(modulo);
        log.setAccion(accion);
        log.setEntidad(entidad);
        log.setIdEntidad(idEntidad != null ? String.valueOf(idEntidad) : "-");
        log.setDescripcion(recortar(descripcion, 350));
        log.setMotivo(motivo != null && !motivo.isBlank() ? recortar(motivo.trim(), 250) : null);
        auditoriaLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarEventos(String moduloFiltro, String accionFiltro) {
        return auditoriaLogRepository.findTop200ByOrderByFechaHoraDescIdAuditoriaDesc().stream()
                .filter(e -> moduloFiltro == null || moduloFiltro.isBlank() || moduloFiltro.equalsIgnoreCase(e.getModulo()))
                .filter(e -> accionFiltro == null || accionFiltro.isBlank() || accionFiltro.equalsIgnoreCase(e.getAccion()))
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("idAuditoria", e.getIdAuditoria());
                    row.put("fechaHora", e.getFechaHora() != null ? e.getFechaHora().format(FMT) : "-");
                    row.put("actorNombre", e.getActorNombre());
                    row.put("actorRol", e.getActorRol());
                    row.put("modulo", e.getModulo());
                    row.put("accion", e.getAccion());
                    row.put("entidad", e.getEntidad());
                    row.put("idEntidad", e.getIdEntidad());
                    row.put("descripcion", e.getDescripcion());
                    row.put("motivo", e.getMotivo());
                    return row;
                })
                .toList();
    }

    private String recortar(String texto, int max) {
        if (texto == null) return "";
        return texto.length() <= max ? texto : texto.substring(0, max - 3) + "...";
    }
}
