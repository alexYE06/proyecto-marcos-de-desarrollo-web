package pe.transporte.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import pe.transporte.dao.AsientoDAO;
import pe.transporte.dao.BoletoDAO;

import java.io.IOException;
import java.util.List;

@WebServlet("/reserva-asientos")
public class ReservaAsientoServlet extends HttpServlet {

    private AsientoDAO asientoDAO = new AsientoDAO();
    private BoletoDAO boletoDAO = new BoletoDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            int idViaje = Integer.parseInt(req.getParameter("idViaje"));
            int idBus = Integer.parseInt(req.getParameter("idBus"));

            // Asientos configurados en el bus y asientos tomados
            var mapaAsientos = asientoDAO.listarPorBus(idBus);
            List<Integer> asientosOcupados = boletoDAO.obtenerAsientosOcupados(idViaje);

            req.setAttribute("idViaje", idViaje);
            req.setAttribute("mapaAsientos", mapaAsientos);
            req.setAttribute("asientosOcupados", asientosOcupados);

            req.getRequestDispatcher("/WEB-INF/views/cliente/seleccion-asientos.jsp").forward(req, resp);

        } catch (Exception e) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp");
        }
    }
}