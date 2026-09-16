package mx.gestorsalon.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CotizacionCalculatorTest {

    private CotizacionCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CotizacionCalculator();
    }

    @Test
    @DisplayName("subtotal returns 0.00 for a null or empty list")
    void subtotal_nullOrEmpty_returnsZero() {
        assertEquals(new BigDecimal("0.00"), calculator.subtotal(null));
        assertEquals(new BigDecimal("0.00"), calculator.subtotal(List.of()));
    }

    @Test
    @DisplayName("subtotal adds the prices and rounds to two decimals")
    void subtotal_addsAndRounds() {
        BigDecimal result = calculator.subtotal(List.of(new BigDecimal("1500"), new BigDecimal("250.555")));
        assertEquals(new BigDecimal("1750.56"), result);
    }

    @Test
    @DisplayName("subtotal rejects null or negative prices")
    void subtotal_invalidPrice_throws() {
        List<BigDecimal> withNull = Arrays.asList(new BigDecimal("10"), null);
        List<BigDecimal> withNegative = List.of(new BigDecimal("-1"));
        assertThrows(IllegalArgumentException.class, () -> calculator.subtotal(withNull));
        assertThrows(IllegalArgumentException.class, () -> calculator.subtotal(withNegative));
    }

    @Test
    @DisplayName("descuento is 0.00 when the percentage is null or zero")
    void descuento_nullOrZero_returnsZero() {
        assertEquals(new BigDecimal("0.00"), calculator.descuento(new BigDecimal("1000"), null));
        assertEquals(new BigDecimal("0.00"), calculator.descuento(new BigDecimal("1000"), BigDecimal.ZERO));
    }

    @Test
    @DisplayName("descuento applies the percentage to the subtotal")
    void descuento_appliesPercentage() {
        assertEquals(new BigDecimal("150.00"), calculator.descuento(new BigDecimal("1000"), new BigDecimal("0.15")));
    }

    @Test
    @DisplayName("descuento rejects negative percentages and anything above 50%")
    void descuento_outOfRange_throws() {
        BigDecimal subtotal = new BigDecimal("1000");
        BigDecimal negative = new BigDecimal("-0.10");
        BigDecimal tooHigh = new BigDecimal("0.51");
        assertThrows(IllegalArgumentException.class, () -> calculator.descuento(subtotal, negative));
        assertThrows(IllegalArgumentException.class, () -> calculator.descuento(subtotal, tooHigh));
    }

    @Test
    @DisplayName("conIva adds 16% and anticipo takes 30%")
    void conIva_and_anticipo() {
        assertEquals(new BigDecimal("1160.00"), calculator.conIva(new BigDecimal("1000")));
        assertEquals(new BigDecimal("348.00"), calculator.anticipo(new BigDecimal("1160")));
    }

    @Test
    @DisplayName("calcular composes subtotal, discount, tax, deposit and balance")
    void calcular_composesEverything() {
        CotizacionCalculator.Resumen r = calculator.calcular(
                List.of(new BigDecimal("8000"), new BigDecimal("2000")), new BigDecimal("0.10"));
        assertEquals(new BigDecimal("10000.00"), r.subtotal());
        assertEquals(new BigDecimal("1000.00"), r.descuento());
        assertEquals(new BigDecimal("10440.00"), r.total());
        assertEquals(new BigDecimal("3132.00"), r.anticipo());
        assertEquals(new BigDecimal("7308.00"), r.restante());
    }
}