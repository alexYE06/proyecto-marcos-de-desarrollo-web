package pe.transporte.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Encriptación de contraseñas con BCrypt (Spring Security) y compatibilidad automática
 * de lectura para migrar cuentas previas en PBKDF2 o SHA-256 sin pérdida de acceso.
 */
public final class EncriptadorPassword {

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder(10);

    private EncriptadorPassword() {
    }

    public static String encriptar(String password) {
        if (password == null) return null;
        return BCRYPT.encode(password);
    }

    public static boolean verificar(String passwordPlano, String hashAlmacenado) {
        if (passwordPlano == null || hashAlmacenado == null || hashAlmacenado.isBlank()) {
            return false;
        }
        if (esBCrypt(hashAlmacenado)) {
            return BCRYPT.matches(passwordPlano, hashAlmacenado);
        }
        if (hashAlmacenado.startsWith("pbkdf2$")) {
            return verificarPbkdf2(passwordPlano, hashAlmacenado);
        }
        String sha256 = sha256Legado(passwordPlano);
        return MessageDigest.isEqual(
                hashAlmacenado.getBytes(StandardCharsets.UTF_8),
                sha256.getBytes(StandardCharsets.UTF_8)
        );
    }

    public static boolean requiereActualizacion(String hashAlmacenado) {
        return !esBCrypt(hashAlmacenado);
    }

    private static boolean esBCrypt(String hash) {
        return hash != null && (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));
    }

    private static boolean verificarPbkdf2(String passwordPlano, String hashAlmacenado) {
        String[] partes = hashAlmacenado.split("\\$");
        if (partes.length != 4) return false;
        try {
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            PBEKeySpec spec = new PBEKeySpec(passwordPlano.toCharArray(), salt, iteraciones, 256);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] calculado = skf.generateSecret(spec).getEncoded();
            return MessageDigest.isEqual(esperado, calculado);
        } catch (Exception e) {
            return false;
        }
    }

    private static String sha256Legado(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Error al verificar hash legado", ex);
        }
    }
}
