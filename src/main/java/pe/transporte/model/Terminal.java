package pe.transporte.model;

public class Terminal {
    private Integer idTerminal;
    private String nombre;
    private String ciudad;
    private String direccion;

    public Terminal() {}

    public Terminal(Integer idTerminal, String nombre, String ciudad, String direccion) {
        this.idTerminal = idTerminal;
        this.nombre = nombre;
        this.ciudad = ciudad;
        this.direccion = direccion;
    }

    public Integer getIdTerminal() { return idTerminal; }
    public void setIdTerminal(Integer idTerminal) { this.idTerminal = idTerminal; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
}