package pe.transporte;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.Test;

class AppTest {
    @Test
    void appStartsWithoutErrors() {
        assertDoesNotThrow(() -> App.main(new String[]{}));
    }
}
