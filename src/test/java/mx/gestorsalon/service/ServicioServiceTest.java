package mx.gestorsalon.service;

import mx.gestorsalon.dto.ServicioDTO;
import mx.gestorsalon.exception.AccesoDenegadoException;
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
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
}