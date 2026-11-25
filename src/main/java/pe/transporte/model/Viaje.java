package pe.transporte.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "viaje")
public class Viaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_viaje")
    private Integer idViaje;

    @ManyToOne
    @JoinColumn(name = "id_ruta", nullable = false)
    private Ruta ruta;

    @ManyToOne
    @JoinColumn(name = "id_bus")
    private Bus bus;

    @ManyToOne
    @JoinColumn(name = "id_conductor")
    private Empleado conductor;

    @ManyToOne
    @JoinColumn(name = "id_copiloto")
    private Empleado copiloto;

    @Column(name = "fecha_hora_salida", nullable = false)
    private LocalDateTime fechaHoraSalida;

    @Column(name = "precio", nullable = false)
    private Double precio;

    @Column(name = "asientos_disponibles", nullable = false)
    private Integer asientosDisponibles = 64;

    @Column(name = "servicio", length = 50)
    private String servicio = "Doble Piso VIP";

    @Column(name = "estado", length = 25)
    private String estado = "PROGRAMADO";

    public Viaje() {
    }

    public Integer getIdViaje() { return idViaje; }
    public void setIdViaje(Integer idViaje) { this.idViaje = idViaje; }

    public Ruta getRuta() { return ruta; }
    public void setRuta(Ruta ruta) { this.ruta = ruta; }

    public Bus getBus() { return bus; }
    public void setBus(Bus bus) { this.bus = bus; }

    public Empleado getConductor() { return conductor; }
    public void setConductor(Empleado conductor) { this.conductor = conductor; }

    public Empleado getCopiloto() { return copiloto; }
    public void setCopiloto(Empleado copiloto) { this.copiloto = copiloto; }

    public LocalDateTime getFechaHoraSalida() { return fechaHoraSalida; }
    public void setFechaHoraSalida(LocalDateTime fechaHoraSalida) { this.fechaHoraSalida = fechaHoraSalida; }

    public Double getPrecio() { return precio; }
    public void setPrecio(Double precio) { this.precio = precio; }

    public Integer getAsientosDisponibles() { return asientosDisponibles; }
    public void setAsientosDisponibles(Integer asientosDisponibles) { this.asientosDisponibles = asientosDisponibles; }

    public String getServicio() { return servicio != null ? servicio : "Doble Piso VIP"; }
    public void setServicio(String servicio) { this.servicio = servicio; }

    public String getEstado() { return estado != null ? estado : "PROGRAMADO"; }
    public void setEstado(String estado) { this.estado = estado; }
}
