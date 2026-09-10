package mx.gestorsalon.repository;

import mx.gestorsalon.model.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {

       List<Servicio> findByNegocioIdAndActivoTrue(Long negocioId);

       List<Servicio> findByNegocioId(Long negocioId);

       @Query("SELECT COUNT(s) > 0 FROM Servicio s WHERE s.negocio.id = :negocioId " +
                     "AND LOWER(s.nombre) = LOWER(:nombre) " +
                     "AND s.activo = true " +
                     "AND (:id IS NULL OR s.id != :id)")
       boolean existsByNombreInNegocio(
                     @Param("negocioId") Long negocioId,
                     @Param("nombre") String nombre,
                     @Param("id") Long id);
}