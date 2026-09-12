package mx.gestorsalon.mapper;

import mx.gestorsalon.dto.ClienteDTO;
import mx.gestorsalon.dto.DireccionDTO;
import mx.gestorsalon.model.Cliente;
import mx.gestorsalon.model.Direccion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteMapperTest {

    @Mock
    private DireccionMapper direccionMapper;

    private ClienteMapper clienteMapper;

    @BeforeEach
    void setUp() {
        clienteMapper = new ClienteMapper(direccionMapper);
    }

    @Test
    @DisplayName("toDTO debe retornar null cuando el cliente es null")
    void toDTO_conClienteNull_retornaNull() {
        ClienteDTO dto = clienteMapper.toDTO(null);

        assertNull(dto);
        verifyNoInteractions(direccionMapper);
    }

    @Test
    @DisplayName("toDTO debe mapear campos correctamente y delegar direccion")
    void toDTO_conClienteValido_mapeaCorrectamente() {
        Direccion direccion = new Direccion();
        direccion.setCalle("Calle Roble");

        DireccionDTO direccionDTO = new DireccionDTO();
        direccionDTO.setCalle("Calle Roble");

        when(direccionMapper.toDTO(direccion)).thenReturn(direccionDTO);

        Cliente cliente = new Cliente();
        cliente.setId(5L);
        cliente.setNombre("Carlos Pérez");
        cliente.setTelefono("6449876543");
        cliente.setEmail("carlos@gmail.com");
        cliente.setActivo(true);
        cliente.setDireccion(direccion);

        ClienteDTO dto = clienteMapper.toDTO(cliente);

        assertNotNull(dto);
        assertEquals(5L, dto.getId());
        assertEquals("Carlos Pérez", dto.getNombre());
        assertEquals("6449876543", dto.getTelefono());
        assertEquals("carlos@gmail.com", dto.getEmail());
        assertTrue(dto.getActivo());
        assertNotNull(dto.getDireccion());
        assertEquals("Calle Roble", dto.getDireccion().getCalle());

        verify(direccionMapper, times(1)).toDTO(direccion);
    }

    @Test
    @DisplayName("toEntity debe retornar null cuando el DTO es null")
    void toEntity_conDtoNull_retornaNull() {
        Cliente entity = clienteMapper.toEntity(null);

        assertNull(entity);
        verifyNoInteractions(direccionMapper);
    }

    @Test
    @DisplayName("toEntity debe mapear campos correctamente y delegar direccion cuando activo viene en el DTO")
    void toEntity_conDtoValidoYActivoPresente_mapeaCorrectamente() {
        DireccionDTO direccionDTO = new DireccionDTO();
        direccionDTO.setCalle("Calle Sauces");

        Direccion direccion = new Direccion();
        direccion.setCalle("Calle Sauces");

        when(direccionMapper.toEntity(direccionDTO)).thenReturn(direccion);

        ClienteDTO dto = new ClienteDTO();
        dto.setNombre("Laura Garza");
        dto.setTelefono("6441112233");
        dto.setEmail("laura@gmail.com");
        dto.setActivo(false);
        dto.setDireccion(direccionDTO);

        Cliente entity = clienteMapper.toEntity(dto);

        assertNotNull(entity);
        assertEquals("Laura Garza", entity.getNombre());
        assertEquals("6441112233", entity.getTelefono());
        assertEquals("laura@gmail.com", entity.getEmail());
        assertFalse(entity.getActivo());
        assertNotNull(entity.getDireccion());
        assertEquals("Calle Sauces", entity.getDireccion().getCalle());

        verify(direccionMapper, times(1)).toEntity(direccionDTO);
    }

    @Test
    @DisplayName("toEntity debe asignar activo=true por defecto si el DTO tiene activo null")
    void toEntity_conActivoNull_asignaTruePorDefecto() {
        ClienteDTO dto = new ClienteDTO();
        dto.setNombre("Laura Garza");
        dto.setTelefono("6441112233");
        dto.setActivo(null);

        Cliente entity = clienteMapper.toEntity(dto);

        assertNotNull(entity);
        assertTrue(entity.getActivo());
    }
}
