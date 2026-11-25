package pe.transporte.model;

import jakarta.persistence.*;

@Entity
@Table(name = "ruta")
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ruta")
    private Integer idRuta;

    @ManyToOne
    @JoinColumn(name = "id_terminal_origen", nullable = false)
    private Terminal origen;

    @ManyToOne
    @JoinColumn(name = "id_terminal_destino", nullable = false)
    private Terminal destino;

    @Column(name = "precio_base")
    private Double precioBase;

    @Column(name = "horas_estimadas")
    private Integer horasEstimadas;

    @Column(name = "distancia_km")
    private Integer distanciaKm;

    @Column(name = "activo")
    private Boolean activo = true;

    public Ruta() {
    }

    public Integer getIdRuta() { return idRuta; }
    public void setIdRuta(Integer idRuta) { this.idRuta = idRuta; }

    public Terminal getOrigen() { return origen; }
    public void setOrigen(Terminal origen) { this.origen = origen; }

    public Terminal getDestino() { return destino; }
    public void setDestino(Terminal destino) { this.destino = destino; }

    public Double getPrecioBase() { return precioBase; }
    public void setPrecioBase(Double precioBase) { this.precioBase = precioBase; }

    public Integer getHorasEstimadas() { return horasEstimadas; }
    public void setHorasEstimadas(Integer horasEstimadas) { this.horasEstimadas = horasEstimadas; }

    public Integer getDistanciaKm() { return distanciaKm; }
    public void setDistanciaKm(Integer distanciaKm) { this.distanciaKm = distanciaKm; }

    public Boolean getActivo() { return activo == null || activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    @Override
    public String toString() {
        String ciuOrig = origen != null ? origen.getCiudad() + " (" + origen.getNombre() + ")" : "";
        String ciuDest = destino != null ? destino.getCiudad() + " (" + destino.getNombre() + ")" : "";
        return ciuOrig + " -> " + ciuDest;
    }
}
