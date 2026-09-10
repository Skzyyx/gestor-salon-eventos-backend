package mx.gestorsalon.repository;

import mx.gestorsalon.model.Negocio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NegocioRepository extends JpaRepository<Negocio, Long> {

    // Spring Boot escribirá la consulta SQL automáticamente solo con leer el nombre
    // del método
    Optional<Negocio> findBySlug(String slug);

    // CU-04: ¿Existe OTRO negocio (distinto de este id) que ya use este slug?
    // Lo usamos para validar la unicidad antes de guardar y dar un mensaje
    // amigable.
    boolean existsBySlugAndIdNot(String slug, Long id);

}
