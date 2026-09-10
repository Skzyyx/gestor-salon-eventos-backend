package mx.gestorsalon.repository;

import mx.gestorsalon.model.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public interface TurnoRepository extends JpaRepository<Turno, Long> {

       List<Turno> findByNegocioId(Long negocioId);

       @Query("SELECT COUNT(t) > 0 FROM Turno t WHERE t.negocio.id = :negocioId " +
                     "AND t.diaSemana = :diaSemana " +
                     "AND t.horaInicio < :horaFin AND t.horaFin > :horaInicio " +
                     "AND (:id IS NULL OR t.id != :id)")
       boolean existsOverlapping(
                     @Param("negocioId") Long negocioId,
                     @Param("diaSemana") DayOfWeek diaSemana,
                     @Param("horaInicio") LocalTime horaInicio,
                     @Param("horaFin") LocalTime horaFin,
                     @Param("id") Long id);
}
