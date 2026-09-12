package mx.gestorsalon.mapper;

import mx.gestorsalon.dto.CategoriaDTO;
import mx.gestorsalon.dto.ServicioDTO;
import mx.gestorsalon.model.Categoria;
import mx.gestorsalon.model.Servicio;
import mx.gestorsalon.model.enums.TipoCobro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicioMapperTest {

    @Mock
    private CategoriaMapper categoriaMapper;

    private ServicioMapper servicioMapper;

    @BeforeEach
    void setUp() {
        servicioMapper = new ServicioMapper(categoriaMapper);
    }

    @Test
    @DisplayName("toDTO debe retornar null cuando servicio es null")
    void toDTO_conServicioNull_retornaNull() {
        assertNull(servicioMapper.toDTO(null));
        verifyNoInteractions(categoriaMapper);
    }

    @Test
    @DisplayName("toDTO debe mapear servicio con categoria delegando a categoriaMapper")
    void toDTO_conCategoria_mapeaCorrectamente() {
        Categoria categoria = new Categoria();
        categoria.setId(3L);
        categoria.setNombre("Bebidas");

        CategoriaDTO categoriaDTO = new CategoriaDTO();
        categoriaDTO.setId(3L);
        categoriaDTO.setNombre("Bebidas");

        when(categoriaMapper.toDTO(categoria)).thenReturn(categoriaDTO);

        Servicio servicio = new Servicio();
        servicio.setId(1L);
        servicio.setNombre("Barra Libre");
        servicio.setDescripcion("Refrescos y coctelería");
        servicio.setPrecio(BigDecimal.valueOf(1500));
        servicio.setImagenUrl("https://img.com/barra.png");
        servicio.setActivo(true);
        servicio.setTipoCobro(TipoCobro.POR_PERSONA);
        servicio.setCategoria(categoria);

        ServicioDTO dto = servicioMapper.toDTO(servicio);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Barra Libre", dto.getNombre());
        assertEquals(BigDecimal.valueOf(1500), dto.getPrecio());
        assertEquals(TipoCobro.POR_PERSONA, dto.getTipoCobro());
        assertEquals(3L, dto.getCategoriaId());
        assertNotNull(dto.getCategoria());
        assertEquals("Bebidas", dto.getCategoria().getNombre());

        verify(categoriaMapper, times(1)).toDTO(categoria);
    }

    @Test
    @DisplayName("toDTO debe mapear servicio sin categoria sin llamar a categoriaMapper")
    void toDTO_sinCategoria_mapeaCorrectamente() {
        Servicio servicio = new Servicio();
        servicio.setId(2L);
        servicio.setNombre("Luz y Sonido");
        servicio.setCategoria(null);

        ServicioDTO dto = servicioMapper.toDTO(servicio);

        assertNotNull(dto);
        assertNull(dto.getCategoriaId());
        assertNull(dto.getCategoria());
        verifyNoInteractions(categoriaMapper);
    }

    @Test
    @DisplayName("toEntity debe retornar null cuando dto es null")
    void toEntity_conDtoNull_retornaNull() {
        assertNull(servicioMapper.toEntity(null));
    }

    @Test
    @DisplayName("toEntity debe mapear campos correctamente cuando activo viene en dto")
    void toEntity_conDtoValido_mapeaCorrectamente() {
        ServicioDTO dto = new ServicioDTO();
        dto.setNombre("Meseros");
        dto.setDescripcion("Personal de servicio");
        dto.setPrecio(BigDecimal.valueOf(800));
        dto.setImagenUrl("https://img.com/mesero.png");
        dto.setTipoCobro(TipoCobro.POR_EVENTO);
        dto.setActivo(false);

        Servicio entity = servicioMapper.toEntity(dto);

        assertNotNull(entity);
        assertEquals("Meseros", entity.getNombre());
        assertEquals("Personal de servicio", entity.getDescripcion());
        assertEquals(BigDecimal.valueOf(800), entity.getPrecio());
        assertEquals(TipoCobro.POR_EVENTO, entity.getTipoCobro());
        assertFalse(entity.getActivo());
    }

    @Test
    @DisplayName("toEntity debe asignar activo=true por defecto si activo es null")
    void toEntity_conActivoNull_asignaTruePorDefecto() {
        ServicioDTO dto = new ServicioDTO();
        dto.setNombre("Meseros");
        dto.setActivo(null);

        Servicio entity = servicioMapper.toEntity(dto);

        assertNotNull(entity);
        assertTrue(entity.getActivo());
    }
}
