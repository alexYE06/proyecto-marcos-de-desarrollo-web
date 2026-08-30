package pe.transporte.model;

import java.math.BigDecimal;

public class Ruta {
    private Integer idRuta;
    private Terminal origen;
    private Terminal destino;
    private BigDecimal duracionEstimadaHoras;

    public Ruta() {}

    public Ruta(Integer idRuta, Terminal origen, Terminal destino, BigDecimal duracionEstimadaHoras) {
        this.idRuta = idRuta;
        this.origen = origen;
        this.destino = destino;
        this.duracionEstimadaHoras = duracionEstimadaHoras;
    }

    public Integer getIdRuta() { return idRuta; }
    public void setIdRuta(Integer idRuta) { this.idRuta = idRuta; }

    public Terminal getOrigen() { return origen; }
    public void setOrigen(Terminal origen) { this.origen = origen; }

    public Terminal getDestino() { return destino; }
    public void setDestino(Terminal destino) { this.destino = destino; }

    public BigDecimal getDuracionEstimadaHoras() { return duracionEstimadaHoras; }
    public void setDuracionEstimadaHoras(BigDecimal duracionEstimadaHoras) { this.duracionEstimadaHoras = duracionEstimadaHoras; }
}