package mx.gestorsalon.mapper;

import mx.gestorsalon.dto.TurnoDTO;
import mx.gestorsalon.model.Turno;
import org.springframework.stereotype.Component;

@Component
public class TurnoMapper {

    public TurnoDTO toDTO(Turno turno) {
        if (turno == null) return null;
        TurnoDTO dto = new TurnoDTO();
        dto.setId(turno.getId());
        dto.setDiaSemana(turno.getDiaSemana());
        dto.setTipoTurno(turno.getTipoTurno());
        dto.setHoraInicio(turno.getHoraInicio());
        dto.setHoraFin(turno.getHoraFin());
        dto.setActivo(turno.getActivo());
        return dto;
    }

    public Turno toEntity(TurnoDTO dto) {
        if (dto == null) return null;
        Turno turno = new Turno();
        turno.setDiaSemana(dto.getDiaSemana());
        turno.setTipoTurno(dto.getTipoTurno());
        turno.setHoraInicio(dto.getHoraInicio());
        turno.setHoraFin(dto.getHoraFin());
        if (dto.getActivo() != null) {
            turno.setActivo(dto.getActivo());
        } else {
            turno.setActivo(true);
        }
        return turno;
    }
}
