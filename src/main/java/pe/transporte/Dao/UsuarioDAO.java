package pe.transporte.dao;

import pe.transporte.config.ConexionBD;
import pe.transporte.model.Usuario;

import java.sql.*;

public class UsuarioDAO {

    public boolean registrar(Usuario u) {
        String sql = "INSERT INTO usuario (nombre, apellido, dni_pasaporte, correo, telefono, contrasena_hash, rol) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getApellido());
            ps.setString(3, u.getDniPasaporte());
            ps.setString(4, u.getCorreo());
            ps.setString(5, u.getTelefono());
            ps.setString(6, u.getContrasenaHash());
            ps.setString(7, u.getRol() != null ? u.getRol() : "CLIENTE");

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Usuario obtenerPorCorreo(String correo) {
        String sql = "SELECT * FROM usuario WHERE correo = ?";
        Usuario u = null;

        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, correo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    u = new Usuario();
                    u.setIdUsuario(rs.getInt("id_usuario"));
                    u.setNombre(rs.getString("nombre"));
                    u.setApellido(rs.getString("apellido"));
                    u.setDniPasaporte(rs.getString("dni_pasaporte"));
                    u.setCorreo(rs.getString("correo"));
                    u.setTelefono(rs.getString("telefono"));
                    u.setContrasenaHash(rs.getString("contrasena_hash"));
                    u.setRol(rs.getString("rol"));
                    u.setFechaRegistro(rs.getTimestamp("fecha_registro").toLocalDateTime());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return u;
    }
}