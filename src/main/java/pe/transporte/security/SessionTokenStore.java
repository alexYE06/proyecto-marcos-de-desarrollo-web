package pe.transporte.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pe.transporte.model.Usuario;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Emite tokens JWT firmados (HS256) y mantiene control de revocación inmediata (logout / bloqueo por Admin).
 */
@Component
public class SessionTokenStore {

    private final JwtService jwtService;
    private final ConcurrentHashMap<String, Instant> jtisRevocados = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Long> versionPorUsuario = new ConcurrentHashMap<>();

    public SessionTokenStore() {
        this(new JwtService("UrbanRidePeru2026ClaveSecretaJwtHmacSha256SeguridadWebUTP!", 8));
    }

    @Autowired
    public SessionTokenStore(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public SesionUsuario crearSesion(Usuario usuario) {
        long ver = versionPorUsuario.getOrDefault(usuario.getIdUsuario(), 1L);
        return jwtService.generarTokenJwt(usuario, ver);
    }

    public Optional<SesionUsuario> obtenerSesion(String token) {
        return jwtService.verificarYDecodificar(token)
                .filter(datos -> !jtisRevocados.containsKey(datos.jti()))
                .filter(datos -> {
                    long verActual = versionPorUsuario.getOrDefault(datos.sesion().idUsuario(), 1L);
                    return datos.versionUsuario() == verActual;
                })
                .map(JwtService.DatosJwtVerificado::sesion);
    }

    public void revocarToken(String token) {
        jwtService.verificarYDecodificar(token)
                .ifPresent(datos -> jtisRevocados.put(datos.jti(), datos.sesion().expiraEn()));
    }

    public void revocarSesionesDeUsuario(Integer idUsuario) {
        if (idUsuario == null) return;
        versionPorUsuario.merge(idUsuario, 2L, (prev, inc) -> prev + 1L);
    }
}
