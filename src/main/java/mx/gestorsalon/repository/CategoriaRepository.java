package mx.gestorsalon.repository;

import mx.gestorsalon.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByNegocioId(Long negocioId);
}
