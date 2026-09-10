package mx.gestorsalon.mapper;

import mx.gestorsalon.dto.CategoriaDTO;
import mx.gestorsalon.model.Categoria;
import org.springframework.stereotype.Component;

@Component
public class CategoriaMapper {

    public CategoriaDTO toDTO(Categoria categoria) {
        if (categoria == null)
            return null;
        CategoriaDTO dto = new CategoriaDTO();
        dto.setId(categoria.getId());
        dto.setNombre(categoria.getNombre());
        dto.setDescripcion(categoria.getDescripcion());
        dto.setActiva(categoria.getActiva());
        return dto;
    }

    public Categoria toEntity(CategoriaDTO dto) {
        if (dto == null)
            return null;
        Categoria categoria = new Categoria();
        categoria.setNombre(dto.getNombre());
        categoria.setDescripcion(dto.getDescripcion());

        if (dto.getActiva() != null) {
            categoria.setActiva(dto.getActiva());
        } else {
            categoria.setActiva(true);
        }
        return categoria;
    }
}
