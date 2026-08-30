package pe.transporte.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class Viaje {
    private Integer idViaje;
    private Ruta ruta;
    private Bus bus;
    private Empleado conductor;
    private LocalDate fechaSalida;
    private LocalTime horaSalida;
    private BigDecimal precioBase;
    private String estadoViaje; // 'Programado', 'En Ruta', 'Cancelado'

    public Viaje() {}

    public Viaje(Integer idViaje, Ruta ruta, Bus bus, Empleado conductor, 
                 LocalDate fechaSalida, LocalTime horaSalida, BigDecimal precioBase, String estadoViaje) {
        this.idViaje = idViaje;
        this.ruta = ruta;
        this.bus = bus;
        this.conductor = conductor;
        this.fechaSalida = fechaSalida;
        this.horaSalida = horaSalida;
        this.precioBase = precioBase;
        this.estadoViaje = estadoViaje;
    }

    public Integer getIdViaje() { return idViaje; }
    public void setIdViaje(Integer idViaje) { this.idViaje = idViaje; }

    public Ruta getRuta() { return ruta; }
    public void setRuta(Ruta ruta) { this.ruta = ruta; }

    public Bus getBus() { return bus; }
    public void setBus(Bus bus) { this.bus = bus; }

    public Empleado getConductor() { return conductor; }
    public void setConductor(Empleado conductor) { this.conductor = conductor; }

    public LocalDate getFechaSalida() { return fechaSalida; }
    public void setFechaSalida(LocalDate fechaSalida) { this.fechaSalida = fechaSalida; }

    public LocalTime getHoraSalida() { return horaSalida; }
    public void setHoraSalida(LocalTime horaSalida) { this.horaSalida = horaSalida; }

    public BigDecimal getPrecioBase() { return precioBase; }
    public void setPrecioBase(BigDecimal precioBase) { this.precioBase = precioBase; }

    public String getEstadoViaje() { return estadoViaje; }
    public void setEstadoViaje(String estadoViaje) { this.estadoViaje = estadoViaje; }
}