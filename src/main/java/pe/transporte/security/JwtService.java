package pe.transporte.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.transporte.model.Usuario;
import pe.transporte.model.enums.RolUsuario;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio emisor y validador de JSON Web Tokens (JWT - RFC 7519) firmados con HMAC-SHA256 (HS256).
 */
@Component
public class JwtService {

    private static final String ALGORITMO_HMAC = "HmacSHA256";
    private static final Base64.Encoder B64_URL_ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_URL_DEC = Base64.getUrlDecoder();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final byte[] secretBytes;
    private final Duration duracion;

    public JwtService(
            @Value("${security.jwt.secret:UrbanRidePeru2026ClaveSecretaJwtHmacSha256SeguridadWebUTP!}") String secret,
            @Value("${security.jwt.expiration-hours:8}") long horasExpiracion) {
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.duracion = Duration.ofHours(horasExpiracion > 0 ? horasExpiracion : 8);
    }

    public SesionUsuario generarTokenJwt(Usuario usuario, long versionSesionUsuario) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(duracion);
        RolUsuario rol = RolUsuario.desdeTexto(usuario.getRol());

        try {
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("jti", UUID.randomUUID().toString());
            payload.put("sub", usuario.getCorreo());
            payload.put("idUsuario", usuario.getIdUsuario());
            payload.put("nombre", usuario.getNombreCompleto());
            payload.put("rol", rol.name());
            payload.put("ver", versionSesionUsuario);
            payload.put("iat", ahora.getEpochSecond());
            payload.put("exp", expira.getEpochSecond());

            String headerB64 = B64_URL_ENC.encodeToString(MAPPER.writeValueAsBytes(header));
            String payloadB64 = B64_URL_ENC.encodeToString(MAPPER.writeValueAsBytes(payload));
            String contenido = headerB64 + "." + payloadB64;
            String firmaB64 = firmar(contenido);
            String jwt = contenido + "." + firmaB64;

            return new SesionUsuario(
                    jwt,
                    usuario.getIdUsuario(),
                    usuario.getNombreCompleto(),
                    usuario.getCorreo(),
                    rol,
                    expira
            );
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el token JWT", e);
        }
    }

    public Optional<DatosJwtVerificado> verificarYDecodificar(String jwt) {
        if (jwt == null || jwt.isBlank()) {
            return Optional.empty();
        }
        String[] partes = jwt.trim().split("\\.");
        if (partes.length != 3) {
            return Optional.empty();
        }

        try {
            String contenido = partes[0] + "." + partes[1];
            String firmaEsperada = firmar(contenido);
            if (!MessageDigest.isEqual(
                    firmaEsperada.getBytes(StandardCharsets.UTF_8),
                    partes[2].getBytes(StandardCharsets.UTF_8))) {
                return Optional.empty();
            }

            byte[] payloadJson = B64_URL_DEC.decode(partes[1]);
            Map<String, Object> claims = MAPPER.readValue(payloadJson, new TypeReference<>() {});

            long expEpoch = ((Number) claims.get("exp")).longValue();
            Instant expira = Instant.ofEpochSecond(expEpoch);
            if (Instant.now().isAfter(expira)) {
                return Optional.empty();
            }

            Integer idUsuario = ((Number) claims.get("idUsuario")).intValue();
            long version = claims.get("ver") instanceof Number n ? n.longValue() : 0L;
            String jti = String.valueOf(claims.getOrDefault("jti", ""));
            String correo = String.valueOf(claims.getOrDefault("sub", ""));
            String nombre = String.valueOf(claims.getOrDefault("nombre", ""));
            RolUsuario rol = RolUsuario.desdeTexto(String.valueOf(claims.getOrDefault("rol", "CLIENTE")));

            SesionUsuario sesion = new SesionUsuario(jwt.trim(), idUsuario, nombre, correo, rol, expira);
            return Optional.of(new DatosJwtVerificado(sesion, jti, version));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String firmar(String contenido) throws Exception {
        Mac mac = Mac.getInstance(ALGORITMO_HMAC);
        mac.init(new SecretKeySpec(secretBytes, ALGORITMO_HMAC));
        byte[] rawHmac = mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8));
        return B64_URL_ENC.encodeToString(rawHmac);
    }

    public record DatosJwtVerificado(
            SesionUsuario sesion,
            String jti,
            long versionUsuario
    ) {
    }
}
