package pe.transporte.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import pe.transporte.dao.UsuarioDAO;
import pe.transporte.model.Usuario;
import pe.transporte.util.EncriptadorPassword;

import java.io.IOException;

@WebServlet("/auth")
public class AuthServlet extends HttpServlet {

    private UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if ("logout".equals(action)) {
            HttpSession session = req.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
        } else {
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");

        if ("login".equals(action)) {
            String correo = req.getParameter("correo");
            String password = req.getParameter("password");

            Usuario user = usuarioDAO.obtenerPorCorreo(correo);

            // Validación de credenciales con BCrypt
            if (user != null && EncriptadorPassword.verificar(password, user.getContrasenaHash())) {
                HttpSession session = req.getSession();
                session.setAttribute("usuarioLogueado", user);

                if ("ADMIN".equalsIgnoreCase(user.getRol())) {
                    resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
                } else {
                    resp.sendRedirect(req.getContextPath() + "/index.jsp");
                }
            } else {
                req.setAttribute("error", "Correo o contraseña incorrectos");
                req.getRequestDispatcher("/login.jsp").forward(req, resp);
            }
        } else if ("registro".equals(action)) {
            Usuario nuevo = new Usuario();
            nuevo.setNombre(req.getParameter("nombre"));
            nuevo.setApellido(req.getParameter("apellido"));
            nuevo.setDniPasaporte(req.getParameter("dni"));
            nuevo.setCorreo(req.getParameter("correo"));
            nuevo.setTelefono(req.getParameter("telefono"));
            
            // Encriptar contraseña antes de guardar
            String hash = EncriptadorPassword.encriptar(req.getParameter("password"));
            nuevo.setContrasenaHash(hash);
            nuevo.setRol("CLIENTE");

            if (usuarioDAO.registrar(nuevo)) {
                resp.sendRedirect(req.getContextPath() + "/login.jsp?registro=exito");
            } else {
                req.setAttribute("error", "No se pudo registrar. El correo o DNI ya existen.");
                req.getRequestDispatcher("/login.jsp").forward(req, resp);
            }
        }
    }
}