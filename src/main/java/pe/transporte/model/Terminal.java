package pe.transporte.model;

import jakarta.persistence.*;

@Entity
@Table(name = "terminal")
public class Terminal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_terminal")
    private Integer idTerminal;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "ciudad", nullable = false, length = 80)
    private String ciudad;

    @Column(name = "direccion", length = 200)
    private String direccion;

    @Column(name = "activo")
    private Boolean activo = true;

    public Terminal() {
    }

    public Terminal(Integer idTerminal, String nombre, String ciudad, String direccion) {
        this.idTerminal = idTerminal;
        this.nombre = nombre;
        this.ciudad = ciudad;
        this.direccion = direccion;
        this.activo = true;
    }

    public Integer getIdTerminal() { return idTerminal; }
    public void setIdTerminal(Integer idTerminal) { this.idTerminal = idTerminal; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public Boolean getActivo() { return activo == null || activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
