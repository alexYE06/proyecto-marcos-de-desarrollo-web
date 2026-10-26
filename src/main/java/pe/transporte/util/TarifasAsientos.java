package pe.transporte.util;

import java.util.Set;

/**
 * Misma regla de tarifas y asientos ocupados que usa asientos.js:
 *  - Piso 1: asientos 1-4 = S/ 125, 5-20 = S/ 115
 *  - Piso 2: asientos 21-64 = S/ 90
 * Si cambias las tarifas en asientos.js, cámbialas también aquí.
 */
public final class TarifasAsientos {

    public static final int TOTAL_ASIENTOS = 64;
    public static final int MAX_POR_VENTA = 5;

    // Mismos asientos "ocupados" de ejemplo que asientos.js
    public static final Set<Integer> OCUPADOS_BASE =
            Set.of(2, 8, 11, 15, 19, 26, 27, 34, 38, 45, 46, 53, 60);

    private TarifasAsientos() {}

    public static int precio(int numeroAsiento) {
        if (numeroAsiento <= 4) return 125;
        if (numeroAsiento <= 20) return 115;
        return 90;
    }
}
