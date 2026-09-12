package mx.gestorsalon.mapper;

import mx.gestorsalon.dto.CategoriaDTO;
import mx.gestorsalon.model.Categoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CategoriaMapperTest {

    private CategoriaMapper categoriaMapper;

    @BeforeEach
    void setUp() {
        categoriaMapper = new CategoriaMapper();
    }

    @Test
    @DisplayName("toDTO debe retornar null si categoria es null")
    void toDTO_conCategoriaNull_retornaNull() {
        assertNull(categoriaMapper.toDTO(null));
    }

    @Test
    @DisplayName("toDTO debe mapear todos los campos de categoria")
    void toDTO_conCategoriaValida_mapeaCorrectamente() {
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("Mobiliario");
        categoria.setDescripcion("Mesas y sillas");
        categoria.setActiva(true);

        CategoriaDTO dto = categoriaMapper.toDTO(categoria);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Mobiliario", dto.getNombre());
        assertEquals("Mesas y sillas", dto.getDescripcion());
        assertTrue(dto.getActiva());
    }

    @Test
    @DisplayName("toEntity debe retornar null si dto es null")
    void toEntity_conDtoNull_retornaNull() {
        assertNull(categoriaMapper.toEntity(null));
    }

    @Test
    @DisplayName("toEntity debe mapear todos los campos cuando activa viene en dto")
    void toEntity_conDtoValido_mapeaCorrectamente() {
        CategoriaDTO dto = new CategoriaDTO();
        dto.setNombre("Mobiliario");
        dto.setDescripcion("Mesas y sillas");
        dto.setActiva(false);

        Categoria entity = categoriaMapper.toEntity(dto);

        assertNotNull(entity);
        assertEquals("Mobiliario", entity.getNombre());
        assertEquals("Mesas y sillas", entity.getDescripcion());
        assertFalse(entity.getActiva());
    }

    @Test
    @DisplayName("toEntity debe asignar activa=true por defecto si activa es null")
    void toEntity_conActivaNull_asignaTruePorDefecto() {
        CategoriaDTO dto = new CategoriaDTO();
        dto.setNombre("Mobiliario");
        dto.setActiva(null);

        Categoria entity = categoriaMapper.toEntity(dto);

        assertNotNull(entity);
        assertTrue(entity.getActiva());
    }
}
