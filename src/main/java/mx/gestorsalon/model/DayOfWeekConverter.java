package mx.gestorsalon.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.DayOfWeek;

/**
 * Persiste DayOfWeek como su número ISO-8601 (lunes=1 ... domingo=7) en la columna
 * INT existente (dia_semana). Así el dominio usa el tipo seguro DayOfWeek SIN migrar
 * datos: la columna sigue siendo INT 1-7, exactamente como estaba.
 *
 * autoApply = true → se aplica a todo atributo DayOfWeek del proyecto automáticamente.
 */
@Converter(autoApply = true)
public class DayOfWeekConverter implements AttributeConverter<DayOfWeek, Integer> {

    @Override
    public Integer convertToDatabaseColumn(DayOfWeek dia) {
        return (dia == null) ? null : dia.getValue();
    }

    @Override
    public DayOfWeek convertToEntityAttribute(Integer valor) {
        return (valor == null) ? null : DayOfWeek.of(valor);
    }
}
