package mx.gestorsalon.service;

import mx.gestorsalon.dto.CategoriaDTO;
import mx.gestorsalon.exception.AccesoDenegadoException;
import mx.gestorsalon.exception.RecursoNoEncontradoException;
import mx.gestorsalon.mapper.CategoriaMapper;
import mx.gestorsalon.model.Categoria;
import mx.gestorsalon.model.Negocio;
import mx.gestorsalon.repository.CategoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    private static final String NOMBRE_ANTERIOR = "Banquetes";

    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private CategoriaMapper categoriaMapper;

    @InjectMocks
    private CategoriaService categoriaService;

    private Negocio negocio;

    /** Operación del servicio que recibe un id de categoría y debe validar que sea del negocio. */
    @FunctionalInterface
    private interface OperacionSobreCategoria {
        void ejecutar(CategoriaService service, Negocio negocio, Long categoriaId);
    }

    static Stream<Arguments> operacionesSobreUnaCategoria() {
        return Stream.of(
                Arguments.of("update", (OperacionSobreCategoria) (s, n, id) -> s.update(n, id, categoriaDto(null))),
                Arguments.of("deactivate", (OperacionSobreCategoria) CategoriaService::deactivate));
    }

    @BeforeEach
    void setUp() {
        negocio = negocioConId(1L);
    }

    private static Negocio negocioConId(Long id) {
        Negocio n = new Negocio();
        n.setId(id);
        return n;
    }

    private static CategoriaDTO categoriaDto(Boolean activa) {
        CategoriaDTO dto = new CategoriaDTO();
        dto.setNombre("Banquetes y bebidas");
        dto.setDescripcion("Menús, barra libre y meseros");
        dto.setActiva(activa);
        return dto;
    }

    private static Categoria categoriaExistente(Long id, Negocio duenio) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setNegocio(duenio);
        c.setNombre(NOMBRE_ANTERIOR);
        c.setDescripcion("Solo comida");
        return c;
    }

    @Test
    @DisplayName("getAllByNegocio devuelve mapeadas, en orden, todas las categorías del negocio")
    void getAllByNegocio_variasCategorias_mapeaCadaUna() {
        // Arrange
        Categoria banquetes = categoriaExistente(5L, negocio);
        Categoria mobiliario = categoriaExistente(6L, negocio);
        CategoriaDTO banquetesDto = new CategoriaDTO();
        CategoriaDTO mobiliarioDto = new CategoriaDTO();
        when(categoriaRepository.findByNegocioId(1L)).thenReturn(List.of(banquetes, mobiliario));
        when(categoriaMapper.toDTO(banquetes)).thenReturn(banquetesDto);
        when(categoriaMapper.toDTO(mobiliario)).thenReturn(mobiliarioDto);

        // Act
        List<CategoriaDTO> resultado = categoriaService.getAllByNegocio(1L);

        // Assert
        assertEquals(2, resultado.size());
        assertSame(banquetesDto, resultado.get(0));
        assertSame(mobiliarioDto, resultado.get(1));
    }

    @Test
    @DisplayName("create asigna el negocio del usuario a la categoría y devuelve el DTO de la categoría guardada")
    void create_categoriaNueva_asignaNegocioYDevuelveDtoGuardado() {
        // Arrange
        CategoriaDTO dto = categoriaDto(null);
        Categoria entidad = new Categoria();
        Categoria guardada = categoriaExistente(5L, negocio);
        CategoriaDTO respuesta = new CategoriaDTO();
        when(categoriaMapper.toEntity(dto)).thenReturn(entidad);
        when(categoriaRepository.save(entidad)).thenReturn(guardada);
        when(categoriaMapper.toDTO(guardada)).thenReturn(respuesta);

        // Act
        CategoriaDTO resultado = categoriaService.create(negocio, dto);

        // Assert
        assertSame(respuesta, resultado);
        assertSame(negocio, entidad.getNegocio());
    }

    @Test
    @DisplayName("update copia nombre y descripción del DTO y devuelve la categoría mapeada")
    void update_categoriaPropia_copiaNombreYDescripcion() {
        // Arrange
        Categoria existente = categoriaExistente(5L, negocio);
        CategoriaDTO dto = categoriaDto(true);
        CategoriaDTO respuesta = new CategoriaDTO();
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.save(existente)).thenReturn(existente);
        when(categoriaMapper.toDTO(existente)).thenReturn(respuesta);

        // Act
        CategoriaDTO resultado = categoriaService.update(negocio, 5L, dto);

        // Assert
        assertSame(respuesta, resultado);
        assertEquals("Banquetes y bebidas", existente.getNombre());
        assertEquals("Menús, barra libre y meseros", existente.getDescripcion());
        assertSame(negocio, existente.getNegocio());
    }

    @ParameterizedTest(name = "activa actual {0}, en el DTO {1} -> queda {2}")
    @DisplayName("update deja la categoría activa si el DTO no indica estado y respeta true/false explícitos")
    @CsvSource({
            "false, ,      true",
            "true,  ,      true",
            "false, true,  true",
            "true,  false, false"
    })
    void update_activaOpcional_nuloLaDejaActiva(boolean activaActual, Boolean activaDto, boolean esperada) {
        // Arrange
        Categoria existente = categoriaExistente(5L, negocio);
        existente.setActiva(activaActual);
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(existente));

        // Act
        categoriaService.update(negocio, 5L, categoriaDto(activaDto));

        // Assert
        assertEquals(esperada, existente.getActiva());
        verify(categoriaRepository).save(existente);
    }

    @ParameterizedTest(name = "{0} de una categoría ajena se rechaza")
    @DisplayName("update y deactivate rechazan una categoría de otro negocio sin modificarla ni guardarla")
    @MethodSource("operacionesSobreUnaCategoria")
    void updateYDeactivate_categoriaDeOtroNegocio_lanzaAccesoDenegado(String operacion,
                                                                      OperacionSobreCategoria accion) {
        // Arrange
        Categoria ajena = categoriaExistente(9L, negocioConId(2L));
        when(categoriaRepository.findById(9L)).thenReturn(Optional.of(ajena));

        // Act
        AccesoDenegadoException error = assertThrows(AccesoDenegadoException.class,
                () -> accion.ejecutar(categoriaService, negocio, 9L));

        // Assert
        assertEquals("La categoría no pertenece a su negocio", error.getMessage());
        assertEquals(NOMBRE_ANTERIOR, ajena.getNombre());
        assertTrue(ajena.getActiva());
        verify(categoriaRepository, never()).save(any());
        verifyNoInteractions(categoriaMapper);
    }

    @ParameterizedTest(name = "{0} de un id inexistente se rechaza")
    @DisplayName("update y deactivate avisan cuando la categoría no existe y no guardan nada")
    @MethodSource("operacionesSobreUnaCategoria")
    void updateYDeactivate_categoriaInexistente_lanzaRecursoNoEncontrado(String operacion,
                                                                         OperacionSobreCategoria accion) {
        // Arrange
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        RecursoNoEncontradoException error = assertThrows(RecursoNoEncontradoException.class,
                () -> accion.ejecutar(categoriaService, negocio, 99L));

        // Assert
        assertEquals("Categoría no encontrada", error.getMessage());
        verify(categoriaRepository, never()).save(any());
        verifyNoInteractions(categoriaMapper);
    }

    @Test
    @DisplayName("deactivate hace baja lógica: marca la categoría inactiva, la guarda y nunca la borra")
    void deactivate_categoriaPropia_quedaInactivaSinBorrarse() {
        // Arrange
        Categoria propia = categoriaExistente(5L, negocio);
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(propia));

        // Act
        categoriaService.deactivate(negocio, 5L);

        // Assert
        assertFalse(propia.getActiva());
        assertEquals(NOMBRE_ANTERIOR, propia.getNombre());
        verify(categoriaRepository).save(propia);
        verify(categoriaRepository, never()).delete(any());
        verify(categoriaRepository, never()).deleteById(any());
    }
}
