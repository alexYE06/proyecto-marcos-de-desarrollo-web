package pe.transporte.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import pe.transporte.model.Usuario;

import java.io.IOException;

@WebServlet("/admin/dashboard")
public class AdminDashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Usuario usuario = (session != null) ? (Usuario) session.getAttribute("usuarioLogueado") : null;

        // Filtro de seguridad por rol
        if (usuario != null && "ADMIN".equalsIgnoreCase(usuario.getRol())) {
            req.getRequestDispatcher("/WEB-INF/views/admin/panel-control.jsp").forward(req, resp);
        } else {
            resp.sendRedirect(req.getContextPath() + "/login.jsp?error=acceso_denegado");
        }
    }
}