package pe.transporte.dao;

import pe.transporte.config.ConexionBD;
import pe.transporte.model.Bus;

import java.sql.*;

public class BusDAO {

    public Bus obtenerPorId(int idBus) {
        String sql = "SELECT * FROM bus WHERE id_bus = ?";
        Bus b = null;

        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, idBus);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    b = new Bus(
                        rs.getInt("id_bus"),
                        rs.getString("placa"),
                        rs.getString("numero_interno"),
                        rs.getString("modelo"),
                        rs.getString("marca"),
                        rs.getInt("pisos"),
                        rs.getInt("capacidad_asientos"),
                        rs.getString("estado")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return b;
    }
}