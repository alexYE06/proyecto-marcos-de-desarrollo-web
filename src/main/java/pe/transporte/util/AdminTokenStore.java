package pe.transporte.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guarda en memoria los tokens de las sesiones de administrador.
 * Si reinicias el servidor, los administradores deben volver a iniciar sesión.
 */
@Component
public class AdminTokenStore {

    private static final Duration DURACION = Duration.ofHours(8);

    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, Instant> tokens = new ConcurrentHashMap<>();

    public String crear() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.put(token, Instant.now().plus(DURACION));
        return token;
    }

    public boolean esValido(String token) {
        if (token == null) return false;
        Instant expira = tokens.get(token);
        if (expira == null) return false;
        if (Instant.now().isAfter(expira)) {
            tokens.remove(token);
            return false;
        }
        return true;
    }

    public void revocar(String token) {
        if (token != null) tokens.remove(token);
    }
}
