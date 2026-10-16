package mx.gestorsalon.service;

import mx.gestorsalon.dto.PaqueteDTO;
import mx.gestorsalon.dto.PaqueteServicioDTO;
import mx.gestorsalon.exception.AccesoDenegadoException;
import mx.gestorsalon.mapper.PaqueteMapper;
import mx.gestorsalon.model.Negocio;
import mx.gestorsalon.model.Paquete;
import mx.gestorsalon.model.PaqueteServicio;
import mx.gestorsalon.model.Servicio;
import mx.gestorsalon.repository.PaqueteRepository;
import mx.gestorsalon.repository.ServicioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaqueteServiceTest {

    private static final String NOMBRE = "Paquete Boda Plata";

    @Mock
    private PaqueteRepository paqueteRepository;
    @Mock
    private ServicioRepository servicioRepository;
    @Mock
    private PaqueteMapper paqueteMapper;

    @InjectMocks
    private PaqueteService paqueteService;

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

    private static PaqueteServicioDTO linea(Long servicioId, Integer cantidad) {
        PaqueteServicioDTO item = new PaqueteServicioDTO();
        item.setServicioId(servicioId);
        item.setCantidad(cantidad);
        return item;
    }

    private static PaqueteDTO paquete(String precio, PaqueteServicioDTO... lineas) {
        PaqueteDTO dto = new PaqueteDTO();
        dto.setNombre(NOMBRE);
        dto.setDescripcion("Banquete y mobiliario para 150 invitados");
        dto.setPrecio(precio == null ? null : new BigDecimal(precio));
        dto.setItems(List.of(lineas));
        return dto;
    }

    private static Servicio servicioExistente(Long id, Negocio duenio, String precio) {
        Servicio s = new Servicio();
        s.setId(id);
        s.setNegocio(duenio);
        s.setPrecio(new BigDecimal(precio));
        return s;
    }

    private static Paquete paqueteExistente(Long id, Negocio duenio) {
        Paquete p = new Paquete();
        p.setId(id);
        p.setNegocio(duenio);
        p.setNombre(NOMBRE);
        return p;
    }

    @ParameterizedTest(name = "precio {0} se rechaza")
    @DisplayName("create rechaza precios nulos, cero o negativos antes de tocar la base")
    @NullSource
    @ValueSource(strings = {"0", "0.00", "-0.01"})
    void create_precioNoPositivo_lanzaExcepcion(String precio) {
        // Arrange
        PaqueteDTO dto = paquete(precio, linea(10L, 1));

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> paqueteService.create(negocio, dto));

        // Assert
        assertEquals("El precio debe ser mayor a $0", error.getMessage());
        verifyNoInteractions(paqueteRepository, servicioRepository, paqueteMapper);
    }

    @ParameterizedTest(name = "servicios {0} se rechazan")
    @DisplayName("create rechaza un paquete sin servicios antes de tocar la base")
    @NullAndEmptySource
    void create_sinServicios_lanzaExcepcion(List<PaqueteServicioDTO> lineas) {
        // Arrange
        PaqueteDTO dto = paquete("4500.00");
        dto.setItems(lineas);

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> paqueteService.create(negocio, dto));

        // Assert
        assertEquals("Un paquete debe incluir al menos un servicio", error.getMessage());
        verifyNoInteractions(paqueteRepository, servicioRepository, paqueteMapper);
    }

    @Test
    @DisplayName("create rechaza el mismo servicio dos veces y pide usar la cantidad")
    void create_servicioRepetido_lanzaExcepcion() {
        // Arrange
        PaqueteDTO dto = paquete("4500.00", linea(10L, 1), linea(20L, 1), linea(10L, 2));

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> paqueteService.create(negocio, dto));

        // Assert
        assertEquals("Hay un servicio repetido en el paquete; usa la cantidad en su lugar", error.getMessage());
        verifyNoInteractions(paqueteRepository, servicioRepository, paqueteMapper);
    }

    @Test
    @DisplayName("create rechaza un nombre que ya usa otro paquete activo del negocio")
    void create_nombreDuplicado_lanzaExcepcion() {
        // Arrange
        PaqueteDTO dto = paquete("4500.00", linea(10L, 1));
        when(paqueteRepository.existsByNombreInNegocio(1L, NOMBRE, null)).thenReturn(true);

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> paqueteService.create(negocio, dto));

        // Assert
        assertEquals("Ya existe un paquete activo con ese nombre", error.getMessage());
        verify(paqueteRepository, never()).save(any());
        verifyNoInteractions(servicioRepository, paqueteMapper);
    }

    @Test
    @DisplayName("create rechaza un servicio de otro negocio aunque los demás sean propios")
    void create_servicioDeOtroNegocio_lanzaAccesoDenegado() {
        // Arrange
        PaqueteDTO dto = paquete("4500.00", linea(10L, 1), linea(30L, 1));
        when(paqueteRepository.existsByNombreInNegocio(1L, NOMBRE, null)).thenReturn(false);
        when(paqueteMapper.toEntity(dto)).thenReturn(new Paquete());
        when(servicioRepository.findById(10L))
                .thenReturn(Optional.of(servicioExistente(10L, negocio, "350.00")));
        when(servicioRepository.findById(30L))
                .thenReturn(Optional.of(servicioExistente(30L, negocioConId(2L), "900.00")));

        // Act
        AccesoDenegadoException error = assertThrows(AccesoDenegadoException.class,
                () -> paqueteService.create(negocio, dto));

        // Assert
        assertEquals("Un servicio seleccionado no pertenece a su negocio", error.getMessage());
        verify(paqueteRepository, never()).save(any());
    }

    @Test
    @DisplayName("create acepta el precio mínimo de un centavo y congela el precio actual de cada servicio")
    void create_paqueteValido_guardaLineasConPrecioCongelado() {
        // Arrange
        PaqueteDTO dto = paquete("0.01", linea(10L, 2), linea(20L, null));
        Servicio banquete = servicioExistente(10L, negocio, "350.00");
        Servicio mobiliario = servicioExistente(20L, negocio, "1200.00");
        Paquete entidad = new Paquete();
        PaqueteDTO respuesta = new PaqueteDTO();
        when(paqueteRepository.existsByNombreInNegocio(1L, NOMBRE, null)).thenReturn(false);
        when(paqueteMapper.toEntity(dto)).thenReturn(entidad);
        when(servicioRepository.findById(10L)).thenReturn(Optional.of(banquete));
        when(servicioRepository.findById(20L)).thenReturn(Optional.of(mobiliario));
        when(paqueteRepository.save(entidad)).thenReturn(entidad);
        when(paqueteMapper.toDTO(entidad)).thenReturn(respuesta);

        // Act
        PaqueteDTO resultado = paqueteService.create(negocio, dto);

        // Assert
        assertSame(respuesta, resultado);
        ArgumentCaptor<Paquete> guardado = ArgumentCaptor.forClass(Paquete.class);
        verify(paqueteRepository).save(guardado.capture());
        Paquete paquete = guardado.getValue();
        assertSame(negocio, paquete.getNegocio());
        assertEquals(2, paquete.getItems().size());
        PaqueteServicio primera = paquete.getItems().get(0);
        assertSame(banquete, primera.getServicio());
        assertSame(paquete, primera.getPaquete());
        assertEquals(2, primera.getCantidad());
        assertEquals(new BigDecimal("350.00"), primera.getPrecioUnitario());
        PaqueteServicio segunda = paquete.getItems().get(1);
        assertSame(mobiliario, segunda.getServicio());
        assertEquals(1, segunda.getCantidad());
        assertEquals(new BigDecimal("1200.00"), segunda.getPrecioUnitario());
    }

    @Test
    @DisplayName("update rechaza editar un paquete de otro negocio antes de validar nada")
    void update_paqueteDeOtroNegocio_lanzaAccesoDenegado() {
        // Arrange
        when(paqueteRepository.findById(5L)).thenReturn(Optional.of(paqueteExistente(5L, negocioConId(2L))));
        PaqueteDTO dto = paquete("4500.00", linea(10L, 1));

        // Act
        AccesoDenegadoException error = assertThrows(AccesoDenegadoException.class,
                () -> paqueteService.update(negocio, 5L, dto));

        // Assert
        assertEquals("El paquete no pertenece a su negocio", error.getMessage());
        verify(paqueteRepository, never()).existsByNombreInNegocio(any(), any(), any());
        verify(paqueteRepository, never()).save(any());
        verifyNoInteractions(servicioRepository, paqueteMapper);
    }

    @Test
    @DisplayName("update conserva el precio congelado de las líneas que ya existían y congela el actual en las nuevas")
    void update_lineaExistenteYNueva_conservaPrecioPrevio() {
        // Arrange
        Servicio banquete = servicioExistente(10L, negocio, "380.00");
        Servicio mobiliario = servicioExistente(20L, negocio, "1200.00");
        Paquete existente = paqueteExistente(5L, negocio);
        PaqueteServicio lineaPrevia = new PaqueteServicio();
        lineaPrevia.setPaquete(existente);
        lineaPrevia.setServicio(banquete);
        lineaPrevia.setPrecioUnitario(new BigDecimal("350.00"));
        existente.getItems().add(lineaPrevia);
        PaqueteDTO dto = paquete("5200.00", linea(10L, 3), linea(20L, 1));
        when(paqueteRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(paqueteRepository.existsByNombreInNegocio(1L, NOMBRE, 5L)).thenReturn(false);
        when(servicioRepository.findById(10L)).thenReturn(Optional.of(banquete));
        when(servicioRepository.findById(20L)).thenReturn(Optional.of(mobiliario));

        // Act
        paqueteService.update(negocio, 5L, dto);

        // Assert
        verify(paqueteRepository).existsByNombreInNegocio(1L, NOMBRE, 5L);
        verify(paqueteRepository).flush();
        ArgumentCaptor<Paquete> guardado = ArgumentCaptor.forClass(Paquete.class);
        verify(paqueteRepository).save(guardado.capture());
        Paquete paquete = guardado.getValue();
        assertSame(existente, paquete);
        assertEquals(new BigDecimal("5200.00"), paquete.getPrecio());
        assertEquals(2, paquete.getItems().size());
        assertEquals(3, paquete.getItems().get(0).getCantidad());
        assertEquals(new BigDecimal("350.00"), paquete.getItems().get(0).getPrecioUnitario());
        assertEquals(new BigDecimal("1200.00"), paquete.getItems().get(1).getPrecioUnitario());
    }
}