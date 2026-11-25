package pe.transporte.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "empleado")
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empleado")
    private Integer idEmpleado;

    @Column(name = "dni_empleado", nullable = false, unique = true, length = 15)
    private String dniEmpleado;

    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 60)
    private String apellido;

    @Column(name = "cargo", nullable = false, length = 30)
    private String cargo; // 'Conductor', 'Copiloto', 'Terramoza'

    @Column(name = "licencia", length = 25)
    private String licencia;

    @Column(name = "telefono", length = 15)
    private String telefono;

    @Column(name = "sueldo", precision = 10, scale = 2)
    private BigDecimal sueldo;

    @Column(name = "activo")
    private Boolean activo = true;

    public Empleado() {
    }

    public Empleado(Integer idEmpleado, String dniEmpleado, String nombre, String apellido,
                    String cargo, String telefono, BigDecimal sueldo) {
        this.idEmpleado = idEmpleado;
        this.dniEmpleado = dniEmpleado;
        this.nombre = nombre;
        this.apellido = apellido;
        this.cargo = cargo;
        this.telefono = telefono;
        this.sueldo = sueldo;
        this.activo = true;
    }

    public String getNombreCompleto() {
        return ((nombre != null ? nombre : "") + " " + (apellido != null ? apellido : "")).trim();
    }

    public Integer getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(Integer idEmpleado) { this.idEmpleado = idEmpleado; }

    public String getDniEmpleado() { return dniEmpleado; }
    public void setDniEmpleado(String dniEmpleado) { this.dniEmpleado = dniEmpleado; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }

    public String getLicencia() { return licencia; }
    public void setLicencia(String licencia) { this.licencia = licencia; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public BigDecimal getSueldo() { return sueldo; }
    public void setSueldo(BigDecimal sueldo) { this.sueldo = sueldo; }

    public Boolean getActivo() { return activo == null || activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
