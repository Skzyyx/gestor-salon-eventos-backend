package mx.gestorsalon.repository;

import mx.gestorsalon.model.RolNegocio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolNegocioRepository extends JpaRepository<RolNegocio, Long> {

    List<RolNegocio> findByNegocioId(Long negocioId);

    Optional<RolNegocio> findByNegocioIdAndNombre(Long negocioId, String nombre);

    Optional<RolNegocio> findByIdAndNegocioId(Long id, Long negocioId);
}
