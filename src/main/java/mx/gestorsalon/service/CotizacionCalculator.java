package mx.gestorsalon.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Pure price arithmetic for an event quote: subtotal, discount, tax and
 * deposit.
 * No Spring dependencies, so it can be unit tested in isolation.
 */
public class CotizacionCalculator {

    static final BigDecimal IVA = new BigDecimal("0.16");
    static final BigDecimal PORCENTAJE_ANTICIPO = new BigDecimal("0.30");
    static final BigDecimal DESCUENTO_MAXIMO = new BigDecimal("0.50");

    public record Resumen(BigDecimal subtotal, BigDecimal descuento, BigDecimal total,
            BigDecimal anticipo, BigDecimal restante) {
    }

    public Resumen calcular(List<BigDecimal> precios, BigDecimal porcentajeDescuento) {
        BigDecimal subtotal = subtotal(precios);
        BigDecimal descuento = descuento(subtotal, porcentajeDescuento);
        BigDecimal total = conIva(subtotal.subtract(descuento));
        BigDecimal anticipo = anticipo(total);
        return new Resumen(subtotal, descuento, total, anticipo, redondear(total.subtract(anticipo)));
    }

    public BigDecimal subtotal(List<BigDecimal> precios) {
        if (precios == null || precios.isEmpty()) {
            return redondear(BigDecimal.ZERO);
        }
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal precio : precios) {
            if (precio == null || precio.signum() < 0) {
                throw new IllegalArgumentException("Every price must be zero or positive");
            }
            total = total.add(precio);
        }
        return redondear(total);
    }

    public BigDecimal descuento(BigDecimal subtotal, BigDecimal porcentaje) {
        if (porcentaje == null || porcentaje.signum() == 0) {
            return redondear(BigDecimal.ZERO);
        }
        if (porcentaje.signum() < 0 || porcentaje.compareTo(DESCUENTO_MAXIMO) > 0) {
            throw new IllegalArgumentException("Discount must be between 0 and 0.50");
        }
        return redondear(subtotal.multiply(porcentaje));
    }

    public BigDecimal conIva(BigDecimal monto) {
        return redondear(monto.add(monto.multiply(IVA)));
    }

    public BigDecimal anticipo(BigDecimal total) {
        return redondear(total.multiply(PORCENTAJE_ANTICIPO));
    }

    private BigDecimal redondear(BigDecimal monto) {
        return monto.setScale(2, RoundingMode.HALF_UP);
    }
}