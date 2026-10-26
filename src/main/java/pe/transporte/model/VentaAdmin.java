package pe.transporte.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "venta_admin")
public class VentaAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venta_admin")
    private Integer idVentaAdmin;

    // Números de asiento vendidos, separados por coma. Ej: "3,4,21"
    @Column(name = "asientos", nullable = false, length = 100)
    private String asientos;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "precio_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioTotal;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    // TARJETA, PAYPAL, YAPE, EFECTIVO (mismos valores que pago.js)
    @Column(name = "metodo_pago", nullable = false, length = 20)
    private String metodoPago = "EFECTIVO";

    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    public VentaAdmin() {}

    public Integer getIdVentaAdmin() { return idVentaAdmin; }
    public void setIdVentaAdmin(Integer idVentaAdmin) { this.idVentaAdmin = idVentaAdmin; }

    public String getAsientos() { return asientos; }
    public void setAsientos(String asientos) { this.asientos = asientos; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public BigDecimal getPrecioTotal() { return precioTotal; }
    public void setPrecioTotal(BigDecimal precioTotal) { this.precioTotal = precioTotal; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
}
