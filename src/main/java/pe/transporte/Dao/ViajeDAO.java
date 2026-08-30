package pe.transporte.dao;

import pe.transporte.config.ConexionBD;
import pe.transporte.model.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ViajeDAO {

    public List<Viaje> buscarViajes(int idOrigen, int idDestino, LocalDate fecha) {
        List<Viaje> lista = new ArrayList<>();
        String sql = "SELECT v.id_viaje, v.fecha_salida, v.hora_salida, v.precio_base, v.estado_viaje, "
                   + "b.id_bus, b.modelo, b.placa, b.pisos, b.capacidad_asientos, "
                   + "r.id_ruta, r.duracion_estimada_horas, "
                   + "t1.id_terminal as orig_id, t1.nombre as orig_nom, t1.ciudad as orig_ciu, "
                   + "t2.id_terminal as dest_id, t2.nombre as dest_nom, t2.ciudad as dest_ciu "
                   + "FROM viaje v "
                   + "INNER JOIN bus b ON v.id_bus = b.id_bus "
                   + "INNER JOIN ruta r ON v.id_ruta = r.id_ruta "
                   + "INNER JOIN terminal t1 ON r.id_terminal_origen = t1.id_terminal "
                   + "INNER JOIN terminal t2 ON r.id_terminal_destino = t2.id_terminal "
                   + "WHERE r.id_terminal_origen = ? AND r.id_terminal_destino = ? AND v.fecha_salida = ? AND v.estado_viaje = 'Programado'";

        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, idOrigen);
            ps.setInt(2, idDestino);
            ps.setDate(3, Date.valueOf(fecha));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Terminal orig = new Terminal(rs.getInt("orig_id"), rs.getString("orig_nom"), rs.getString("orig_ciu"), null);
                    Terminal dest = new Terminal(rs.getInt("dest_id"), rs.getString("dest_nom"), rs.getString("dest_ciu"), null);
                    Ruta ruta = new Ruta(rs.getInt("id_ruta"), orig, dest, rs.getBigDecimal("duracion_estimada_horas"));
                    
                    Bus bus = new Bus(rs.getInt("id_bus"), rs.getString("placa"), null, rs.getString("modelo"), null, rs.getInt("pisos"), rs.getInt("capacidad_asientos"), "Activo");

                    Viaje viaje = new Viaje();
                    viaje.setIdViaje(rs.getInt("id_viaje"));
                    viaje.setRuta(ruta);
                    viaje.setBus(bus);
                    viaje.setFechaSalida(rs.getDate("fecha_salida").toLocalDate());
                    viaje.setHoraSalida(rs.getTime("hora_salida").toLocalTime());
                    viaje.setPrecioBase(rs.getBigDecimal("precio_base"));
                    viaje.setEstadoViaje(rs.getString("estado_viaje"));

                    lista.add(viaje);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
}