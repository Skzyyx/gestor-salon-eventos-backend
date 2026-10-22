package mx.gestorsalon.service;

import mx.gestorsalon.dto.ClienteDTO;
import mx.gestorsalon.dto.DireccionDTO;
import mx.gestorsalon.exception.AccesoDenegadoException;
import mx.gestorsalon.exception.RecursoNoEncontradoException;
import mx.gestorsalon.mapper.ClienteMapper;
import mx.gestorsalon.mapper.DireccionMapper;
import mx.gestorsalon.model.Cliente;
import mx.gestorsalon.model.Direccion;
import mx.gestorsalon.model.Negocio;
import mx.gestorsalon.repository.ClienteRepository;
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
class ClienteServiceTest {

    private static final String NOMBRE_ANTERIOR = "Laura Gómez";
    private static final String TELEFONO_ANTERIOR = "6871234567";

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ClienteMapper clienteMapper;
    @Mock
    private DireccionMapper direccionMapper;

    @InjectMocks
    private ClienteService clienteService;

    private Negocio negocio;

    /** Operación del servicio que recibe un id de cliente y debe validar que sea del negocio. */
    @FunctionalInterface
    private interface OperacionSobreCliente {
        void ejecutar(ClienteService service, Negocio negocio, Long clienteId);
    }

    static Stream<Arguments> operacionesSobreUnCliente() {
        return Stream.of(
                Arguments.of("update", (OperacionSobreCliente) (s, n, id) -> s.update(n, id, clienteDto())),
                Arguments.of("deactivate", (OperacionSobreCliente) ClienteService::deactivate));
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

    private static ClienteDTO clienteDto() {
        ClienteDTO dto = new ClienteDTO();
        dto.setNombre("Laura Gómez de Castro");
        dto.setTelefono("+52 687 765 4321");
        dto.setEmail("laura@correo.mx");
        return dto;
    }

    private static Cliente clienteExistente(Long id, Negocio duenio) {
        Cliente c = new Cliente();
        c.setId(id);
        c.setNegocio(duenio);
        c.setNombre(NOMBRE_ANTERIOR);
        c.setTelefono(TELEFONO_ANTERIOR);
        c.setEmail("laura.vieja@correo.mx");
        return c;
    }

    @Test
    @DisplayName("getAllByNegocio devuelve mapeados, en orden, todos los clientes del negocio")
    void getAllByNegocio_variosClientes_mapeaCadaUno() {
        // Arrange
        Cliente laura = clienteExistente(3L, negocio);
        Cliente pedro = clienteExistente(4L, negocio);
        ClienteDTO lauraDto = new ClienteDTO();
        ClienteDTO pedroDto = new ClienteDTO();
        when(clienteRepository.findByNegocioId(1L)).thenReturn(List.of(laura, pedro));
        when(clienteMapper.toDTO(laura)).thenReturn(lauraDto);
        when(clienteMapper.toDTO(pedro)).thenReturn(pedroDto);

        // Act
        List<ClienteDTO> resultado = clienteService.getAllByNegocio(1L);

        // Assert
        assertEquals(2, resultado.size());
        assertSame(lauraDto, resultado.get(0));
        assertSame(pedroDto, resultado.get(1));
    }

    @Test
    @DisplayName("create asigna el negocio del usuario al cliente y devuelve el DTO del cliente guardado")
    void create_clienteNuevo_asignaNegocioYDevuelveDtoGuardado() {
        // Arrange
        ClienteDTO dto = clienteDto();
        Cliente entidad = new Cliente();
        Cliente guardado = clienteExistente(3L, negocio);
        ClienteDTO respuesta = new ClienteDTO();
        when(clienteMapper.toEntity(dto)).thenReturn(entidad);
        when(clienteRepository.save(entidad)).thenReturn(guardado);
        when(clienteMapper.toDTO(guardado)).thenReturn(respuesta);

        // Act
        ClienteDTO resultado = clienteService.create(negocio, dto);

        // Assert
        assertSame(respuesta, resultado);
        assertSame(negocio, entidad.getNegocio());
    }

    @Test
    @DisplayName("update copia nombre, teléfono, correo y dirección del DTO y devuelve el cliente mapeado")
    void update_clientePropio_copiaDatosDeContactoYDireccion() {
        // Arrange
        Cliente existente = clienteExistente(3L, negocio);
        ClienteDTO dto = clienteDto();
        DireccionDTO direccionDto = new DireccionDTO();
        direccionDto.setCalle("Blvd. Juan de Dios Bátiz");
        dto.setDireccion(direccionDto);
        Direccion direccion = new Direccion();
        ClienteDTO respuesta = new ClienteDTO();
        when(clienteRepository.findById(3L)).thenReturn(Optional.of(existente));
        when(direccionMapper.toEntity(direccionDto)).thenReturn(direccion);
        when(clienteRepository.save(existente)).thenReturn(existente);
        when(clienteMapper.toDTO(existente)).thenReturn(respuesta);

        // Act
        ClienteDTO resultado = clienteService.update(negocio, 3L, dto);

        // Assert
        assertSame(respuesta, resultado);
        assertEquals("Laura Gómez de Castro", existente.getNombre());
        assertEquals("+52 687 765 4321", existente.getTelefono());
        assertEquals("laura@correo.mx", existente.getEmail());
        assertSame(direccion, existente.getDireccion());
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
        Cliente existente = clienteExistente(3L, negocio);
        existente.setActivo(activoActual);
        ClienteDTO dto = clienteDto();
        dto.setActivo(activoDto);
        when(clienteRepository.findById(3L)).thenReturn(Optional.of(existente));

        // Act
        clienteService.update(negocio, 3L, dto);

        // Assert
        assertEquals(esperado, existente.getActivo());
        verify(clienteRepository).save(existente);
    }

    @ParameterizedTest(name = "{0} de un cliente ajeno se rechaza")
    @DisplayName("update y deactivate rechazan un cliente de otro negocio sin modificarlo ni guardarlo")
    @MethodSource("operacionesSobreUnCliente")
    void updateYDeactivate_clienteDeOtroNegocio_lanzaAccesoDenegado(String operacion,
                                                                    OperacionSobreCliente accion) {
        // Arrange
        Cliente ajeno = clienteExistente(8L, negocioConId(2L));
        when(clienteRepository.findById(8L)).thenReturn(Optional.of(ajeno));

        // Act
        AccesoDenegadoException error = assertThrows(AccesoDenegadoException.class,
                () -> accion.ejecutar(clienteService, negocio, 8L));

        // Assert
        assertEquals("El cliente no pertenece a su negocio", error.getMessage());
        assertEquals(NOMBRE_ANTERIOR, ajeno.getNombre());
        assertTrue(ajeno.getActivo());
        verify(clienteRepository, never()).save(any());
        verifyNoInteractions(clienteMapper, direccionMapper);
    }

    @ParameterizedTest(name = "{0} de un id inexistente se rechaza")
    @DisplayName("update y deactivate avisan cuando el cliente no existe y no guardan nada")
    @MethodSource("operacionesSobreUnCliente")
    void updateYDeactivate_clienteInexistente_lanzaRecursoNoEncontrado(String operacion,
                                                                       OperacionSobreCliente accion) {
        // Arrange
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        RecursoNoEncontradoException error = assertThrows(RecursoNoEncontradoException.class,
                () -> accion.ejecutar(clienteService, negocio, 99L));

        // Assert
        assertEquals("Cliente no encontrado", error.getMessage());
        verify(clienteRepository, never()).save(any());
        verifyNoInteractions(clienteMapper, direccionMapper);
    }

    @Test
    @DisplayName("deactivate hace baja lógica: marca el cliente inactivo, lo guarda y nunca lo borra")
    void deactivate_clientePropio_quedaInactivoSinBorrarse() {
        // Arrange
        Cliente propio = clienteExistente(3L, negocio);
        when(clienteRepository.findById(3L)).thenReturn(Optional.of(propio));

        // Act
        clienteService.deactivate(negocio, 3L);

        // Assert
        assertFalse(propio.getActivo());
        assertEquals(NOMBRE_ANTERIOR, propio.getNombre());
        verify(clienteRepository).save(propio);
        verify(clienteRepository, never()).delete(any());
        verify(clienteRepository, never()).deleteById(any());
    }
}
