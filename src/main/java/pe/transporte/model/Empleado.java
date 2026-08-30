package pe.transporte.model;

import java.math.BigDecimal;

public class Empleado {
    private Integer idEmpleado;
    private String dniEmpleado;
    private String nombre;
    private String apellido;
    private String cargo; // 'Conductor', 'Copiloto'
    private String telefono;
    private BigDecimal sueldo;

    public Empleado() {}

    public Empleado(Integer idEmpleado, String dniEmpleado, String nombre, String apellido, 
                    String cargo, String telefono, BigDecimal sueldo) {
        this.idEmpleado = idEmpleado;
        this.dniEmpleado = dniEmpleado;
        this.nombre = nombre;
        this.apellido = apellido;
        this.cargo = cargo;
        this.telefono = telefono;
        this.sueldo = sueldo;
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

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public BigDecimal getSueldo() { return sueldo; }
    public void setSueldo(BigDecimal sueldo) { this.sueldo = sueldo; }
}