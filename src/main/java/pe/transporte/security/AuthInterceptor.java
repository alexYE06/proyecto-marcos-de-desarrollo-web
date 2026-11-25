package pe.transporte.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import pe.transporte.exception.AccesoDenegadoException;
import pe.transporte.exception.NoAutorizadoException;
import pe.transporte.model.enums.RolUsuario;

import java.util.Arrays;
import java.util.Optional;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATRIBUTO_SESION = "SESION_ACTUAL";

    private final SessionTokenStore tokenStore;

    public AuthInterceptor(SessionTokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = extraerToken(request);
        Optional<SesionUsuario> sesionOpt = tokenStore.obtenerSesion(token);
        sesionOpt.ifPresent(sesion -> request.setAttribute(ATRIBUTO_SESION, sesion));

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequiereRol anotacion = handlerMethod.getMethodAnnotation(RequiereRol.class);
        if (anotacion == null) {
            anotacion = handlerMethod.getBeanType().getAnnotation(RequiereRol.class);
        }

        if (anotacion == null) {
            return true;
        }

        SesionUsuario sesion = sesionOpt.orElseThrow(() ->
                new NoAutorizadoException("Debes iniciar sesión o tu sesión ha expirado."));

        RolUsuario[] rolesPermitidos = anotacion.value();
        boolean autorizado = Arrays.asList(rolesPermitidos).contains(sesion.rol());
        if (!autorizado) {
            throw new AccesoDenegadoException(
                    "Acceso denegado: el cargo " + sesion.rol() + " no tiene permisos para esta función.");
        }

        return true;
    }

    public static String extraerToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.toLowerCase().startsWith("bearer ")) {
            return auth.substring(7).trim();
        }
        String custom = request.getHeader("X-Auth-Token");
        if (custom != null && !custom.isBlank()) {
            return custom.trim();
        }
        String adminLegacy = request.getHeader("X-Admin-Token");
        if (adminLegacy != null && !adminLegacy.isBlank()) {
            return adminLegacy.trim();
        }
        return null;
    }

    public static SesionUsuario sesionObligatoria(HttpServletRequest request) {
        Object attr = request.getAttribute(ATRIBUTO_SESION);
        if (attr instanceof SesionUsuario sesion) {
            return sesion;
        }
        throw new NoAutorizadoException("Sesión no válida o expirada.");
    }

    public static Optional<SesionUsuario> sesionOpcional(HttpServletRequest request) {
        Object attr = request.getAttribute(ATRIBUTO_SESION);
        if (attr instanceof SesionUsuario sesion) {
            return Optional.of(sesion);
        }
        return Optional.empty();
    }
}
