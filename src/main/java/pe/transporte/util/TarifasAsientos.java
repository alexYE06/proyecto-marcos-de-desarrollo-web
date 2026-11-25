package pe.transporte.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regla centralizada de estructura y tarifas del bus de dos pisos (64 asientos):
 * - Piso 1 Panorámico (asientos 1-4): tarifa base + S/ 35
 * - Piso 1 VIP Ejecutivo (asientos 5-20): tarifa base + S/ 25
 * - Piso 2 Confort (asientos 21-64): tarifa base del viaje (ej. S/ 90)
 */
public final class TarifasAsientos {

    public static final int TOTAL_ASIENTOS = 64;
    public static final int MAX_POR_VENTA = 5;
    public static final double PRECIO_BASE_DEFECTO = 90.0;

    private TarifasAsientos() {
    }

    public static int piso(int numeroAsiento) {
        return numeroAsiento <= 20 ? 1 : 2;
    }

    public static String categoria(int numeroAsiento) {
        if (numeroAsiento <= 4) return "VIP Panorámico 180°";
        if (numeroAsiento <= 20) return "VIP Ejecutivo 160°";
        return "Confort 140°";
    }

    public static BigDecimal precio(int numeroAsiento, Double precioBaseViaje) {
        double base = (precioBaseViaje != null && precioBaseViaje > 0) ? precioBaseViaje : PRECIO_BASE_DEFECTO;
        double monto = base;
        if (numeroAsiento <= 4) {
            monto = base + 35.0;
        } else if (numeroAsiento <= 20) {
            monto = base + 25.0;
        }
        return BigDecimal.valueOf(monto).setScale(2, RoundingMode.HALF_UP);
    }

    public static int precio(int numeroAsiento) {
        return precio(numeroAsiento, PRECIO_BASE_DEFECTO).intValue();
    }
}
