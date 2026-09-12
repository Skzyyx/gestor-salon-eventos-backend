package mx.gestorsalon.mapper;

import mx.gestorsalon.dto.TurnoDTO;
import mx.gestorsalon.model.Turno;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class TurnoMapperTest {

    private TurnoMapper turnoMapper;

    @BeforeEach
    void setUp() {
        turnoMapper = new TurnoMapper();
    }

    @Test
    @DisplayName("toDTO debe retornar null si turno es null")
    void toDTO_conTurnoNull_retornaNull() {
        assertNull(turnoMapper.toDTO(null));
    }

    @Test
    @DisplayName("toDTO debe mapear todos los campos correctamente")
    void toDTO_conTurnoValido_mapeaCorrectamente() {
        Turno turno = new Turno();
        turno.setId(10L);
        turno.setDiaSemana(DayOfWeek.SATURDAY);
        turno.setTipoTurno("NOCHE");
        turno.setHoraInicio(LocalTime.of(19, 0));
        turno.setHoraFin(LocalTime.of(2, 0));
        turno.setActivo(true);

        TurnoDTO dto = turnoMapper.toDTO(turno);

        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals(DayOfWeek.SATURDAY, dto.getDiaSemana());
        assertEquals("NOCHE", dto.getTipoTurno());
        assertEquals(LocalTime.of(19, 0), dto.getHoraInicio());
        assertEquals(LocalTime.of(2, 0), dto.getHoraFin());
        assertTrue(dto.getActivo());
    }

    @Test
    @DisplayName("toEntity debe retornar null si dto es null")
    void toEntity_conDtoNull_retornaNull() {
        assertNull(turnoMapper.toEntity(null));
    }

    @Test
    @DisplayName("toEntity debe mapear todos los campos correctamente cuando activo viene en dto")
    void toEntity_conDtoValido_mapeaCorrectamente() {
        TurnoDTO dto = new TurnoDTO();
        dto.setDiaSemana(DayOfWeek.SUNDAY);
        dto.setTipoTurno("MAÑANA");
        dto.setHoraInicio(LocalTime.of(9, 0));
        dto.setHoraFin(LocalTime.of(14, 0));
        dto.setActivo(false);

        Turno entity = turnoMapper.toEntity(dto);

        assertNotNull(entity);
        assertEquals(DayOfWeek.SUNDAY, entity.getDiaSemana());
        assertEquals("MAÑANA", entity.getTipoTurno());
        assertEquals(LocalTime.of(9, 0), entity.getHoraInicio());
        assertEquals(LocalTime.of(14, 0), entity.getHoraFin());
        assertFalse(entity.getActivo());
    }

    @Test
    @DisplayName("toEntity debe asignar activo=true por defecto si activo es null")
    void toEntity_conActivoNull_asignaTruePorDefecto() {
        TurnoDTO dto = new TurnoDTO();
        dto.setDiaSemana(DayOfWeek.FRIDAY);
        dto.setTipoTurno("TARDE");
        dto.setHoraInicio(LocalTime.of(14, 0));
        dto.setHoraFin(LocalTime.of(19, 0));
        dto.setActivo(null);

        Turno entity = turnoMapper.toEntity(dto);

        assertNotNull(entity);
        assertTrue(entity.getActivo());
    }
}
