package pe.transporte.model;

import jakarta.persistence.*;

@Entity
@Table(name = "ruta")
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ruta")
    private Integer idRuta;

    // Llave foránea hacia la tabla Terminal (Origen)
    @ManyToOne
    @JoinColumn(name = "id_terminal_origen", nullable = false)
    private Terminal origen;

    // Llave foránea hacia la tabla Terminal (Destino)
    @ManyToOne
    @JoinColumn(name = "id_terminal_destino", nullable = false)
    private Terminal destino;

    @Column(name = "precio_base")
    private Double precioBase;

    @Column(name = "horas_estimadas")
    private Integer horasEstimadas;

    public Ruta() {
    }

    // Getters y Setters
    public Integer getIdRuta() {
        return idRuta;
    }

    public void setIdRuta(Integer idRuta) {
        this.idRuta = idRuta;
    }

    public Terminal getOrigen() {
        return origen;
    }

    public void setOrigen(Terminal origen) {
        this.origen = origen;
    }

    public Terminal getDestino() {
        return destino;
    }

    public void setDestino(Terminal destino) {
        this.destino = destino;
    }

    public Double getPrecioBase() {
        return precioBase;
    }

    public void setPrecioBase(Double precioBase) {
        this.precioBase = precioBase;
    }

    public Integer getHorasEstimadas() {
        return horasEstimadas;
    }

    public void setHorasEstimadas(Integer horasEstimadas) {
        this.horasEstimadas = horasEstimadas;
    }
}
