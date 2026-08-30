package pe.transporte.dao;

import pe.transporte.config.ConexionBD;
import pe.transporte.model.Asiento;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AsientoDAO {

    public List<Asiento> listarPorBus(int idBus) {
        List<Asiento> lista = new ArrayList<>();
        String sql = "SELECT * FROM asiento WHERE id_bus = ? ORDER BY piso, numero_asiento";

        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, idBus);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Asiento a = new Asiento(
                        rs.getInt("id_asiento"),
                        rs.getInt("id_bus"),
                        rs.getInt("numero_asiento"),
                        rs.getInt("piso"),
                        rs.getString("tipo_asiento"),
                        rs.getInt("posicion_x"),
                        rs.getInt("posicion_y")
                    );
                    lista.add(a);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
}