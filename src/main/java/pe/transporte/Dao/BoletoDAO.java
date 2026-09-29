package pe.transporte.dao;

import pe.transporte.config.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BoletoDAO {

    public List<Integer> obtenerAsientosOcupados(int idViaje) {
        List<Integer> ocupados = new ArrayList<>();
        String sql = "SELECT b.id_asiento FROM boleto b "
                + "INNER JOIN venta v ON b.id_venta = v.id_venta "
                + "WHERE b.id_viaje = ? AND v.estado_pago IN ('RESERVADO', 'PAGADO')";

        try (Connection cn = ConexionBD.getConexion(); PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, idViaje);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ocupados.add(rs.getInt("id_asiento"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ocupados;
    }
}
