package mx.gestorsalon.mapper;

import lombok.RequiredArgsConstructor;
import mx.gestorsalon.dto.ServicioDTO;
import mx.gestorsalon.model.Servicio;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor // Añadir esto
public class ServicioMapper {

    private final CategoriaMapper categoriaMapper; // Añadir esto

    public ServicioDTO toDTO(Servicio servicio) {
        if (servicio == null)
            return null;
        ServicioDTO dto = new ServicioDTO();
        dto.setId(servicio.getId());
        dto.setNombre(servicio.getNombre());
        dto.setDescripcion(servicio.getDescripcion());
        dto.setPrecio(servicio.getPrecio());
        dto.setImagenUrl(servicio.getImagenUrl());
        dto.setActivo(servicio.getActivo());

        // NUEVO: Mapear Tipo de Cobro y Categoría
        dto.setTipoCobro(servicio.getTipoCobro());
        if (servicio.getCategoria() != null) {
            dto.setCategoriaId(servicio.getCategoria().getId());
            dto.setCategoria(categoriaMapper.toDTO(servicio.getCategoria()));
        }

        return dto;
    }

    public Servicio toEntity(ServicioDTO dto) {
        if (dto == null)
            return null;
        Servicio servicio = new Servicio();
        servicio.setNombre(dto.getNombre());
        servicio.setDescripcion(dto.getDescripcion());
        servicio.setPrecio(dto.getPrecio());
        servicio.setImagenUrl(dto.getImagenUrl());

        // NUEVO: Mapear Tipo de Cobro
        servicio.setTipoCobro(dto.getTipoCobro());
        // (La categoría como Entidad la buscaremos en la base de datos dentro del
        // Service)

        if (dto.getActivo() != null) {
            servicio.setActivo(dto.getActivo());
        } else {
            servicio.setActivo(true);
        }
        return servicio;
    }
}
