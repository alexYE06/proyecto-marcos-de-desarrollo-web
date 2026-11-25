package pe.transporte.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

public final class GeneradorQR {

    private GeneradorQR() {
    }

    public static String construirPayloadBoleto(String codigoBoleto, Integer idViaje, Integer numeroAsiento, String docPasajero) {
        return "URBANRIDE|BOLETO:" + codigoBoleto
                + "|VIAJE:" + (idViaje != null ? idViaje : 0)
                + "|ASIENTO:" + (numeroAsiento != null ? numeroAsiento : 0)
                + "|DOC:" + (docPasajero != null ? docPasajero : "-");
    }

    public static String generarQRBoleto(String codigoBoleto, Integer idViaje, Integer numeroAsiento, String docPasajero) {
        return generarQRBase64(construirPayloadBoleto(codigoBoleto, idViaje, numeroAsiento, docPasajero), 220, 220);
    }

    public static String generarTextoQR(String codigoBoleto, String dniPasajero) {
        String contenido = "URBANRIDE|BOLETO:" + codigoBoleto + "|DOC:" + dniPasajero;
        return generarQRBase64(contenido, 220, 220);
    }

    public static String extraerCodigoBoleto(String entrada) {
        if (entrada == null) return "";
        String texto = entrada.trim();
        for (String parte : texto.split("\\|")) {
            if (parte.toUpperCase().startsWith("BOLETO:")) {
                return parte.substring("BOLETO:".length()).trim();
            }
        }
        return texto;
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
            return null;
        }
    }
}
