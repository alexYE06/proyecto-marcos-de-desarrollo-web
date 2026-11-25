package pe.transporte.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "operacion_venta")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venta")
    private Integer idVenta;

    @Column(name = "codigo_reserva", nullable = false, unique = true, length = 25)
    private String codigoReserva;

    @Column(name = "tipo_comprobante", nullable = false, length = 20)
    private String tipoComprobante = "BOLETA"; // BOLETA | FACTURA

    @Column(name = "serie_numero", nullable = false, unique = true, length = 25)
    private String serieNumero;

    @Column(name = "ruc_factura", length = 11)
    private String rucFactura;

    @Column(name = "razon_social_factura", length = 150)
    private String razonSocialFactura;

    @Column(name = "canal_venta", nullable = false, length = 20)
    private String canalVenta; // WEB | VENTANILLA

    @ManyToOne
    @JoinColumn(name = "id_viaje", nullable = false)
    private Viaje viaje;

    @ManyToOne
    @JoinColumn(name = "id_cliente")
    private Usuario cliente;

    @ManyToOne
    @JoinColumn(name = "id_vendedor")
    private Usuario vendedor;

    @Column(name = "comprador_nombre", nullable = false, length = 120)
    private String compradorNombre;

    @Column(name = "comprador_tipo_doc", nullable = false, length = 15)
    private String compradorTipoDoc = "DNI";

    @Column(name = "comprador_documento", nullable = false, length = 20)
    private String compradorDocumento;

    @Column(name = "comprador_correo", length = 120)
    private String compradorCorreo;

    @Column(name = "comprador_telefono", length = 20)
    private String compradorTelefono;

    @Column(name = "metodo_pago", nullable = false, length = 20)
    private String metodoPago;

    @Column(name = "codigo_cupon", length = 30)
    private String codigoCupon;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "descuento", nullable = false, precision = 10, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal = BigDecimal.ZERO;

    @Column(name = "estado_pago", nullable = false, length = 30)
    private String estadoPago; // RESERVADO | PAGADO | EXPIRADO | ANULACION_SOLICITADA | ANULADO

    @Column(name = "expira_en")
    private LocalDateTime expiraEn;

    @Column(name = "fecha_venta", nullable = false)
    private LocalDateTime fechaVenta = LocalDateTime.now();

    @Column(name = "motivo_anulacion", length = 250)
    private String motivoAnulacion;

    @ManyToOne
    @JoinColumn(name = "id_solicitante_anulacion")
    private Usuario solicitanteAnulacion;

    @ManyToOne
    @JoinColumn(name = "id_autorizador_anulacion")
    private Usuario autorizadorAnulacion;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Boleto> boletos = new ArrayList<>();

    public Venta() {
    }

    public void agregarBoleto(Boleto boleto) {
        boletos.add(boleto);
        boleto.setVenta(this);
    }

    public Integer getIdVenta() { return idVenta; }
    public void setIdVenta(Integer idVenta) { this.idVenta = idVenta; }

    public String getCodigoReserva() { return codigoReserva; }
    public void setCodigoReserva(String codigoReserva) { this.codigoReserva = codigoReserva; }

    public String getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(String tipoComprobante) { this.tipoComprobante = tipoComprobante; }

    public String getSerieNumero() { return serieNumero; }
    public void setSerieNumero(String serieNumero) { this.serieNumero = serieNumero; }

    public String getRucFactura() { return rucFactura; }
    public void setRucFactura(String rucFactura) { this.rucFactura = rucFactura; }

    public String getRazonSocialFactura() { return razonSocialFactura; }
    public void setRazonSocialFactura(String razonSocialFactura) { this.razonSocialFactura = razonSocialFactura; }

    public String getCanalVenta() { return canalVenta; }
    public void setCanalVenta(String canalVenta) { this.canalVenta = canalVenta; }

    public Viaje getViaje() { return viaje; }
    public void setViaje(Viaje viaje) { this.viaje = viaje; }

    public Usuario getCliente() { return cliente; }
    public void setCliente(Usuario cliente) { this.cliente = cliente; }

    public Usuario getVendedor() { return vendedor; }
    public void setVendedor(Usuario vendedor) { this.vendedor = vendedor; }

    public String getCompradorNombre() { return compradorNombre; }
    public void setCompradorNombre(String compradorNombre) { this.compradorNombre = compradorNombre; }

    public String getCompradorTipoDoc() { return compradorTipoDoc; }
    public void setCompradorTipoDoc(String compradorTipoDoc) { this.compradorTipoDoc = compradorTipoDoc; }

    public String getCompradorDocumento() { return compradorDocumento; }
    public void setCompradorDocumento(String compradorDocumento) { this.compradorDocumento = compradorDocumento; }

    public String getCompradorCorreo() { return compradorCorreo; }
    public void setCompradorCorreo(String compradorCorreo) { this.compradorCorreo = compradorCorreo; }

    public String getCompradorTelefono() { return compradorTelefono; }
    public void setCompradorTelefono(String compradorTelefono) { this.compradorTelefono = compradorTelefono; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getCodigoCupon() { return codigoCupon; }
    public void setCodigoCupon(String codigoCupon) { this.codigoCupon = codigoCupon; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getDescuento() { return descuento; }
    public void setDescuento(BigDecimal descuento) { this.descuento = descuento; }

    public BigDecimal getMontoTotal() { return montoTotal; }
    public void setMontoTotal(BigDecimal montoTotal) { this.montoTotal = montoTotal; }

    public String getEstadoPago() { return estadoPago; }
    public void setEstadoPago(String estadoPago) { this.estadoPago = estadoPago; }

    public LocalDateTime getExpiraEn() { return expiraEn; }
    public void setExpiraEn(LocalDateTime expiraEn) { this.expiraEn = expiraEn; }

    public LocalDateTime getFechaVenta() { return fechaVenta; }
    public void setFechaVenta(LocalDateTime fechaVenta) { this.fechaVenta = fechaVenta; }

    public String getMotivoAnulacion() { return motivoAnulacion; }
    public void setMotivoAnulacion(String motivoAnulacion) { this.motivoAnulacion = motivoAnulacion; }

    public Usuario getSolicitanteAnulacion() { return solicitanteAnulacion; }
    public void setSolicitanteAnulacion(Usuario solicitanteAnulacion) { this.solicitanteAnulacion = solicitanteAnulacion; }

    public Usuario getAutorizadorAnulacion() { return autorizadorAnulacion; }
    public void setAutorizadorAnulacion(Usuario autorizadorAnulacion) { this.autorizadorAnulacion = autorizadorAnulacion; }

    public LocalDateTime getFechaAnulacion() { return fechaAnulacion; }
    public void setFechaAnulacion(LocalDateTime fechaAnulacion) { this.fechaAnulacion = fechaAnulacion; }

    public List<Boleto> getBoletos() { return boletos; }
    public void setBoletos(List<Boleto> boletos) { this.boletos = boletos; }
}
