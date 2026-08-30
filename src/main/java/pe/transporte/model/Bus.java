package pe.transporte.model;

public class Bus {
    private Integer idBus;
    private String placa;
    private String numeroInterno;
    private String modelo;
    private String marca;
    private Integer pisos;
    private Integer capacidadAsientos;
    private String estado; // 'Activo', 'Mantenimiento'

    public Bus() {}

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

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}