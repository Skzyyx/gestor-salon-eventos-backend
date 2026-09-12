package mx.gestorsalon.service;

import lombok.RequiredArgsConstructor;
import mx.gestorsalon.dto.DireccionDTO;
import mx.gestorsalon.dto.negocio.NegocioResponse;
import mx.gestorsalon.dto.negocio.NegocioUpdateRequest;
import mx.gestorsalon.mapper.NegocioMapper;
import mx.gestorsalon.model.Direccion;
import mx.gestorsalon.model.Negocio;
import mx.gestorsalon.repository.NegocioRepository;
import mx.gestorsalon.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NegocioService {

    private final NegocioRepository negocioRepository;
    private final SecurityUtils securityUtils;
    private final NegocioMapper negocioMapper;

    /**
     * Obtiene el negocio asociado al usuario que está logueado actualmente.
     */
    @Transactional(readOnly = true)
    public NegocioResponse getMiNegocio() {
        Negocio negocio = securityUtils.getNegocioActual();
        return negocioMapper.toResponse(negocio);
    }

    /**
     * Actualiza la información del negocio del usuario logueado.
     */
    @Transactional
    public NegocioResponse updateMiNegocio(NegocioUpdateRequest request) {
        Negocio negocio = securityUtils.getNegocioActual();

        // Si el Frontend mandó un nombre nuevo, lo actualizamos
        if (request.getNombre() != null && !request.getNombre().trim().isEmpty()) {
            negocio.setNombre(request.getNombre());
        }

        // CU-04: el slug es la "dirección web pública". Validamos antes de guardar.
        if (request.getSlug() != null && !request.getSlug().trim().isEmpty()) {
            String nuevoSlug = request.getSlug().trim().toLowerCase();

            // FA-04.2: solo se permiten minúsculas, números y guiones
            if (!nuevoSlug.matches("^[a-z0-9-]+$")) {
                throw new IllegalArgumentException(
                        "La dirección solo puede contener letras minúsculas, números y guiones");
            }

            // FA-04.1: el slug debe ser único en TODA la plataforma
            // (excluimos el propio negocio para que pueda reguardar su slug actual)
            if (negocioRepository.existsBySlugAndIdNot(nuevoSlug, negocio.getId())) {
                throw new IllegalArgumentException(
                        "Esta dirección ya está en uso por otro negocio");
            }

            negocio.setSlug(nuevoSlug);
        }

        if (request.getLogoUrl() != null) {
            negocio.setLogoUrl(request.getLogoUrl());
        }

        // CU-04: contacto (opcionales, se actualizan si vienen)
        if (request.getTelefono() != null)
            negocio.setTelefono(request.getTelefono());
        if (request.getEmail() != null)
            negocio.setEmail(request.getEmail());
        if (request.getDescripcion() != null)
            negocio.setDescripcion(request.getDescripcion());

        // CU-04: dirección (value object). Si el request la trae, la actualizamos
        // completa; reutilizamos la instancia existente o creamos una nueva.
        if (request.getDireccion() != null) {
            Direccion dir = negocio.getDireccion() != null ? negocio.getDireccion() : new Direccion();
            DireccionDTO dto = request.getDireccion();
            dir.setCalle(dto.getCalle());
            dir.setNumero(dto.getNumero());
            dir.setColonia(dto.getColonia());
            dir.setCiudad(dto.getCiudad());
            dir.setEstado(dto.getEstado());
            dir.setCodigoPostal(dto.getCodigoPostal());
            negocio.setDireccion(dir);
        }

        // Guardamos los cambios en MySQL
        Negocio negocioActualizado = negocioRepository.save(negocio);

        return negocioMapper.toResponse(negocioActualizado);
    }

}
