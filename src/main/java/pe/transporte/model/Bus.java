package pe.transporte.model;

import jakarta.persistence.*;

@Entity
@Table(name = "bus")
public class Bus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bus")
    private Integer idBus;

    @Column(name = "placa", nullable = false, unique = true, length = 15)
    private String placa;

    @Column(name = "numero_interno", length = 20)
    private String numeroInterno;

    @Column(name = "modelo", length = 80)
    private String modelo;

    @Column(name = "marca", length = 60)
    private String marca;

    @Column(name = "pisos", nullable = false)
    private Integer pisos = 2;

    @Column(name = "capacidad_asientos", nullable = false)
    private Integer capacidadAsientos = 64;

    @Column(name = "estado", nullable = false, length = 25)
    private String estado = "Activo"; // 'Activo', 'Mantenimiento', 'Inactivo'

    public Bus() {
    }

    public Bus(Integer idBus, String placa, String numeroInterno, String modelo,
               String marca, Integer pisos, Integer capacidadAsientos, String estado) {
        this.idBus = idBus;
        this.placa = placa;
        this.numeroInterno = numeroInterno;
        this.modelo = modelo;
        this.marca = marca;
        this.pisos = pisos;
        this.capacidadAsientos = capacidadAsientos;
        this.estado = estado;
    }

    public Integer getIdBus() { return idBus; }
    public void setIdBus(Integer idBus) { this.idBus = idBus; }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }

    public String getNumeroInterno() { return numeroInterno; }
    public void setNumeroInterno(String numeroInterno) { this.numeroInterno = numeroInterno; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public Integer getPisos() { return pisos; }
    public void setPisos(Integer pisos) { this.pisos = pisos; }

    public Integer getCapacidadAsientos() { return capacidadAsientos; }
    public void setCapacidadAsientos(Integer capacidadAsientos) { this.capacidadAsientos = capacidadAsientos; }

    public String getEstado() { return estado != null ? estado : "Activo"; }
    public void setEstado(String estado) { this.estado = estado; }
}
