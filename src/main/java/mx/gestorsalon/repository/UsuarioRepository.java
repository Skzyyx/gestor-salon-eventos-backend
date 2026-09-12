package mx.gestorsalon.repository;

import mx.gestorsalon.model.enums.Rol;
import mx.gestorsalon.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    List<Usuario> findByNegocioIdAndRol(Long negocioId, Rol rol);

    Optional<Usuario> findByIdAndNegocioId(Long id, Long negocioId);

    long countByNegocioIdAndRolAndActivoTrue(Long negocioId, Rol rol);

    /**
     * Igual que findByCorreo, pero trae en la MISMA query el rolNegocio y sus
     * permisos. Es la que usa el login (loadUserByUsername): como las authorities
     * se leen FUERA de la transacción (open-in-view=false), sin este JOIN FETCH
     * reventaría con LazyInitException al hacer
     * usuario.getRolNegocio().getPermisos().
     *
     * LEFT JOIN (no JOIN a secas): rolNegocio es NULL para ADMIN/SUPERADMIN; con un
     * INNER JOIN esos usuarios NO se encontrarían y nadie podría loguear como
     * admin.
     */
    @Query("""
            SELECT u FROM Usuario u
            LEFT JOIN FETCH u.rolNegocio rn
            LEFT JOIN FETCH rn.permisos
            WHERE u.correo = :correo
            """)
    Optional<Usuario> findByCorreoConPermisos(@Param("correo") String correo);
}
