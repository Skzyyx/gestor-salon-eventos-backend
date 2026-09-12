package mx.gestorsalon.mapper;

import mx.gestorsalon.dto.DireccionDTO;
import mx.gestorsalon.model.Direccion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DireccionMapperTest {

    private DireccionMapper direccionMapper;

    @BeforeEach
    void setUp() {
        direccionMapper = new DireccionMapper();
    }

    @Test
    @DisplayName("toDTO debe retornar un DTO vacío cuando la entidad es null")
    void toDTO_conEntidadNull_retornaDtoVacio() {
        DireccionDTO dto = direccionMapper.toDTO(null);

        assertNotNull(dto);
        assertNull(dto.getCalle());
        assertNull(dto.getNumero());
        assertNull(dto.getColonia());
        assertNull(dto.getCiudad());
        assertNull(dto.getEstado());
        assertNull(dto.getCodigoPostal());
    }

    @Test
    @DisplayName("toDTO debe mapear todos los campos correctamente")
    void toDTO_conEntidadValida_mapeaCamposCorrectamente() {
        Direccion entity = new Direccion();
        entity.setCalle("Av. Hidalgo");
        entity.setNumero("123");
        entity.setColonia("Centro");
        entity.setCiudad("Obregón");
        entity.setEstado("Sonora");
        entity.setCodigoPostal("85000");

        DireccionDTO dto = direccionMapper.toDTO(entity);

        assertNotNull(dto);
        assertEquals("Av. Hidalgo", dto.getCalle());
        assertEquals("123", dto.getNumero());
        assertEquals("Centro", dto.getColonia());
        assertEquals("Obregón", dto.getCiudad());
        assertEquals("Sonora", dto.getEstado());
        assertEquals("85000", dto.getCodigoPostal());
    }

    @Test
    @DisplayName("toEntity debe retornar null cuando el DTO es null")
    void toEntity_conDtoNull_retornaNull() {
        Direccion entity = direccionMapper.toEntity(null);

        assertNull(entity);
    }

    @Test
    @DisplayName("toEntity debe mapear todos los campos correctamente")
    void toEntity_conDtoValido_mapeaCamposCorrectamente() {
        DireccionDTO dto = new DireccionDTO();
        dto.setCalle("Av. Hidalgo");
        dto.setNumero("123");
        dto.setColonia("Centro");
        dto.setCiudad("Obregón");
        dto.setEstado("Sonora");
        dto.setCodigoPostal("85000");

        Direccion entity = direccionMapper.toEntity(dto);

        assertNotNull(entity);
        assertEquals("Av. Hidalgo", entity.getCalle());
        assertEquals("123", entity.getNumero());
        assertEquals("Centro", entity.getColonia());
        assertEquals("Obregón", entity.getCiudad());
        assertEquals("Sonora", entity.getEstado());
        assertEquals("85000", entity.getCodigoPostal());
    }
}
