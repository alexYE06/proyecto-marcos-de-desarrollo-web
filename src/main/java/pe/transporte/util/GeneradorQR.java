package pe.transporte.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class GeneradorQR {

    public static String generarTextoQR(String codigoBoleto, String dniPasajero) {
        String contenido = "BOLETO:" + codigoBoleto + "|DNI:" + dniPasajero + "|VERIFICADO";
        return generarQRBase64(contenido, 200, 200);
    }

    public static String generarQRBase64(String texto, int ancho, int alto) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(texto, BarcodeFormat.QR_CODE, ancho, alto);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            byte[] imageBytes = outputStream.toByteArray();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            return "data:image/png;base64," + base64Image;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}