package pe.transporte.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "boleto_emitido")
public class Boleto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_boleto")
    private Integer idBoleto;

    @Column(name = "codigo_boleto", nullable = false, unique = true, length = 30)
    private String codigoBoleto;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_venta", nullable = false)
    private Venta venta;

    @ManyToOne
    @JoinColumn(name = "id_viaje", nullable = false)
    private Viaje viaje;

    @Column(name = "numero_asiento", nullable = false)
    private Integer numeroAsiento;

    @Column(name = "piso", nullable = false)
    private Integer piso;

    @Column(name = "categoria_asiento", nullable = false, length = 30)
    private String categoriaAsiento;

    @Column(name = "pasajero_tipo_doc", nullable = false, length = 15)
    private String pasajeroTipoDoc = "DNI";

    @Column(name = "pasajero_documento", nullable = false, length = 20)
    private String pasajeroDocumento;

    @Column(name = "pasajero_nombre", nullable = false, length = 80)
    private String pasajeroNombre;

    @Column(name = "pasajero_apellido", nullable = false, length = 80)
    private String pasajeroApellido;

    @Column(name = "pasajero_correo", length = 120)
    private String pasajeroCorreo;

    @Column(name = "pasajero_telefono", length = 20)
    private String pasajeroTelefono;

    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "codigo_qr", columnDefinition = "TEXT")
    private String codigoQr;

    @Column(name = "estado_abordaje", nullable = false, length = 20)
    private String estadoAbordaje = "PENDIENTE"; // PENDIENTE | ABORDADO | NO_SHOW

    @Column(name = "fecha_abordaje")
    private LocalDateTime fechaAbordaje;

    @ManyToOne
    @JoinColumn(name = "id_inspector_abordaje")
    private Usuario inspectorAbordaje;

    public Boleto() {
    }

    public String getPasajeroNombreCompleto() {
        return ((pasajeroNombre != null ? pasajeroNombre : "") + " " + (pasajeroApellido != null ? pasajeroApellido : "")).trim();
    }

    public Integer getIdBoleto() { return idBoleto; }
    public void setIdBoleto(Integer idBoleto) { this.idBoleto = idBoleto; }

    public String getCodigoBoleto() { return codigoBoleto; }
    public void setCodigoBoleto(String codigoBoleto) { this.codigoBoleto = codigoBoleto; }

    public Venta getVenta() { return venta; }
    public void setVenta(Venta venta) { this.venta = venta; }

    public Viaje getViaje() { return viaje; }
    public void setViaje(Viaje viaje) { this.viaje = viaje; }

    public Integer getNumeroAsiento() { return numeroAsiento; }
    public void setNumeroAsiento(Integer numeroAsiento) { this.numeroAsiento = numeroAsiento; }

    public Integer getPiso() { return piso; }
    public void setPiso(Integer piso) { this.piso = piso; }

    public String getCategoriaAsiento() { return categoriaAsiento; }
    public void setCategoriaAsiento(String categoriaAsiento) { this.categoriaAsiento = categoriaAsiento; }

    public String getPasajeroTipoDoc() { return pasajeroTipoDoc; }
    public void setPasajeroTipoDoc(String pasajeroTipoDoc) { this.pasajeroTipoDoc = pasajeroTipoDoc; }

    public String getPasajeroDocumento() { return pasajeroDocumento; }
    public void setPasajeroDocumento(String pasajeroDocumento) { this.pasajeroDocumento = pasajeroDocumento; }

    public String getPasajeroNombre() { return pasajeroNombre; }
    public void setPasajeroNombre(String pasajeroNombre) { this.pasajeroNombre = pasajeroNombre; }

    public String getPasajeroApellido() { return pasajeroApellido; }
    public void setPasajeroApellido(String pasajeroApellido) { this.pasajeroApellido = pasajeroApellido; }

    public String getPasajeroCorreo() { return pasajeroCorreo; }
    public void setPasajeroCorreo(String pasajeroCorreo) { this.pasajeroCorreo = pasajeroCorreo; }

    public String getPasajeroTelefono() { return pasajeroTelefono; }
    public void setPasajeroTelefono(String pasajeroTelefono) { this.pasajeroTelefono = pasajeroTelefono; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public String getCodigoQr() { return codigoQr; }
    public void setCodigoQr(String codigoQr) { this.codigoQr = codigoQr; }

    public String getEstadoAbordaje() { return estadoAbordaje; }
    public void setEstadoAbordaje(String estadoAbordaje) { this.estadoAbordaje = estadoAbordaje; }

    public LocalDateTime getFechaAbordaje() { return fechaAbordaje; }
    public void setFechaAbordaje(LocalDateTime fechaAbordaje) { this.fechaAbordaje = fechaAbordaje; }

    public Usuario getInspectorAbordaje() { return inspectorAbordaje; }
    public void setInspectorAbordaje(Usuario inspectorAbordaje) { this.inspectorAbordaje = inspectorAbordaje; }
}
