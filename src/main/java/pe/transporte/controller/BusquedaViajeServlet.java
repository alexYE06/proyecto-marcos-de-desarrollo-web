package pe.transporte.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import pe.transporte.dao.ViajeDAO;
import pe.transporte.model.Viaje;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@WebServlet("/buscar-viajes")
public class BusquedaViajeServlet extends HttpServlet {

    private ViajeDAO viajeDAO = new ViajeDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            int idOrigen = Integer.parseInt(req.getParameter("origen"));
            int idDestino = Integer.parseInt(req.getParameter("destino"));
            LocalDate fecha = LocalDate.parse(req.getParameter("fecha"));

            List<Viaje> viajesEncontrados = viajeDAO.buscarViajes(idOrigen, idDestino, fecha);

            req.setAttribute("listaViajes", viajesEncontrados);
            req.setAttribute("fechaBusqueda", fecha);
            
            req.getRequestDispatcher("/WEB-INF/views/cliente/catalogo-viajes.jsp").forward(req, resp);

        } catch (Exception e) {
            req.setAttribute("error", "Parámetros de búsqueda inválidos.");
            req.getRequestDispatcher("/index.jsp").forward(req, resp);
        }
    }
}