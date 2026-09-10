package mx.gestorsalon.service;

import lombok.RequiredArgsConstructor;
import mx.gestorsalon.dto.CategoriaDTO;
import mx.gestorsalon.exception.AccesoDenegadoException;
import mx.gestorsalon.exception.RecursoNoEncontradoException;
import mx.gestorsalon.mapper.CategoriaMapper;
import mx.gestorsalon.model.Categoria;
import mx.gestorsalon.model.Negocio;
import mx.gestorsalon.repository.CategoriaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper categoriaMapper;

    @Transactional(readOnly = true)
    public List<CategoriaDTO> getAllByNegocio(Long negocioId) {
        return categoriaRepository.findByNegocioId(negocioId)
                .stream()
                .map(categoriaMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoriaDTO create(Negocio negocio, CategoriaDTO dto) {
        Categoria categoria = categoriaMapper.toEntity(dto);
        // Le asignamos el negocio actual (Magia Multi-Tenant)
        categoria.setNegocio(negocio);
        categoria = categoriaRepository.save(categoria);
        return categoriaMapper.toDTO(categoria);
    }

    @Transactional
    public CategoriaDTO update(Negocio negocio, @NonNull Long categoriaId, CategoriaDTO dto) {
        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada"));

        // Validar que la categoría que intentan editar sí le pertenezca a su negocio
        if (!categoria.getNegocio().getId().equals(negocio.getId())) {
            throw new AccesoDenegadoException("La categoría no pertenece a su negocio");
        }

        categoria.setNombre(dto.getNombre());
        categoria.setDescripcion(dto.getDescripcion());
        categoria.setActiva(dto.getActiva() != null ? dto.getActiva() : true);

        categoria = categoriaRepository.save(categoria);
        return categoriaMapper.toDTO(categoria);
    }

    @Transactional
    public void deactivate(Negocio negocio, @NonNull Long categoriaId) {
        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada"));

        if (!categoria.getNegocio().getId().equals(negocio.getId())) {
            throw new AccesoDenegadoException("La categoría no pertenece a su negocio");
        }

        // En lugar de borrar de BD, solo desactivamos (Soft-delete)
        categoria.setActiva(false);
        categoriaRepository.save(categoria);
    }
}
