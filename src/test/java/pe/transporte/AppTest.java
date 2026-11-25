package pe.transporte;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import pe.transporte.controller.AdminGestionController;
import pe.transporte.controller.EmbarqueController;
import pe.transporte.controller.VendedorPosController;
import pe.transporte.dto.AdminOperacionDtos.ValidarAbordajeRequest;
import pe.transporte.dto.VentaDtos.EmitirVentaRequest;
import pe.transporte.dto.VentaDtos.PasajeroItemRequest;
import pe.transporte.exception.AccesoDenegadoException;
import pe.transporte.exception.NoAutorizadoException;
import pe.transporte.exception.ReglaNegocioException;
import pe.transporte.model.*;
import pe.transporte.model.enums.CanalVenta;
import pe.transporte.model.enums.RolUsuario;
import pe.transporte.repository.*;
import pe.transporte.security.AuthInterceptor;
import pe.transporte.security.SesionUsuario;
import pe.transporte.security.SessionTokenStore;
import pe.transporte.service.AuditoriaService;
import pe.transporte.service.CatalogoViajeService;
import pe.transporte.service.EmbarqueService;
import pe.transporte.service.VentaPasajeService;
import pe.transporte.util.EncriptadorPassword;
import pe.transporte.util.GeneradorQR;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {

    @Test
    void encriptadorUsaSaltAleatorioYVerificaCompatibilidadSha256Legado() throws Exception {
        String h1 = EncriptadorPassword.encriptar("ClaveSegura2026!");
        String h2 = EncriptadorPassword.encriptar("ClaveSegura2026!");

        assertNotEquals(h1, h2, "Dos hashes de la misma clave deben diferir gracias al salt aleatorio");
        assertTrue(h1.startsWith("$2a$"), "Debe encriptar con BCrypt de Spring Security");
        assertTrue(EncriptadorPassword.verificar("ClaveSegura2026!", h1));
        assertFalse(EncriptadorPassword.verificar("OtraClave", h1));
        assertFalse(EncriptadorPassword.requiereActualizacion(h1));

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest("legado123".getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            String h = Integer.toHexString(0xff & b);
            if (h.length() == 1) hex.append('0');
            hex.append(h);
        }
        assertTrue(EncriptadorPassword.verificar("legado123", hex.toString()));
        assertTrue(EncriptadorPassword.requiereActualizacion(hex.toString()));
    }

    @Test
    void generadorQrCreaImagenBase64YExtraeCodigoBoleto() {
        String qr = GeneradorQR.generarQRBoleto("TK-9X8Y7Z", 12, 4, "72481920");
        assertNotNull(qr);
        assertTrue(qr.startsWith("data:image/png;base64,"));

        String payload = GeneradorQR.construirPayloadBoleto("TK-9X8Y7Z", 12, 4, "72481920");
        assertEquals("TK-9X8Y7Z", GeneradorQR.extraerCodigoBoleto(payload));
        assertEquals("TK-9X8Y7Z", GeneradorQR.extraerCodigoBoleto("  TK-9X8Y7Z "));
    }

    @Test
    void interceptorRbacBloqueaEscalacionDePrivilegiosEntreCargos() throws Exception {
        SessionTokenStore tokenStore = new SessionTokenStore();
        AuthInterceptor interceptor = new AuthInterceptor(tokenStore);

        Usuario vendedor = crearUsuario(2, "Lucía", "VENDEDOR");
        Usuario embarque = crearUsuario(3, "Jorge", "EMBARQUE");
        Usuario admin = crearUsuario(1, "Carlos", "ADMIN");

        SesionUsuario sesionVendedor = tokenStore.crearSesion(vendedor);
        SesionUsuario sesionEmbarque = tokenStore.crearSesion(embarque);
        SesionUsuario sesionAdmin = tokenStore.crearSesion(admin);

        AdminGestionController adminCtrl = new AdminGestionController(null, null, null);
        VendedorPosController posCtrl = new VendedorPosController(null);
        EmbarqueController embCtrl = new EmbarqueController(null);

        HandlerMethod endpointAdmin = new HandlerMethod(adminCtrl, AdminGestionController.class.getMethod("obtenerDashboard"));
        Method metodoArqueo = VendedorPosController.class.getMethod("obtenerArqueoCaja", jakarta.servlet.http.HttpServletRequest.class);
        HandlerMethod endpointPos = new HandlerMethod(posCtrl, metodoArqueo);
        HandlerMethod endpointEmb = new HandlerMethod(embCtrl, EmbarqueController.class.getMethod("listarViajesOperativos", String.class));

        // 1. Sin token -> 401 NoAutorizadoException
        assertThrows(NoAutorizadoException.class, () ->
                interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), endpointAdmin));

        // 2. VENDEDOR no puede entrar a /api/admin -> 403 AccesoDenegadoException
        assertThrows(AccesoDenegadoException.class, () ->
                interceptor.preHandle(requestConToken(sesionVendedor.token()), new MockHttpServletResponse(), endpointAdmin));

        // 3. VENDEDOR sí entra a su POS, pero NO a Embarque
        assertTrue(interceptor.preHandle(requestConToken(sesionVendedor.token()), new MockHttpServletResponse(), endpointPos));
        assertThrows(AccesoDenegadoException.class, () ->
                interceptor.preHandle(requestConToken(sesionVendedor.token()), new MockHttpServletResponse(), endpointEmb));

        // 4. EMBARQUE sí entra a Embarque, pero NO a POS ni a Admin
        assertTrue(interceptor.preHandle(requestConToken(sesionEmbarque.token()), new MockHttpServletResponse(), endpointEmb));
        assertThrows(AccesoDenegadoException.class, () ->
                interceptor.preHandle(requestConToken(sesionEmbarque.token()), new MockHttpServletResponse(), endpointPos));

        // 5. ADMIN tiene acceso jerárquico de supervisión a todos los paneles
        assertTrue(interceptor.preHandle(requestConToken(sesionAdmin.token()), new MockHttpServletResponse(), endpointAdmin));
        assertTrue(interceptor.preHandle(requestConToken(sesionAdmin.token()), new MockHttpServletResponse(), endpointPos));
        assertTrue(interceptor.preHandle(requestConToken(sesionAdmin.token()), new MockHttpServletResponse(), endpointEmb));

        // 6. Revocación por desactivación invalida el token al instante
        tokenStore.revocarSesionesDeUsuario(vendedor.getIdUsuario());
        assertThrows(NoAutorizadoException.class, () ->
                interceptor.preHandle(requestConToken(sesionVendedor.token()), new MockHttpServletResponse(), endpointPos));
    }

    @Test
    void vendedorSoloSolicitaAnulacionYAdminEjecutaLiberacion() {
        Usuario vendedor = crearUsuario(2, "Lucía", "VENDEDOR");
        Usuario admin = crearUsuario(1, "Carlos", "ADMIN");

        Ruta ruta = new Ruta();
        ruta.setOrigen(new Terminal(1, "Javier Prado", "Lima", null));
        ruta.setDestino(new Terminal(2, "Terrapuerto Ica", "Ica", null));

        Viaje viaje = new Viaje();
        viaje.setIdViaje(10);
        viaje.setRuta(ruta);
        viaje.setPrecio(50.0);
        viaje.setAsientosDisponibles(64);
        viaje.setFechaHoraSalida(LocalDateTime.now().plusDays(1));
        viaje.setEstado("PROGRAMADO");

        Map<Integer, Venta> almacenVentas = new HashMap<>();
        List<Boleto> almacenBoletos = new ArrayList<>();

        VentaRepository ventaRepo = crearStub(VentaRepository.class, (metodo, args) -> switch (metodo) {
            case "findByEstadoPagoAndExpiraEnBefore" -> List.of();
            case "count" -> (long) almacenVentas.size();
            case "saveAndFlush", "save" -> {
                Venta v = (Venta) args[0];
                if (v.getIdVenta() == null) v.setIdVenta(99);
                almacenVentas.put(v.getIdVenta(), v);
                almacenBoletos.clear();
                almacenBoletos.addAll(v.getBoletos());
                yield v;
            }
            case "findById" -> Optional.ofNullable(almacenVentas.get((Integer) args[0]));
            default -> List.of();
        });

        BoletoRepository boletoRepo = crearStub(BoletoRepository.class, (metodo, args) ->
                "findByViaje_IdViajeOrderByNumeroAsientoAsc".equals(metodo) ? new ArrayList<>(almacenBoletos) : List.of());

        ViajeRepository viajeRepo = crearStub(ViajeRepository.class, (metodo, args) -> switch (metodo) {
            case "findById" -> Optional.of(viaje);
            case "save", "saveAndFlush" -> args[0];
            default -> List.of();
        });

        UsuarioRepository usuarioRepo = crearStub(UsuarioRepository.class, (metodo, args) -> {
            if ("findById".equals(metodo)) {
                Integer id = (Integer) args[0];
                return id.equals(1) ? Optional.of(admin) : Optional.of(vendedor);
            }
            return Optional.empty();
        });

        CuponRepository cuponRepo = crearStub(CuponRepository.class, (m, a) -> Optional.empty());
        TerminalRepository termRepo = crearStub(TerminalRepository.class, (m, a) -> List.of());
        RutaRepository rutaRepo = crearStub(RutaRepository.class, (m, a) -> List.of());

        List<AuditoriaLog> logsGuardados = new ArrayList<>();
        AuditoriaLogRepository auditRepo = crearStub(AuditoriaLogRepository.class, (m, a) -> {
            if ("save".equals(m)) {
                logsGuardados.add((AuditoriaLog) a[0]);
                return a[0];
            }
            return logsGuardados;
        });
        AuditoriaService auditoriaSvc = new AuditoriaService(auditRepo);

        CatalogoViajeService catalogoSvc = new CatalogoViajeService(termRepo, rutaRepo, viajeRepo, ventaRepo, boletoRepo);
        VentaPasajeService service = new VentaPasajeService(ventaRepo, boletoRepo, viajeRepo, usuarioRepo, cuponRepo, catalogoSvc, auditoriaSvc);

        SesionUsuario sesionVend = new SesionUsuario("tok-v", 2, "Lucía", "v@u.pe", RolUsuario.VENDEDOR, Instant.now().plusSeconds(3600));
        SesionUsuario sesionAdm = new SesionUsuario("tok-a", 1, "Carlos", "a@u.pe", RolUsuario.ADMIN, Instant.now().plusSeconds(3600));

        EmitirVentaRequest reqVenta = new EmitirVentaRequest(
                null, 10, "BOLETA", null, null,
                "Juan Pérez", "DNI", "70112233", "juan@correo.pe", "999000111",
                "EFECTIVO", null,
                List.of(new PasajeroItemRequest(3, "DNI", "70112233", "Juan", "Pérez", "juan@correo.pe", "999000111"))
        );

        var emitida = service.emitirVenta(reqVenta, CanalVenta.VENTANILLA, sesionVend);
        assertEquals("PAGADO", emitida.estadoPago());
        assertEquals("85.00", emitida.montoTotal().toPlainString());
        assertEquals(63, viaje.getAsientosDisponibles(), "El asiento 3 debe descontarse de los disponibles");
        assertNotNull(emitida.boletos().get(0).codigoQr());

        // Intento de volver a vender el mismo asiento 3 -> debe fallar por colisión
        assertThrows(ReglaNegocioException.class, () ->
                service.emitirVenta(reqVenta, CanalVenta.VENTANILLA, sesionVend));

        // El administrador NO puede anular directamente sin motivo de auditoría
        assertThrows(ReglaNegocioException.class, () ->
                service.aprobarOEjecutarAnulacionAdmin(99, "   ", sesionAdm));

        // El vendedor solicita anulación -> pasa a ANULACION_SOLICITADA (asiento sigue retenido hasta aprobación Admin)
        var solicitada = service.solicitarAnulacionPorVendedor(99, "Error al digitar DNI", sesionVend);
        assertEquals("ANULACION_SOLICITADA", solicitada.estadoPago());
        assertEquals(63, viaje.getAsientosDisponibles());

        // El administrador aprueba la anulación -> pasa a ANULADO, libera el asiento y registra auditoría inmutable
        var anulada = service.aprobarOEjecutarAnulacionAdmin(99, null, sesionAdm);
        assertEquals("ANULADO", anulada.estadoPago());
        assertEquals(64, viaje.getAsientosDisponibles(), "Tras la anulación del Admin el asiento queda libre");
        assertTrue(logsGuardados.stream().anyMatch(l -> "ANULAR_VENTA".equals(l.getAccion()) && "ADMIN".equals(l.getActorRol())),
                "La anulación del administrador debe quedar registrada en la bitácora de auditoría");
    }

    @Test
    void embarqueValidaQrYRechazaDobleAbordaje() {
        Usuario inspector = crearUsuario(3, "Jorge", "EMBARQUE");

        Ruta ruta = new Ruta();
        ruta.setOrigen(new Terminal(1, "Lima", "Lima", null));
        ruta.setDestino(new Terminal(2, "Arequipa", "Arequipa", null));
        Viaje viaje = new Viaje();
        viaje.setIdViaje(5);
        viaje.setRuta(ruta);
        viaje.setEstado("EMBARCANDO");

        Venta venta = new Venta();
        venta.setEstadoPago("PAGADO");

        Boleto boleto = new Boleto();
        boleto.setIdBoleto(50);
        boleto.setCodigoBoleto("TK-ABC123");
        boleto.setViaje(viaje);
        boleto.setVenta(venta);
        boleto.setNumeroAsiento(12);
        boleto.setPiso(1);
        boleto.setCategoriaAsiento("VIP Ejecutivo 160°");
        boleto.setPasajeroNombre("María");
        boleto.setPasajeroApellido("Gómez");
        boleto.setPasajeroDocumento("74889900");
        boleto.setEstadoAbordaje("PENDIENTE");

        BoletoRepository boletoRepo = crearStub(BoletoRepository.class, (metodo, args) -> switch (metodo) {
            case "findByCodigoBoletoIgnoreCase" -> Optional.of(boleto);
            case "saveAndFlush" -> args[0];
            default -> List.of();
        });
        UsuarioRepository usuarioRepo = crearStub(UsuarioRepository.class, (m, a) -> Optional.of(inspector));
        ViajeRepository viajeRepo = crearStub(ViajeRepository.class, (m, a) -> Optional.of(viaje));
        VentaRepository ventaRepo = crearStub(VentaRepository.class, (m, a) -> List.of());
        TerminalRepository termRepo = crearStub(TerminalRepository.class, (m, a) -> List.of());
        RutaRepository rutaRepo = crearStub(RutaRepository.class, (m, a) -> List.of());

        CatalogoViajeService catalogoSvc = new CatalogoViajeService(termRepo, rutaRepo, viajeRepo, ventaRepo, boletoRepo);
        EmbarqueService embarqueService = new EmbarqueService(viajeRepo, boletoRepo, usuarioRepo, catalogoSvc, null);

        SesionUsuario sesionInsp = new SesionUsuario("tok-e", 3, "Jorge", "e@u.pe", RolUsuario.EMBARQUE, Instant.now().plusSeconds(3600));
        String qrLeido = GeneradorQR.construirPayloadBoleto("TK-ABC123", 5, 12, "74889900");

        var ok = embarqueService.validarYRegistrarAbordaje(new ValidarAbordajeRequest(5, qrLeido, null), sesionInsp);
        assertEquals("ABORDADO", boleto.getEstadoAbordaje());
        assertNotNull(ok.get("boleto"));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                embarqueService.validarYRegistrarAbordaje(new ValidarAbordajeRequest(5, qrLeido, null), sesionInsp));
        assertTrue(ex.getMessage().contains("YA UTILIZADO"));
    }

    @FunctionalInterface
    private interface ManejadorStub {
        Object invocar(String metodo, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T crearStub(Class<T> interfaz, ManejadorStub manejador) {
        return (T) Proxy.newProxyInstance(
                interfaz.getClassLoader(),
                new Class<?>[]{interfaz},
                (proxy, method, args) -> {
                    if ("toString".equals(method.getName())) return interfaz.getSimpleName() + "Stub";
                    if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                    if ("equals".equals(method.getName())) return proxy == args[0];
                    return manejador.invocar(method.getName(), args);
                }
        );
    }

    private Usuario crearUsuario(Integer id, String nombre, String rol) {
        Usuario u = new Usuario();
        u.setIdUsuario(id);
        u.setNombre(nombre);
        u.setApellido("Prueba");
        u.setCorreo(nombre.toLowerCase() + "@urbanride.pe");
        u.setRol(rol);
        u.setActivo(true);
        return u;
    }

    private MockHttpServletRequest requestConToken(String token) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        return req;
    }
}
