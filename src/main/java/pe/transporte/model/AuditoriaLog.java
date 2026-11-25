package pe.transporte.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_log")
public class AuditoriaLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Integer idAuditoria;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    @Column(name = "id_usuario_actor")
    private Integer idUsuarioActor;

    @Column(name = "actor_nombre", nullable = false, length = 120)
    private String actorNombre;

    @Column(name = "actor_rol", nullable = false, length = 25)
    private String actorRol;

    @Column(name = "modulo", nullable = false, length = 40)
    private String modulo;

    @Column(name = "accion", nullable = false, length = 45)
    private String accion;

    @Column(name = "entidad", nullable = false, length = 40)
    private String entidad;

    @Column(name = "id_entidad", length = 40)
    private String idEntidad;

    @Column(name = "descripcion", nullable = false, length = 350)
    private String descripcion;

    @Column(name = "motivo", length = 250)
    private String motivo;

    public AuditoriaLog() {
    }

    public Integer getIdAuditoria() { return idAuditoria; }
    public void setIdAuditoria(Integer idAuditoria) { this.idAuditoria = idAuditoria; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public Integer getIdUsuarioActor() { return idUsuarioActor; }
    public void setIdUsuarioActor(Integer idUsuarioActor) { this.idUsuarioActor = idUsuarioActor; }

    public String getActorNombre() { return actorNombre; }
    public void setActorNombre(String actorNombre) { this.actorNombre = actorNombre; }

    public String getActorRol() { return actorRol; }
    public void setActorRol(String actorRol) { this.actorRol = actorRol; }

    public String getModulo() { return modulo; }
    public void setModulo(String modulo) { this.modulo = modulo; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }

    public String getIdEntidad() { return idEntidad; }
    public void setIdEntidad(String idEntidad) { this.idEntidad = idEntidad; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
