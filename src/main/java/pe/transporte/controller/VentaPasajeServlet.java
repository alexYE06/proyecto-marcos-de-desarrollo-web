package pe.transporte.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import pe.transporte.dao.VentaDAO;
import pe.transporte.model.*;
import pe.transporte.util.GeneradorQR;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@WebServlet("/procesar-venta")
public class VentaPasajeServlet extends HttpServlet {

    private VentaDAO ventaDAO = new VentaDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuarioLogueado == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }

        try {
            int idViaje = Integer.parseInt(req.getParameter("idViaje"));
            String[] asientosSeleccionados = req.getParameterValues("asientos"); // IDs de asientos
            BigDecimal precioUnitario = new BigDecimal(req.getParameter("precioBase"));

            // 1. Construir Cabecera de Venta
            Venta venta = new Venta();
            venta.setIdVenta(UUID.randomUUID().toString());
            venta.setCodigoReserva("TR-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
            venta.setUsuario(usuarioLogueado);
            venta.setMontoTotal(precioUnitario.multiply(new BigDecimal(asientosSeleccionados.length)));
            venta.setEstadoPago("RESERVADO");
            venta.setExpiraEn(LocalDateTime.now().plusMinutes(10)); // 10 minutos para pagar

            // 2. Construir Lista de Boletos
            List<Boleto> boletos = new ArrayList<>();
            for (String idAsientoStr : asientosSeleccionados) {
                int idAsiento = Integer.parseInt(idAsientoStr);
                
                String nombrePasajero = req.getParameter("pasajero_nombre_" + idAsiento);
                String dniPasajero = req.getParameter("pasajero_dni_" + idAsiento);

                Boleto b = new Boleto();
                b.setCodigoBoleto("TK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                b.setVenta(venta);
                
                Viaje viajeObj = new Viaje();
                viajeObj.setIdViaje(idViaje);
                b.setViaje(viajeObj);

                Asiento asientoObj = new Asiento();
                asientoObj.setIdAsiento(idAsiento);
                b.setAsiento(asientoObj);

                b.setPasajeroNombre(nombrePasajero);
                b.setPasajeroDni(dniPasajero);
                b.setPrecio(precioUnitario);
                b.setCodigoQr(GeneradorQR.generarTextoQR(b.getCodigoBoleto(), dniPasajero));

                boletos.add(b);
            }

            // 3. Persistir en la BD con transacción SQL
            if (ventaDAO.registrarVentaCompleta(venta, boletos)) {
                req.setAttribute("venta", venta);
                req.setAttribute("boletos", boletos);
                req.getRequestDispatcher("/WEB-INF/views/cliente/checkout-pago.jsp").forward(req, resp);
            } else {
                req.setAttribute("error", "Uno o más asientos seleccionados ya no se encuentran disponibles.");
                resp.sendRedirect(req.getContextPath() + "/index.jsp");
            }

        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/index.jsp");
        }
    }
}