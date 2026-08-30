package pe.transporte.model;

public class Asiento {
    private Integer idAsiento;
    private Integer idBus;
    private Integer numeroAsiento;
    private Integer piso;
    private String tipoAsiento; // 'VIP', 'Semi-Cama'
    private Integer posicionX;
    private Integer posicionY;

    public Asiento() {}

    public Asiento(Integer idAsiento, Integer idBus, Integer numeroAsiento, 
                   Integer piso, String tipoAsiento, Integer posicionX, Integer posicionY) {
        this.idAsiento = idAsiento;
        this.idBus = idBus;
        this.numeroAsiento = numeroAsiento;
        this.piso = piso;
        this.tipoAsiento = tipoAsiento;
        this.posicionX = posicionX;
        this.posicionY = posicionY;
    }

    public Integer getIdAsiento() { return idAsiento; }
    public void setIdAsiento(Integer idAsiento) { this.idAsiento = idAsiento; }

    public Integer getIdBus() { return idBus; }
    public void setIdBus(Integer idBus) { this.idBus = idBus; }

    public Integer getNumeroAsiento() { return numeroAsiento; }
    public void setNumeroAsiento(Integer numeroAsiento) { this.numeroAsiento = numeroAsiento; }

    public Integer getPiso() { return piso; }
    public void setPiso(Integer piso) { this.piso = piso; }

    public String getTipoAsiento() { return tipoAsiento; }
    public void setTipoAsiento(String tipoAsiento) { this.tipoAsiento = tipoAsiento; }

    public Integer getPosicionX() { return posicionX; }
    public void setPosicionX(Integer posicionX) { this.posicionX = posicionX; }

    public Integer getPosicionY() { return posicionY; }
    public void setPosicionY(Integer posicionY) { this.posicionY = posicionY; }
}