package mx.gestorsalon.service;

import lombok.RequiredArgsConstructor;
import mx.gestorsalon.dto.PaqueteDTO;
import mx.gestorsalon.dto.PaqueteServicioDTO;
import mx.gestorsalon.exception.AccesoDenegadoException;
import mx.gestorsalon.exception.RecursoNoEncontradoException;
import mx.gestorsalon.mapper.PaqueteMapper;
import mx.gestorsalon.model.Negocio;
import mx.gestorsalon.model.Paquete;
import mx.gestorsalon.model.PaqueteServicio;
import mx.gestorsalon.model.Servicio;
import mx.gestorsalon.repository.PaqueteRepository;
import mx.gestorsalon.repository.ServicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaqueteService {

    private final PaqueteRepository paqueteRepository;
    private final ServicioRepository servicioRepository;
    private final PaqueteMapper paqueteMapper;

    @Transactional(readOnly = true)
    public List<PaqueteDTO> getAllByNegocio(Long negocioId) {
        return paqueteRepository.findByNegocioIdWithItems(negocioId)
                .stream()
                .map(paqueteMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public PaqueteDTO create(Negocio negocio, PaqueteDTO dto) {
        validarPaquete(negocio.getId(), dto, null);

        Paquete paquete = paqueteMapper.toEntity(dto);
        paquete.setNegocio(negocio);
        // Sin precios previos: cada línea congela el precio actual del servicio.
        aplicarItems(negocio, paquete, dto.getItems(), Map.of());

        paquete = paqueteRepository.save(paquete);
        return paqueteMapper.toDTO(paquete);
    }

    @Transactional
    public PaqueteDTO update(Negocio negocio, Long paqueteId, PaqueteDTO dto) {
        Paquete paquete = obtenerPropio(negocio, paqueteId);
        validarPaquete(negocio.getId(), dto, paqueteId);

        // Conservamos el precio "de ese momento" de las líneas que ya existían;
        // solo las nuevas congelan el precio actual del servicio.
        Map<Long, BigDecimal> preciosPrevios = paquete.getItems().stream()
                .collect(Collectors.toMap(
                        i -> i.getServicio().getId(),
                        PaqueteServicio::getPrecioUnitario,
                        (a, b) -> a));

        paquete.setNombre(dto.getNombre());
        paquete.setDescripcion(dto.getDescripcion());
        paquete.setPrecio(dto.getPrecio());
        paquete.setImagenUrl(dto.getImagenUrl());
        if (dto.getActivo() != null) {
            paquete.setActivo(dto.getActivo());
        }

        // orphanRemoval elimina las líneas anteriores; reconstruimos desde el DTO.
        // El flush fuerza el DELETE de las viejas ANTES de insertar las nuevas: sin él,
        // Hibernate inserta primero y choca con el UNIQUE(paquete_id, servicio_id).
        paquete.getItems().clear();
        paqueteRepository.flush();
        aplicarItems(negocio, paquete, dto.getItems(), preciosPrevios);

        paquete = paqueteRepository.save(paquete);
        return paqueteMapper.toDTO(paquete);
    }

    @Transactional
    public void deactivate(Negocio negocio, Long paqueteId) {
        Paquete paquete = obtenerPropio(negocio, paqueteId);
        paquete.setActivo(false); // Soft-delete
        paqueteRepository.save(paquete);
    }

    private void validarPaquete(Long negocioId, PaqueteDTO dto, Long idExcluir) {
        if (dto.getPrecio() == null || dto.getPrecio().signum() <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a $0");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new IllegalArgumentException("Un paquete debe incluir al menos un servicio");
        }
        long distintos = dto.getItems().stream()
                .map(PaqueteServicioDTO::getServicioId)
                .distinct()
                .count();
        if (distintos != dto.getItems().size()) {
            throw new IllegalArgumentException("Hay un servicio repetido en el paquete; usa la cantidad en su lugar");
        }
        if (paqueteRepository.existsByNombreInNegocio(negocioId, dto.getNombre(), idExcluir)) {
            throw new IllegalArgumentException("Ya existe un paquete activo con ese nombre");
        }
    }

    /**
     * Construye las líneas del paquete validando tenant y congelando el precio unitario:
     * reusa el precio previo si la línea ya existía, o toma el precio actual del servicio
     * si es nueva.
     */
    private void aplicarItems(Negocio negocio, Paquete paquete, List<PaqueteServicioDTO> itemsDto,
            Map<Long, BigDecimal> preciosPrevios) {
        for (PaqueteServicioDTO itemDto : itemsDto) {
            Servicio servicio = servicioRepository.findById(itemDto.getServicioId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Uno de los servicios seleccionados no existe"));
            if (!servicio.getNegocio().getId().equals(negocio.getId())) {
                throw new AccesoDenegadoException("Un servicio seleccionado no pertenece a su negocio");
            }

            int cantidad = (itemDto.getCantidad() == null || itemDto.getCantidad() < 1)
                    ? 1
                    : itemDto.getCantidad();

            PaqueteServicio linea = new PaqueteServicio();
            linea.setPaquete(paquete);
            linea.setServicio(servicio);
            linea.setCantidad(cantidad);
            linea.setPrecioUnitario(preciosPrevios.getOrDefault(servicio.getId(), servicio.getPrecio()));
            paquete.getItems().add(linea);
        }
    }

    private Paquete obtenerPropio(Negocio negocio, Long paqueteId) {
        Paquete paquete = paqueteRepository.findById(paqueteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paquete no encontrado"));
        if (!paquete.getNegocio().getId().equals(negocio.getId())) {
            throw new AccesoDenegadoException("El paquete no pertenece a su negocio");
        }
        return paquete;
    }
}
