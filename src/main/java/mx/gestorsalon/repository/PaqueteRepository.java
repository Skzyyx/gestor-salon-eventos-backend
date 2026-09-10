package mx.gestorsalon.repository;

import mx.gestorsalon.model.Paquete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaqueteRepository extends JpaRepository<Paquete, Long> {

        /**
         * Trae los paquetes del negocio con sus líneas y el servicio de cada línea en
         * una sola consulta (evita N+1). items es la única colección en el fetch, así
         * que no hay MultipleBagFetchException.
         */
        @Query("SELECT DISTINCT p FROM Paquete p " +
                        "LEFT JOIN FETCH p.items i " +
                        "LEFT JOIN FETCH i.servicio " +
                        "WHERE p.negocio.id = :negocioId")
        List<Paquete> findByNegocioIdWithItems(@Param("negocioId") Long negocioId);

        @Query("SELECT COUNT(p) > 0 FROM Paquete p WHERE p.negocio.id = :negocioId " +
                        "AND LOWER(p.nombre) = LOWER(:nombre) " +
                        "AND p.activo = true " +
                        "AND (:id IS NULL OR p.id != :id)")
        boolean existsByNombreInNegocio(
                        @Param("negocioId") Long negocioId,
                        @Param("nombre") String nombre,
                        @Param("id") Long id);
}
