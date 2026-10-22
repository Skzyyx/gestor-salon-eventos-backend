package mx.gestorsalon.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.DateTimeException;
import java.time.DayOfWeek;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DayOfWeekConverterTest {

    @ParameterizedTest(name = "{0} se guarda como {1}")
    @DisplayName("convertToDatabaseColumn usa la numeración ISO de la columna INT existente (lunes=1 ... domingo=7)")
    @CsvSource({
            "MONDAY, 1",
            "TUESDAY, 2",
            "WEDNESDAY, 3",
            "THURSDAY, 4",
            "FRIDAY, 5",
            "SATURDAY, 6",
            "SUNDAY, 7"
    })
    void convertToDatabaseColumn_diaDeLaSemana_devuelveNumeroIso(DayOfWeek dia, int valorEsperado) {
        // Arrange
        DayOfWeekConverter converter = new DayOfWeekConverter();
        Integer esperado = valorEsperado;

        // Act
        Integer valor = converter.convertToDatabaseColumn(dia);

        // Assert
        assertEquals(esperado, valor);
    }

    @ParameterizedTest(name = "{0} ida y vuelta")
    @DisplayName("un día guardado en la base y leído de vuelta es el mismo día, y su valor está entre 1 y 7")
    @EnumSource(DayOfWeek.class)
    void idaYVuelta_cualquierDia_conservaElDia(DayOfWeek dia) {
        // Arrange
        DayOfWeekConverter converter = new DayOfWeekConverter();

        // Act
        Integer valor = converter.convertToDatabaseColumn(dia);
        DayOfWeek leido = converter.convertToEntityAttribute(valor);

        // Assert
        assertTrue(valor >= 1 && valor <= 7, "valor fuera de la columna 1-7: " + valor);
        assertEquals(dia, leido);
    }

    @Test
    @DisplayName("convertToDatabaseColumn devuelve null cuando el día es null")
    void convertToDatabaseColumn_null_devuelveNull() {
        // Arrange
        DayOfWeekConverter converter = new DayOfWeekConverter();
        DayOfWeek sinDia = null;

        // Act
        Integer valor = converter.convertToDatabaseColumn(sinDia);

        // Assert
        assertNull(valor);
    }

    @Test
    @DisplayName("convertToEntityAttribute devuelve null cuando la columna es null")
    void convertToEntityAttribute_null_devuelveNull() {
        // Arrange
        DayOfWeekConverter converter = new DayOfWeekConverter();
        Integer sinValor = null;

        // Act
        DayOfWeek dia = converter.convertToEntityAttribute(sinValor);

        // Assert
        assertNull(dia);
    }

    @ParameterizedTest(name = "valor {0} se rechaza")
    @DisplayName("convertToEntityAttribute rechaza valores fuera de 1-7 en lugar de inventar un día")
    @ValueSource(ints = {0, 8, -1})
    void convertToEntityAttribute_valorFueraDeRango_lanzaDateTimeException(int valor) {
        // Arrange
        DayOfWeekConverter converter = new DayOfWeekConverter();
        Integer columna = valor;

        // Act
        DateTimeException error = assertThrows(DateTimeException.class,
                () -> converter.convertToEntityAttribute(columna));

        // Assert
        assertTrue(error.getMessage().contains(String.valueOf(valor)),
                "el mensaje debería indicar el valor inválido: " + error.getMessage());
    }
}
