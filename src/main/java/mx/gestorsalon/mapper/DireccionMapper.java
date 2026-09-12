package mx.gestorsalon.mapper;

import mx.gestorsalon.dto.DireccionDTO;
import mx.gestorsalon.model.Direccion;
import org.springframework.stereotype.Component;

/**
 * Mapeo centralizado del value object {@link Direccion} <-> {@link DireccionDTO}.
 * Lo comparten Negocio y Cliente (y luego cualquier entidad con dirección), para no
 * repetir la conversión campo a campo en cada mapper.
 */
@Component
public class DireccionMapper {

    /**
     * Devuelve siempre un DTO (vacío, no null) para que el frontend reciba el objeto
     * y no tenga que protegerse contra null cuando la entidad aún no tiene dirección.
     */
    public DireccionDTO toDTO(Direccion d) {
        DireccionDTO dto = new DireccionDTO();
        if (d != null) {
            dto.setCalle(d.getCalle());
            dto.setNumero(d.getNumero());
            dto.setColonia(d.getColonia());
            dto.setCiudad(d.getCiudad());
            dto.setEstado(d.getEstado());
            dto.setCodigoPostal(d.getCodigoPostal());
        }
        return dto;
    }

    /**
     * Convierte el DTO entrante a value object. Devuelve null cuando no se envió
     * dirección, para no persistir un objeto embebido con todos sus campos vacíos.
     */
    public Direccion toEntity(DireccionDTO dto) {
        if (dto == null) {
            return null;
        }
        Direccion d = new Direccion();
        d.setCalle(dto.getCalle());
        d.setNumero(dto.getNumero());
        d.setColonia(dto.getColonia());
        d.setCiudad(dto.getCiudad());
        d.setEstado(dto.getEstado());
        d.setCodigoPostal(dto.getCodigoPostal());
        return d;
    }
}
