package mx.gestorsalon.service;

import mx.gestorsalon.dto.TurnoDTO;
import mx.gestorsalon.exception.AccesoDenegadoException;
import mx.gestorsalon.exception.RecursoNoEncontradoException;
import mx.gestorsalon.mapper.TurnoMapper;
import mx.gestorsalon.model.Negocio;
import mx.gestorsalon.model.Turno;
import mx.gestorsalon.repository.TurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TurnoServiceTest {

    private static final DayOfWeek SABADO = DayOfWeek.SATURDAY;
    private static final LocalTime CATORCE = LocalTime.of(14, 0);
    private static final LocalTime DIECIOCHO = LocalTime.of(18, 0);

    @Mock
    private TurnoRepository turnoRepository;
    @Mock
    private TurnoMapper turnoMapper;

    @InjectMocks
    private TurnoService turnoService;

    private Negocio negocio;

    @BeforeEach
    void setUp() {
        negocio = negocioConId(1L);
    }

    private static Negocio negocioConId(Long id) {
        Negocio n = new Negocio();
        n.setId(id);
        return n;
    }

    private static TurnoDTO turno(LocalTime inicio, LocalTime fin) {
        TurnoDTO dto = new TurnoDTO();
        dto.setDiaSemana(SABADO);
        dto.setTipoTurno("TARDE");
        dto.setHoraInicio(inicio);
        dto.setHoraFin(fin);
        return dto;
    }

    private static Turno turnoExistente(Long id, Negocio duenio) {
        Turno t = new Turno();
        t.setId(id);
        t.setNegocio(duenio);
        return t;
    }

    @ParameterizedTest(name = "{0} a {1} se acepta")
    @DisplayName("create acepta un turno cuyo inicio es anterior al fin, aunque sea por un minuto")
    @CsvSource({"14:00, 18:00", "14:00, 14:01", "00:00, 23:59"})
    void create_inicioAntesDelFin_guardaConElNegocio(LocalTime inicio, LocalTime fin) {
        // Arrange
        TurnoDTO dto = turno(inicio, fin);
        Turno entidad = new Turno();
        TurnoDTO respuesta = new TurnoDTO();
        when(turnoRepository.existsOverlapping(1L, SABADO, inicio, fin, null)).thenReturn(false);
        when(turnoMapper.toEntity(dto)).thenReturn(entidad);
        when(turnoRepository.save(entidad)).thenReturn(entidad);
        when(turnoMapper.toDTO(entidad)).thenReturn(respuesta);

        // Act
        TurnoDTO resultado = turnoService.create(negocio, dto);

        // Assert
        assertSame(respuesta, resultado);
        assertSame(negocio, entidad.getNegocio());
    }

    @ParameterizedTest(name = "{0} a {1} se rechaza")
    @DisplayName("create rechaza un turno cuyo inicio no es anterior al fin, sin consultar la base")
    @CsvSource({"18:00, 14:00", "14:00, 14:00"})
    void create_inicioNoAnteriorAlFin_lanzaExcepcion(LocalTime inicio, LocalTime fin) {
        // Arrange
        TurnoDTO dto = turno(inicio, fin);

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> turnoService.create(negocio, dto));

        // Assert
        assertEquals("La hora de inicio debe ser anterior a la hora de fin", error.getMessage());
        verifyNoInteractions(turnoRepository, turnoMapper);
    }

    @Test
    @DisplayName("create rechaza un turno que se solapa con otro del mismo día")
    void create_horarioSolapado_lanzaExcepcion() {
        // Arrange
        TurnoDTO dto = turno(CATORCE, DIECIOCHO);
        when(turnoRepository.existsOverlapping(1L, SABADO, CATORCE, DIECIOCHO, null)).thenReturn(true);

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> turnoService.create(negocio, dto));

        // Assert
        assertTrue(error.getMessage().contains("se solapa"));
        verify(turnoRepository, never()).save(any());
    }

    @Test
    @DisplayName("update rechaza el turno de otro negocio antes de validar horarios")
    void update_turnoDeOtroNegocio_lanzaAccesoDenegado() {
        // Arrange
        when(turnoRepository.findById(10L)).thenReturn(Optional.of(turnoExistente(10L, negocioConId(2L))));
        TurnoDTO dto = turno(CATORCE, DIECIOCHO);

        // Act + Assert
        assertThrows(AccesoDenegadoException.class, () -> turnoService.update(negocio, 10L, dto));
        verify(turnoRepository, never()).existsOverlapping(any(), any(), any(), any(), any());
        verify(turnoRepository, never()).save(any());
    }

    @Test
    @DisplayName("update excluye al propio turno al buscar solapes y lo deja activo si no se indica")
    void update_turnoPropio_excluyeSuIdYQuedaActivo() {
        // Arrange
        Turno existente = turnoExistente(10L, negocio);
        existente.setActivo(false);
        TurnoDTO dto = turno(CATORCE, DIECIOCHO);
        when(turnoRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(turnoRepository.existsOverlapping(1L, SABADO, CATORCE, DIECIOCHO, 10L)).thenReturn(false);
        when(turnoRepository.save(existente)).thenReturn(existente);

        // Act
        turnoService.update(negocio, 10L, dto);

        // Assert
        assertTrue(existente.getActivo());
        assertEquals(CATORCE, existente.getHoraInicio());
        verify(turnoRepository).existsOverlapping(1L, SABADO, CATORCE, DIECIOCHO, 10L);
    }

    @Test
    @DisplayName("delete avisa cuando el turno no existe y no borra nada")
    void delete_turnoInexistente_lanzaRecursoNoEncontrado() {
        // Arrange
        when(turnoRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(RecursoNoEncontradoException.class, () -> turnoService.delete(negocio, 99L));
        verify(turnoRepository, never()).delete(any());
    }
}