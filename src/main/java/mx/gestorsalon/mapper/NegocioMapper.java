package mx.gestorsalon.mapper;

import lombok.RequiredArgsConstructor;
import mx.gestorsalon.dto.negocio.NegocioResponse;
import mx.gestorsalon.model.Negocio;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NegocioMapper {

    private final DireccionMapper direccionMapper;

    public NegocioResponse toResponse(Negocio negocio) {
        if (negocio == null)
            return null;

        return NegocioResponse.builder()
                .id(negocio.getId())
                .nombre(negocio.getNombre())
                .slug(negocio.getSlug())
                .logoUrl(negocio.getLogoUrl())
                .activo(negocio.getActivo())
                // CU-04: contacto y dirección
                .telefono(negocio.getTelefono())
                .email(negocio.getEmail())
                .descripcion(negocio.getDescripcion())
                .direccion(direccionMapper.toDTO(negocio.getDireccion()))
                .build();
    }
}
