package pe.transporte.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class EncriptadorPassword {

    public static String encriptar(String password) {
        if (password == null || password.trim().isEmpty()) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al encriptar la contraseña", e);
        }
    }

    public static boolean verificar(String passwordOriginal, String hashAlmacenado) {
        if (passwordOriginal == null || hashAlmacenado == null) {
            return false;
        }
        String hashEntrada = encriptar(passwordOriginal);
        return hashEntrada.equals(hashAlmacenado);
    }
}