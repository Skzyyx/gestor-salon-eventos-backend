package mx.gestorsalon.service;

import mx.gestorsalon.dto.ServicioDTO;
import mx.gestorsalon.exception.AccesoDenegadoException;
import mx.gestorsalon.exception.RecursoNoEncontradoException;
import mx.gestorsalon.mapper.ServicioMapper;
import mx.gestorsalon.model.Categoria;
import mx.gestorsalon.model.Negocio;
import mx.gestorsalon.model.Servicio;
import mx.gestorsalon.repository.CategoriaRepository;
import mx.gestorsalon.repository.ServicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServicioService {

    private final ServicioRepository servicioRepository;

    private final ServicioMapper servicioMapper;

    private final CategoriaRepository categoriaRepository;

    @Transactional(readOnly = true)
    public List<ServicioDTO> getAllByNegocio(Long negocioId) {
        return servicioRepository.findByNegocioId(negocioId)
                .stream()
                .map(servicioMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ServicioDTO create(Negocio negocio, ServicioDTO dto) {
        validarServicio(negocio.getId(), dto, null);

        Servicio servicio = servicioMapper.toEntity(dto);
        servicio.setNegocio(negocio);
        // NUEVO: Buscar y asignar la categoría de forma segura
        asignarCategoriaSiExiste(negocio, dto.getCategoriaId(), servicio);
        servicio = servicioRepository.save(servicio);

        return servicioMapper.toDTO(servicio);
    }

    @Transactional
    public ServicioDTO update(Negocio negocio, Long servicioId, ServicioDTO dto) {
        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));

        if (!servicio.getNegocio().getId().equals(negocio.getId())) {
            throw new AccesoDenegadoException("El servicio no pertenece a su negocio");
        }

        validarServicio(negocio.getId(), dto, servicioId);

        servicio.setNombre(dto.getNombre());
        servicio.setDescripcion(dto.getDescripcion());
        servicio.setPrecio(dto.getPrecio());
        servicio.setImagenUrl(dto.getImagenUrl());

        // NUEVO: Actualizar Tipo de Cobro y Categoría
        servicio.setTipoCobro(dto.getTipoCobro());
        asignarCategoriaSiExiste(negocio, dto.getCategoriaId(), servicio);
        if (dto.getActivo() != null) {
            servicio.setActivo(dto.getActivo());
        }

        servicio = servicioRepository.save(servicio);
        return servicioMapper.toDTO(servicio);
    }

    @Transactional
    public void deactivate(Negocio negocio, Long servicioId) {
        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado"));

        if (!servicio.getNegocio().getId().equals(negocio.getId())) {
            throw new AccesoDenegadoException("El servicio no pertenece a su negocio");
        }

        servicio.setActivo(false);
        servicioRepository.save(servicio);
    }

    private void validarServicio(Long negocioId, ServicioDTO dto, Long idExcluir) {
        if (dto.getPrecio() == null || dto.getPrecio().signum() <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a $0");
        }

        boolean nombreDuplicado = servicioRepository.existsByNombreInNegocio(
                negocioId, dto.getNombre(), idExcluir);

        if (nombreDuplicado) {
            throw new IllegalArgumentException("Ya existe un servicio activo con ese nombre");
        }
    }

    private void asignarCategoriaSiExiste(Negocio negocio, Long categoriaId, Servicio servicio) {
        if (categoriaId != null) {
            Categoria categoria = categoriaRepository.findById(categoriaId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada"));

            if (!categoria.getNegocio().getId().equals(negocio.getId())) {
                throw new AccesoDenegadoException("La categoría no pertenece a su negocio");
            }
            servicio.setCategoria(categoria);
        } else {
            servicio.setCategoria(null); // Quitarle la categoría si mandan null
        }
    }

}
