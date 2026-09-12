package mx.gestorsalon.mapper;

import mx.gestorsalon.dto.DireccionDTO;
import mx.gestorsalon.dto.negocio.NegocioResponse;
import mx.gestorsalon.model.Direccion;
import mx.gestorsalon.model.Negocio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NegocioMapperTest {

    @Mock
    private DireccionMapper direccionMapper;

    private NegocioMapper negocioMapper;

    @BeforeEach
    void setUp() {
        negocioMapper = new NegocioMapper(direccionMapper);
    }

    @Test
    @DisplayName("toResponse debe retornar null cuando el negocio es null")
    void toResponse_conNegocioNull_retornaNull() {
        NegocioResponse response = negocioMapper.toResponse(null);

        assertNull(response);
        verifyNoInteractions(direccionMapper);
    }

    @Test
    @DisplayName("toResponse debe mapear todos los campos correctamente y delegar direccion")
    void toResponse_conNegocioValido_mapeaCamposYDelegaDireccion() {
        Direccion direccion = new Direccion();
        direccion.setCalle("Av. Principal");

        DireccionDTO direccionDTO = new DireccionDTO();
        direccionDTO.setCalle("Av. Principal");

        when(direccionMapper.toDTO(direccion)).thenReturn(direccionDTO);

        Negocio negocio = new Negocio();
        negocio.setId(10L);
        negocio.setNombre("Salón Fiesta");
        negocio.setSlug("salon-fiesta");
        negocio.setLogoUrl("https://img.com/logo.png");
        negocio.setActivo(true);
        negocio.setTelefono("6441234567");
        negocio.setEmail("contacto@salonfiesta.com");
        negocio.setDescripcion("El mejor salón");
        negocio.setDireccion(direccion);

        NegocioResponse response = negocioMapper.toResponse(negocio);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Salón Fiesta", response.getNombre());
        assertEquals("salon-fiesta", response.getSlug());
        assertEquals("https://img.com/logo.png", response.getLogoUrl());
        assertTrue(response.getActivo());
        assertEquals("6441234567", response.getTelefono());
        assertEquals("contacto@salonfiesta.com", response.getEmail());
        assertEquals("El mejor salón", response.getDescripcion());
        assertNotNull(response.getDireccion());
        assertEquals("Av. Principal", response.getDireccion().getCalle());

        verify(direccionMapper, times(1)).toDTO(direccion);
    }
}
