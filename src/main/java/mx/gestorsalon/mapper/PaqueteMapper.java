package mx.gestorsalon.mapper;

import lombok.RequiredArgsConstructor;
import mx.gestorsalon.dto.PaqueteDTO;
import mx.gestorsalon.dto.PaqueteServicioDTO;
import mx.gestorsalon.model.Paquete;
import mx.gestorsalon.model.PaqueteServicio;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PaqueteMapper {

    private final ServicioMapper servicioMapper;

    public PaqueteDTO toDTO(Paquete paquete) {
        if (paquete == null)
            return null;
        PaqueteDTO dto = new PaqueteDTO();
        dto.setId(paquete.getId());
        dto.setNombre(paquete.getNombre());
        dto.setDescripcion(paquete.getDescripcion());
        dto.setPrecio(paquete.getPrecio());
        dto.setImagenUrl(paquete.getImagenUrl());
        dto.setActivo(paquete.getActivo());

        if (paquete.getItems() != null) {
            dto.setItems(paquete.getItems().stream()
                    .map(this::toItemDTO)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    private PaqueteServicioDTO toItemDTO(PaqueteServicio item) {
        PaqueteServicioDTO dto = new PaqueteServicioDTO();
        dto.setServicioId(item.getServicio() != null ? item.getServicio().getId() : null);
        dto.setCantidad(item.getCantidad());
        dto.setPrecioUnitario(item.getPrecioUnitario());
        dto.setServicio(servicioMapper.toDTO(item.getServicio()));
        return dto;
    }

    /**
     * Mapea solo los campos planos. Las líneas (servicios + precio congelado) las
     * resuelve el Service desde la BD validando que pertenezcan al negocio.
     */
    public Paquete toEntity(PaqueteDTO dto) {
        if (dto == null)
            return null;
        Paquete paquete = new Paquete();
        paquete.setNombre(dto.getNombre());
        paquete.setDescripcion(dto.getDescripcion());
        paquete.setPrecio(dto.getPrecio());
        paquete.setImagenUrl(dto.getImagenUrl());
        paquete.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
        return paquete;
    }
}
