package pe.transporte.model;

import java.time.LocalDateTime;

public class Usuario {
    private Integer idUsuario;
    private String nombre;
    private String apellido;
    private String dniPasaporte;
    private String correo;
    private String telefono;
    private String contrasenaHash;
    private String rol; // 'CLIENTE', 'ADMIN'
    private LocalDateTime fechaRegistro;

    public Usuario() {}

    public Usuario(Integer idUsuario, String nombre, String apellido, String dniPasaporte, 
                   String correo, String telefono, String contrasenaHash, String rol, LocalDateTime fechaRegistro) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.apellido = apellido;
        this.dniPasaporte = dniPasaporte;
        this.correo = correo;
        this.telefono = telefono;
        this.contrasenaHash = contrasenaHash;
        this.rol = rol;
        this.fechaRegistro = fechaRegistro;
    }

    // Getters y Setters
    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getDniPasaporte() { return dniPasaporte; }
    public void setDniPasaporte(String dniPasaporte) { this.dniPasaporte = dniPasaporte; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getContrasenaHash() { return contrasenaHash; }
    public void setContrasenaHash(String contrasenaHash) { this.contrasenaHash = contrasenaHash; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}