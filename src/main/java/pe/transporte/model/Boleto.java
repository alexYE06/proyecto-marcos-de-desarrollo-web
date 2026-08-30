package pe.transporte.model;

import java.math.BigDecimal;

public class Boleto {
    private Integer idBoleto;
    private String codigoBoleto;
    private Venta venta;
    private Viaje viaje;
    private Asiento asiento;
    private String pasajeroNombre;
    private String pasajeroDni;
    private BigDecimal precio;
    private String codigoQr;

    public Boleto() {}

    public Boleto(Integer idBoleto, String codigoBoleto, Venta venta, Viaje viaje, 
                  Asiento asiento, String pasajeroNombre, String pasajeroDni, 
                  BigDecimal precio, String codigoQr) {
        this.idBoleto = idBoleto;
        this.codigoBoleto = codigoBoleto;
        this.venta = venta;
        this.viaje = viaje;
        this.asiento = asiento;
        this.pasajeroNombre = pasajeroNombre;
        this.pasajeroDni = pasajeroDni;
        this.precio = precio;
        this.codigoQr = codigoQr;
    }

    public Integer getIdBoleto() { return idBoleto; }
    public void setIdBoleto(Integer idBoleto) { this.idBoleto = idBoleto; }

    public String getCodigoBoleto() { return codigoBoleto; }
    public void setCodigoBoleto(String codigoBoleto) { this.codigoBoleto = codigoBoleto; }

    public Venta getVenta() { return venta; }
    public void setVenta(Venta venta) { this.venta = venta; }

    public Viaje getViaje() { return viaje; }
    public void setViaje(Viaje viaje) { this.viaje = viaje; }

    public Asiento getAsiento() { return asiento; }
    public void setAsiento(Asiento asiento) { this.asiento = asiento; }

    public String getPasajeroNombre() { return pasajeroNombre; }
    public void setPasajeroNombre(String pasajeroNombre) { this.pasajeroNombre = pasajeroNombre; }

    public String getPasajeroDni() { return pasajeroDni; }
    public void setPasajeroDni(String pasajeroDni) { this.pasajeroDni = pasajeroDni; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public String getCodigoQr() { return codigoQr; }
    public void setCodigoQr(String codigoQr) { this.codigoQr = codigoQr; }
}