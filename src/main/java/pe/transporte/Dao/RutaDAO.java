package pe.transporte.dao;

import pe.transporte.config.ConexionBD;
import pe.transporte.model.Ruta;
import pe.transporte.model.Terminal;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RutaDAO {

    public List<Ruta> listarTodas() {
        List<Ruta> lista = new ArrayList<>();
        String sql = "SELECT r.id_ruta, r.duracion_estimada_horas, "
                   + "t1.id_terminal as id_orig, t1.nombre as nom_orig, t1.ciudad as ciu_orig, "
                   + "t2.id_terminal as id_dest, t2.nombre as nom_dest, t2.ciudad as ciu_dest "
                   + "FROM ruta r "
                   + "INNER JOIN terminal t1 ON r.id_terminal_origen = t1.id_terminal "
                   + "INNER JOIN terminal t2 ON r.id_terminal_destino = t2.id_terminal";

        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Terminal orig = new Terminal(rs.getInt("id_orig"), rs.getString("nom_orig"), rs.getString("ciu_orig"), null);
                Terminal dest = new Terminal(rs.getInt("id_dest"), rs.getString("nom_dest"), rs.getString("ciu_dest"), null);
                
                Ruta r = new Ruta(rs.getInt("id_ruta"), orig, dest, rs.getBigDecimal("duracion_estimada_horas"));
                lista.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
}