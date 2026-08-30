package pe.transporte.dao;

import pe.transporte.config.ConexionBD;
import pe.transporte.model.Boleto;
import pe.transporte.model.Venta;

import java.sql.*;
import java.util.List;

public class VentaDAO {

    public boolean registrarVentaCompleta(Venta venta, List<Boleto> boletos) {
        String sqlVenta = "INSERT INTO venta (id_venta, codigo_reserva, id_usuario, monto_total, estado_pago, expira_en) "
                        + "VALUES (?, ?, ?, ?, ?, ?)";
        
        String sqlBoleto = "INSERT INTO boleto (codigo_boleto, id_venta, id_viaje, id_asiento, pasajero_nombre, pasajero_dni, precio, codigo_qr) "
                         + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Connection cn = null;
        try {
            cn = ConexionBD.getConexion();
            cn.setAutoCommit(false); // Iniciar Transacción SQL

            // 1. Insertar Venta
            try (PreparedStatement psVenta = cn.prepareStatement(sqlVenta)) {
                psVenta.setString(1, venta.getIdVenta());
                psVenta.setString(2, venta.getCodigoReserva());
                psVenta.setInt(3, venta.getUsuario().getIdUsuario());
                psVenta.setBigDecimal(4, venta.getMontoTotal());
                psVenta.setString(5, venta.getEstadoPago());
                psVenta.setTimestamp(6, Timestamp.valueOf(venta.getExpiraEn()));
                psVenta.executeUpdate();
            }

            // 2. Insertar Detalle de Boletos
            try (PreparedStatement psBoleto = cn.prepareStatement(sqlBoleto)) {
                for (Boleto b : boletos) {
                    psBoleto.setString(1, b.getCodigoBoleto());
                    psBoleto.setString(2, venta.getIdVenta());
                    psBoleto.setInt(3, b.getViaje().getIdViaje());
                    psBoleto.setInt(4, b.getAsiento().getIdAsiento());
                    psBoleto.setString(5, b.getPasajeroNombre());
                    psBoleto.setString(6, b.getPasajeroDni());
                    psBoleto.setBigDecimal(7, b.getPrecio());
                    psBoleto.setString(8, b.getCodigoQr());
                    psBoleto.addBatch();
                }
                psBoleto.executeBatch();
            }

            cn.commit(); // Confirmar transacción en PostgreSQL
            return true;

        } catch (SQLException e) {
            if (cn != null) {
                try {
                    cn.rollback(); // Cancelar cambios si ocurre un error
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (cn != null) {
                try {
                    cn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}