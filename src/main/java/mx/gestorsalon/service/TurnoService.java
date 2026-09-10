package mx.gestorsalon.service;

import mx.gestorsalon.dto.TurnoDTO;
import mx.gestorsalon.exception.AccesoDenegadoException;
import mx.gestorsalon.exception.RecursoNoEncontradoException;
import mx.gestorsalon.mapper.TurnoMapper;
import mx.gestorsalon.model.Negocio;
import mx.gestorsalon.model.Turno;
import mx.gestorsalon.repository.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TurnoService {

    private final TurnoRepository turnoRepository;

    private final TurnoMapper turnoMapper;

    @Transactional(readOnly = true)
    public List<TurnoDTO> getAllByNegocio(Long negocioId) {
        return turnoRepository.findByNegocioId(negocioId)
                .stream()
                .map(turnoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public TurnoDTO create(Negocio negocio, TurnoDTO dto) {
        validarTurno(negocio.getId(), dto, null);

        Turno turno = turnoMapper.toEntity(dto);
        turno.setNegocio(negocio);
        turno = turnoRepository.save(turno);

        return turnoMapper.toDTO(turno);
    }

    @Transactional
    public TurnoDTO update(Negocio negocio, Long turnoId, TurnoDTO dto) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado"));

        if (!turno.getNegocio().getId().equals(negocio.getId())) {
            throw new AccesoDenegadoException("El turno no pertenece a su negocio");
        }

        validarTurno(negocio.getId(), dto, turnoId);

        turno.setDiaSemana(dto.getDiaSemana());
        turno.setTipoTurno(dto.getTipoTurno());
        turno.setHoraInicio(dto.getHoraInicio());
        turno.setHoraFin(dto.getHoraFin());
        turno.setActivo(dto.getActivo() != null ? dto.getActivo() : true);

        turno = turnoRepository.save(turno);
        return turnoMapper.toDTO(turno);
    }

    @Transactional
    public void delete(Negocio negocio, Long turnoId) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado"));

        if (!turno.getNegocio().getId().equals(negocio.getId())) {
            throw new AccesoDenegadoException("El turno no pertenece a su negocio");
        }

        turnoRepository.delete(turno);
    }

    private void validarTurno(Long negocioId, TurnoDTO dto, Long idExcluir) {
        if (!dto.getHoraInicio().isBefore(dto.getHoraFin())) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin");
        }

        boolean solapado = turnoRepository.existsOverlapping(
                negocioId, dto.getDiaSemana(), dto.getHoraInicio(), dto.getHoraFin(), idExcluir);

        if (solapado) {
            throw new IllegalArgumentException("El horario se solapa con otro turno existente en el mismo día");
        }
    }
}
