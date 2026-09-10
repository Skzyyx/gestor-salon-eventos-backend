package mx.gestorsalon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
public class TurnoDTO {
    private Long id;

    // DayOfWeek: el enum estándar del JDK. Jackson lo serializa como "MONDAY".."SUNDAY".
    // @Min/@Max ya no aplican: un valor inválido falla al deserializar el enum.
    @NotNull(message = "El día de la semana es requerido")
    private DayOfWeek diaSemana;

    @NotBlank(message = "El tipo de turno es requerido (MAÑANA, TARDE, NOCHE)")
    private String tipoTurno;

    @NotNull(message = "La hora de inicio es requerida")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es requerida")
    private LocalTime horaFin;

    private Boolean activo;
}
