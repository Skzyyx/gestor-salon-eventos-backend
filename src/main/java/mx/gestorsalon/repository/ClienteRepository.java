package mx.gestorsalon.repository;

import mx.gestorsalon.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    List<Cliente> findByNegocioId(Long negocioId);

    /**
     * Detecta teléfonos repetidos dentro de un mismo negocio (clientes activos).
     * El frontend lo usa para una ADVERTENCIA no bloqueante (FA-08.3); el backend
     * no rechaza el guardado.
     */
    @Query("SELECT COUNT(c) > 0 FROM Cliente c WHERE c.negocio.id = :negocioId " +
            "AND c.telefono = :telefono " +
            "AND c.activo = true " +
            "AND (:id IS NULL OR c.id != :id)")
    boolean existsByTelefonoInNegocio(
            @Param("negocioId") Long negocioId,
            @Param("telefono") String telefono,
            @Param("id") Long id);
}
