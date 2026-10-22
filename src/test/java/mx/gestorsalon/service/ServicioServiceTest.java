package mx.gestorsalon.service;

import mx.gestorsalon.dto.ServicioDTO;
import mx.gestorsalon.exception.AccesoDenegadoException;
import mx.gestorsalon.exception.RecursoNoEncontradoException;
import mx.gestorsalon.mapper.ServicioMapper;
import mx.gestorsalon.model.Categoria;
import mx.gestorsalon.model.Negocio;
import mx.gestorsalon.model.Servicio;
import mx.gestorsalon.model.enums.TipoCobro;
import mx.gestorsalon.repository.CategoriaRepository;
import mx.gestorsalon.repository.ServicioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioServiceTest {

    private static final String NOMBRE = "Barra libre";

    @Mock
    private ServicioRepository servicioRepository;
    @Mock
    private ServicioMapper servicioMapper;
    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private ServicioService servicioService;

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

    private static ServicioDTO servicio(String precio) {
        ServicioDTO dto = new ServicioDTO();
        dto.setNombre(NOMBRE);
        dto.setPrecio(precio == null ? null : new BigDecimal(precio));
        dto.setTipoCobro(TipoCobro.POR_PERSONA);
        return dto;
    }

    private static Servicio servicioExistente(Long id, Negocio duenio) {
        Servicio s = new Servicio();
        s.setId(id);
        s.setNegocio(duenio);
        s.setNombre("Barra nacional");
        s.setDescripcion("Solo cerveza");
        s.setPrecio(new BigDecimal("900.00"));
        s.setTipoCobro(TipoCobro.POR_EVENTO);
        return s;
    }

    private static Categoria categoriaDe(Long id, Negocio duenio) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setNegocio(duenio);
        return c;
    }

    /** Operación del servicio que recibe un id de servicio y debe validar que sea del negocio. */
    @FunctionalInterface
    private interface OperacionSobreServicio {
        void ejecutar(ServicioService service, Negocio negocio, Long servicioId);
    }

    static Stream<Arguments> operacionesSobreUnServicio() {
        return Stream.of(
                Arguments.of("update", (OperacionSobreServicio) (s, n, id) -> s.update(n, id, servicio("1500"))),
                Arguments.of("deactivate", (OperacionSobreServicio) ServicioService::deactivate));
    }

    @ParameterizedTest(name = "precio {0} se rechaza")
    @DisplayName("create rechaza precios nulos, cero o negativos antes de tocar la base")
    @NullSource
    @ValueSource(strings = {"0", "0.00", "-0.01", "-150"})
    void create_precioNoPositivo_lanzaExcepcion(String precio) {
        // Arrange
        ServicioDTO dto = servicio(precio);

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicioService.create(negocio, dto));

        // Assert
        assertEquals("El precio debe ser mayor a $0", error.getMessage());
        verifyNoInteractions(servicioRepository, servicioMapper, categoriaRepository);
    }

    @Test
    @DisplayName("create acepta el precio mínimo de un centavo y asigna el negocio")
    void create_precioDeUnCentavo_guardaElServicio() {
        // Arrange
        ServicioDTO dto = servicio("0.01");
        Servicio entidad = new Servicio();
        ServicioDTO respuesta = new ServicioDTO();
        when(servicioRepository.existsByNombreInNegocio(1L, NOMBRE, null)).thenReturn(false);
        when(servicioMapper.toEntity(dto)).thenReturn(entidad);
        when(servicioRepository.save(entidad)).thenReturn(entidad);
        when(servicioMapper.toDTO(entidad)).thenReturn(respuesta);

        // Act
        ServicioDTO resultado = servicioService.create(negocio, dto);

        // Assert
        assertSame(respuesta, resultado);
        assertSame(negocio, entidad.getNegocio());
        assertNull(entidad.getCategoria());
        verifyNoInteractions(categoriaRepository);
    }

    @Test
    @DisplayName("create rechaza un nombre que ya usa otro servicio activo del negocio")
    void create_nombreDuplicado_lanzaExcepcion() {
        // Arrange
        ServicioDTO dto = servicio("1500");
        when(servicioRepository.existsByNombreInNegocio(1L, NOMBRE, null)).thenReturn(true);

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> servicioService.create(negocio, dto));
        verify(servicioRepository, never()).save(any());
    }

    @Test
    @DisplayName("create rechaza una categoría que pertenece a otro negocio")
    void create_categoriaDeOtroNegocio_lanzaAccesoDenegado() {
        // Arrange
        ServicioDTO dto = servicio("1500");
        dto.setCategoriaId(5L);
        Categoria ajena = new Categoria();
        ajena.setId(5L);
        ajena.setNegocio(negocioConId(2L));
        when(servicioRepository.existsByNombreInNegocio(1L, NOMBRE, null)).thenReturn(false);
        when(servicioMapper.toEntity(dto)).thenReturn(new Servicio());
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(ajena));

        // Act + Assert
        assertThrows(AccesoDenegadoException.class, () -> servicioService.create(negocio, dto));
        verify(servicioRepository, never()).save(any());
    }

    @Test
    @DisplayName("update rechaza editar un servicio de otro negocio")
    void update_servicioDeOtroNegocio_lanzaAccesoDenegado() {
        // Arrange
        Servicio ajeno = new Servicio();
        ajeno.setNegocio(negocioConId(2L));
        when(servicioRepository.findById(7L)).thenReturn(Optional.of(ajeno));
        ServicioDTO dto = servicio("1500");

        // Act + Assert
        assertThrows(AccesoDenegadoException.class, () -> servicioService.update(negocio, 7L, dto));
        verify(servicioRepository, never()).save(any());
    }

    @Test
    @DisplayName("deactivate hace baja lógica: marca el servicio inactivo y lo guarda")
    void deactivate_servicioPropio_quedaInactivo() {
        // Arrange
        Servicio propio = new Servicio();
        propio.setNegocio(negocio);
        when(servicioRepository.findById(7L)).thenReturn(Optional.of(propio));

        // Act
        servicioService.deactivate(negocio, 7L);

        // Assert
        assertFalse(propio.getActivo());
        verify(servicioRepository).save(propio);
    }

    @Test
    @DisplayName("getAllByNegocio devuelve mapeados, en orden, todos los servicios del negocio")
    void getAllByNegocio_variosServicios_mapeaCadaUno() {
        // Arrange
        Servicio barra = servicioExistente(7L, negocio);
        Servicio dj = servicioExistente(8L, negocio);
        ServicioDTO barraDto = new ServicioDTO();
        ServicioDTO djDto = new ServicioDTO();
        when(servicioRepository.findByNegocioId(1L)).thenReturn(List.of(barra, dj));
        when(servicioMapper.toDTO(barra)).thenReturn(barraDto);
        when(servicioMapper.toDTO(dj)).thenReturn(djDto);

        // Act
        List<ServicioDTO> resultado = servicioService.getAllByNegocio(1L);

        // Assert
        assertEquals(2, resultado.size());
        assertSame(barraDto, resultado.get(0));
        assertSame(djDto, resultado.get(1));
    }

    @Test
    @DisplayName("update copia los datos, el tipo de cobro y asigna la categoría propia indicada")
    void update_servicioPropio_copiaCamposYAsignaCategoria() {
        // Arrange
        Servicio existente = servicioExistente(7L, negocio);
        Categoria bebidas = categoriaDe(5L, negocio);
        ServicioDTO dto = servicio("1500");
        dto.setDescripcion("Barra libre nacional e importada");
        dto.setImagenUrl("https://res.cloudinary.com/demo/barra.jpg");
        dto.setCategoriaId(5L);
        ServicioDTO respuesta = new ServicioDTO();
        when(servicioRepository.findById(7L)).thenReturn(Optional.of(existente));
        when(servicioRepository.existsByNombreInNegocio(1L, NOMBRE, 7L)).thenReturn(false);
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(bebidas));
        when(servicioRepository.save(existente)).thenReturn(existente);
        when(servicioMapper.toDTO(existente)).thenReturn(respuesta);

        // Act
        ServicioDTO resultado = servicioService.update(negocio, 7L, dto);

        // Assert
        assertSame(respuesta, resultado);
        assertEquals(NOMBRE, existente.getNombre());
        assertEquals("Barra libre nacional e importada", existente.getDescripcion());
        assertEquals(new BigDecimal("1500"), existente.getPrecio());
        assertEquals("https://res.cloudinary.com/demo/barra.jpg", existente.getImagenUrl());
        assertEquals(TipoCobro.POR_PERSONA, existente.getTipoCobro());
        assertSame(bebidas, existente.getCategoria());
        assertSame(negocio, existente.getNegocio());
    }

    @ParameterizedTest(name = "activo actual {0}, en el DTO {1} -> queda {2}")
    @DisplayName("update solo cambia el estado activo cuando el DTO lo trae; si viene nulo conserva el actual")
    @CsvSource({
            "true,  ,      true",
            "false, ,      false",
            "false, true,  true",
            "true,  false, false"
    })
    void update_activoOpcional_soloCambiaSiVieneEnElDto(boolean activoActual, Boolean activoDto, boolean esperado) {
        // Arrange
        Servicio existente = servicioExistente(7L, negocio);
        existente.setActivo(activoActual);
        ServicioDTO dto = servicio("1500");
        dto.setActivo(activoDto);
        when(servicioRepository.findById(7L)).thenReturn(Optional.of(existente));
        when(servicioRepository.existsByNombreInNegocio(1L, NOMBRE, 7L)).thenReturn(false);

        // Act
        servicioService.update(negocio, 7L, dto);

        // Assert
        assertEquals(esperado, existente.getActivo());
        verify(servicioRepository).save(existente);
    }

    @ParameterizedTest(name = "{0} de un servicio ajeno se rechaza")
    @DisplayName("update y deactivate rechazan un servicio de otro negocio sin validarlo, modificarlo ni guardarlo")
    @MethodSource("operacionesSobreUnServicio")
    void updateYDeactivate_servicioDeOtroNegocio_lanzaAccesoDenegado(String operacion,
                                                                     OperacionSobreServicio accion) {
        // Arrange
        Servicio ajeno = servicioExistente(7L, negocioConId(2L));
        when(servicioRepository.findById(7L)).thenReturn(Optional.of(ajeno));

        // Act
        AccesoDenegadoException error = assertThrows(AccesoDenegadoException.class,
                () -> accion.ejecutar(servicioService, negocio, 7L));

        // Assert
        assertEquals("El servicio no pertenece a su negocio", error.getMessage());
        assertEquals("Barra nacional", ajeno.getNombre());
        assertTrue(ajeno.getActivo());
        verify(servicioRepository, never()).existsByNombreInNegocio(any(), any(), any());
        verify(servicioRepository, never()).save(any());
        verifyNoInteractions(categoriaRepository, servicioMapper);
    }

    @ParameterizedTest(name = "{0} de un id inexistente se rechaza")
    @DisplayName("update y deactivate avisan cuando el servicio no existe y no guardan nada")
    @MethodSource("operacionesSobreUnServicio")
    void updateYDeactivate_servicioInexistente_lanzaRecursoNoEncontrado(String operacion,
                                                                        OperacionSobreServicio accion) {
        // Arrange
        when(servicioRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        RecursoNoEncontradoException error = assertThrows(RecursoNoEncontradoException.class,
                () -> accion.ejecutar(servicioService, negocio, 99L));

        // Assert
        assertEquals("Servicio no encontrado", error.getMessage());
        verify(servicioRepository, never()).save(any());
        verifyNoInteractions(categoriaRepository, servicioMapper);
    }

    @Test
    @DisplayName("update busca nombres duplicados excluyendo su propio id y rechaza si otro servicio ya lo usa")
    void update_nombreDeOtroServicio_excluyeSuIdYLanzaExcepcion() {
        // Arrange
        Servicio existente = servicioExistente(7L, negocio);
        ServicioDTO dto = servicio("1500");
        when(servicioRepository.findById(7L)).thenReturn(Optional.of(existente));
        when(servicioRepository.existsByNombreInNegocio(1L, NOMBRE, 7L)).thenReturn(true);

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicioService.update(negocio, 7L, dto));

        // Assert
        assertEquals("Ya existe un servicio activo con ese nombre", error.getMessage());
        assertEquals("Barra nacional", existente.getNombre());
        verify(servicioRepository).existsByNombreInNegocio(1L, NOMBRE, 7L);
        verify(servicioRepository, never()).save(any());
        verifyNoInteractions(categoriaRepository);
    }

    @Test
    @DisplayName("update rechaza mover el servicio a una categoría de otro negocio")
    void update_categoriaDeOtroNegocio_lanzaAccesoDenegado() {
        // Arrange
        Servicio existente = servicioExistente(7L, negocio);
        ServicioDTO dto = servicio("1500");
        dto.setCategoriaId(5L);
        when(servicioRepository.findById(7L)).thenReturn(Optional.of(existente));
        when(servicioRepository.existsByNombreInNegocio(1L, NOMBRE, 7L)).thenReturn(false);
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(categoriaDe(5L, negocioConId(2L))));

        // Act
        AccesoDenegadoException error = assertThrows(AccesoDenegadoException.class,
                () -> servicioService.update(negocio, 7L, dto));

        // Assert
        assertEquals("La categoría no pertenece a su negocio", error.getMessage());
        assertNull(existente.getCategoria());
        verify(servicioRepository, never()).save(any());
    }

    @Test
    @DisplayName("create avisa cuando la categoría indicada no existe y no guarda el servicio")
    void create_categoriaInexistente_lanzaRecursoNoEncontrado() {
        // Arrange
        ServicioDTO dto = servicio("1500");
        dto.setCategoriaId(99L);
        when(servicioRepository.existsByNombreInNegocio(1L, NOMBRE, null)).thenReturn(false);
        when(servicioMapper.toEntity(dto)).thenReturn(new Servicio());
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        RecursoNoEncontradoException error = assertThrows(RecursoNoEncontradoException.class,
                () -> servicioService.create(negocio, dto));

        // Assert
        assertEquals("Categoría no encontrada", error.getMessage());
        verify(servicioRepository, never()).save(any());
    }

    @Test
    @DisplayName("update sin categoriaId le quita la categoría que tenía el servicio")
    void update_categoriaIdNulo_quitaLaCategoria() {
        // Arrange
        Servicio existente = servicioExistente(7L, negocio);
        existente.setCategoria(categoriaDe(5L, negocio));
        ServicioDTO dto = servicio("1500");
        when(servicioRepository.findById(7L)).thenReturn(Optional.of(existente));
        when(servicioRepository.existsByNombreInNegocio(1L, NOMBRE, 7L)).thenReturn(false);

        // Act
        servicioService.update(negocio, 7L, dto);

        // Assert
        assertNull(existente.getCategoria());
        verify(servicioRepository).save(existente);
        verifyNoInteractions(categoriaRepository);
    }
}
