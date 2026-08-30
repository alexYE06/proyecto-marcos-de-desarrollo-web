package pe.transporte.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Venta {
    private String idVenta; // UUID o ID String
    private String codigoReserva;
    private Usuario usuario;
    private LocalDateTime fechaVenta;
    private BigDecimal montoTotal;
    private String estadoPago; // 'RESERVADO', 'PAGADO', 'CANCELADO'
    private LocalDateTime expiraEn;

    public Venta() {}

    public Venta(String idVenta, String codigoReserva, Usuario usuario, LocalDateTime fechaVenta, 
                 BigDecimal montoTotal, String estadoPago, LocalDateTime expiraEn) {
        this.idVenta = idVenta;
        this.codigoReserva = codigoReserva;
        this.usuario = usuario;
        this.fechaVenta = fechaVenta;
        this.montoTotal = montoTotal;
        this.estadoPago = estadoPago;
        this.expiraEn = expiraEn;
    }

    public String getIdVenta() { return idVenta; }
    public void setIdVenta(String idVenta) { this.idVenta = idVenta; }

    public String getCodigoReserva() { return codigoReserva; }
    public void setCodigoReserva(String codigoReserva) { this.codigoReserva = codigoReserva; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public LocalDateTime getFechaVenta() { return fechaVenta; }
    public void setFechaVenta(LocalDateTime fechaVenta) { this.fechaVenta = fechaVenta; }

    public BigDecimal getMontoTotal() { return montoTotal; }
    public void setMontoTotal(BigDecimal montoTotal) { this.montoTotal = montoTotal; }

    public String getEstadoPago() { return estadoPago; }
    public void setEstadoPago(String estadoPago) { this.estadoPago = estadoPago; }

    public LocalDateTime getExpiraEn() { return expiraEn; }
    public void setExpiraEn(LocalDateTime expiraEn) { this.expiraEn = expiraEn; }
}