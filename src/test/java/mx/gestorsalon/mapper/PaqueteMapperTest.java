package mx.gestorsalon.mapper;

import mx.gestorsalon.dto.PaqueteDTO;
import mx.gestorsalon.dto.ServicioDTO;
import mx.gestorsalon.model.Paquete;
import mx.gestorsalon.model.PaqueteServicio;
import mx.gestorsalon.model.Servicio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaqueteMapperTest {

    @Mock
    private ServicioMapper servicioMapper;

    private PaqueteMapper paqueteMapper;

    @BeforeEach
    void setUp() {
        paqueteMapper = new PaqueteMapper(servicioMapper);
    }

    @Test
    @DisplayName("toDTO debe retornar null si paquete es null")
    void toDTO_conPaqueteNull_retornaNull() {
        assertNull(paqueteMapper.toDTO(null));
        verifyNoInteractions(servicioMapper);
    }

    @Test
    @DisplayName("toDTO debe mapear paquete con items delegando a servicioMapper")
    void toDTO_conItems_mapeaCorrectamente() {
        Servicio servicio = new Servicio();
        servicio.setId(4L);
        servicio.setNombre("Música");

        ServicioDTO servicioDTO = new ServicioDTO();
        servicioDTO.setId(4L);
        servicioDTO.setNombre("Música");

        when(servicioMapper.toDTO(servicio)).thenReturn(servicioDTO);

        PaqueteServicio item = new PaqueteServicio();
        item.setServicio(servicio);
        item.setCantidad(2);
        item.setPrecioUnitario(BigDecimal.valueOf(500));

        Paquete paquete = new Paquete();
        paquete.setId(1L);
        paquete.setNombre("Paquete Oro");
        paquete.setDescripcion("Incluye todo");
        paquete.setPrecio(BigDecimal.valueOf(10000));
        paquete.setImagenUrl("https://img.com/oro.png");
        paquete.setActivo(true);
        paquete.setItems(List.of(item));

        PaqueteDTO dto = paqueteMapper.toDTO(paquete);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Paquete Oro", dto.getNombre());
        assertEquals(BigDecimal.valueOf(10000), dto.getPrecio());
        assertNotNull(dto.getItems());
        assertEquals(1, dto.getItems().size());
        assertEquals(4L, dto.getItems().get(0).getServicioId());
        assertEquals(2, dto.getItems().get(0).getCantidad());
        assertEquals(BigDecimal.valueOf(500), dto.getItems().get(0).getPrecioUnitario());
        assertEquals("Música", dto.getItems().get(0).getServicio().getNombre());

        verify(servicioMapper, times(1)).toDTO(servicio);
    }

    @Test
    @DisplayName("toDTO debe mapear paquete sin items correctamente")
    void toDTO_sinItems_mapeaCorrectamente() {
        Paquete paquete = new Paquete();
        paquete.setId(2L);
        paquete.setNombre("Paquete Básico");
        paquete.setItems(null);

        PaqueteDTO dto = paqueteMapper.toDTO(paquete);

        assertNotNull(dto);
        assertEquals(2L, dto.getId());
        assertNull(dto.getItems());
        verifyNoInteractions(servicioMapper);
    }

    @Test
    @DisplayName("toEntity debe retornar null si dto es null")
    void toEntity_conDtoNull_retornaNull() {
        assertNull(paqueteMapper.toEntity(null));
    }

    @Test
    @DisplayName("toEntity debe mapear campos planos correctamente cuando activo viene en dto")
    void toEntity_conDtoValido_mapeaCorrectamente() {
        PaqueteDTO dto = new PaqueteDTO();
        dto.setNombre("Paquete Plata");
        dto.setDescripcion("Intermedio");
        dto.setPrecio(BigDecimal.valueOf(7000));
        dto.setImagenUrl("https://img.com/plata.png");
        dto.setActivo(false);

        Paquete entity = paqueteMapper.toEntity(dto);

        assertNotNull(entity);
        assertEquals("Paquete Plata", entity.getNombre());
        assertEquals("Intermedio", entity.getDescripcion());
        assertEquals(BigDecimal.valueOf(7000), entity.getPrecio());
        assertEquals("https://img.com/plata.png", entity.getImagenUrl());
        assertFalse(entity.getActivo());
    }

    @Test
    @DisplayName("toEntity debe asignar activo=true por defecto si activo es null")
    void toEntity_conActivoNull_asignaTruePorDefecto() {
        PaqueteDTO dto = new PaqueteDTO();
        dto.setNombre("Paquete Plata");
        dto.setActivo(null);

        Paquete entity = paqueteMapper.toEntity(dto);

        assertNotNull(entity);
        assertTrue(entity.getActivo());
    }
}
