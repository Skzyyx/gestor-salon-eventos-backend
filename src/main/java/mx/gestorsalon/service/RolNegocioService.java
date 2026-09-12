package mx.gestorsalon.service;

import lombok.RequiredArgsConstructor;
import mx.gestorsalon.exception.RecursoNoEncontradoException;
import mx.gestorsalon.exception.AccesoDenegadoException;
import mx.gestorsalon.model.Negocio;
import mx.gestorsalon.model.enums.Permiso;
import mx.gestorsalon.model.RolNegocio;
import mx.gestorsalon.repository.RolNegocioRepository;
import mx.gestorsalon.security.CustomUserDetails;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolNegocioService {

    private final RolNegocioRepository rolNegocioRepository;

    @Transactional(readOnly = true)
    public List<RolNegocio> obtenerRolesPorNegocio(Long negocioId) {
        return rolNegocioRepository.findByNegocioId(negocioId);
    }

    @Transactional(readOnly = true)
    public RolNegocio obtenerPorIdYNegocio(Long id, Long negocioId) {
        return rolNegocioRepository.findByIdAndNegocioId(id, negocioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol no encontrado en este negocio"));
    }

    /**
     * Invariante de "Techo de privilegio": El actor no puede conceder permisos que él mismo no posee.
     */
    public void validarTechoDePrivilegio(CustomUserDetails actorDetails, Set<Permiso> permisosObjetivo) {
        Set<String> permisosActor = actorDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        // Traducimos los Permisos objetivo a la misma nomenclatura "PERM_..."
        for (Permiso permiso : permisosObjetivo) {
            String authorityEsperada = "PERM_" + permiso.name();
            if (!permisosActor.contains(authorityEsperada)) {
                throw new AccesoDenegadoException("No puedes conceder permisos que no tienes: " + permiso.name());
            }
        }
    }

    @Transactional
    public void sembrarPlantillasSiNoExisten(Negocio negocio) {
        crearRolSiNoExiste(negocio, "Gerente", "Rol administrativo con control operativo amplio",
                EnumSet.of(
                        Permiso.SERVICIO_VER, Permiso.SERVICIO_CREAR, Permiso.SERVICIO_EDITAR, Permiso.SERVICIO_ELIMINAR,
                        Permiso.CATEGORIA_VER, Permiso.CATEGORIA_GESTIONAR,
                        Permiso.PAQUETE_VER, Permiso.PAQUETE_CREAR, Permiso.PAQUETE_EDITAR, Permiso.PAQUETE_ELIMINAR,
                        Permiso.CLIENTE_VER, Permiso.CLIENTE_CREAR, Permiso.CLIENTE_EDITAR, Permiso.CLIENTE_ELIMINAR,
                        Permiso.TURNO_VER, Permiso.TURNO_CREAR, Permiso.TURNO_EDITAR, Permiso.TURNO_CANCELAR,
                        Permiso.NEGOCIO_VER_PERFIL,
                        Permiso.EQUIPO_VER, Permiso.EQUIPO_GESTIONAR,
                        Permiso.ROL_VER
                )
        );

        crearRolSiNoExiste(negocio, "Coordinador de eventos", "Control de agenda y lectura de catálogo",
                EnumSet.of(
                        Permiso.SERVICIO_VER, Permiso.CATEGORIA_VER, Permiso.PAQUETE_VER,
                        Permiso.CLIENTE_VER, Permiso.CLIENTE_CREAR, Permiso.CLIENTE_EDITAR,
                        Permiso.TURNO_VER, Permiso.TURNO_CREAR, Permiso.TURNO_EDITAR, Permiso.TURNO_CANCELAR,
                        Permiso.NEGOCIO_VER_PERFIL
                )
        );

        crearRolSiNoExiste(negocio, "Recepcionista", "Atención a clientes y lectura",
                EnumSet.of(
                        Permiso.SERVICIO_VER, Permiso.CATEGORIA_VER, Permiso.PAQUETE_VER,
                        Permiso.CLIENTE_VER, Permiso.CLIENTE_CREAR,
                        Permiso.TURNO_VER, Permiso.TURNO_CREAR,
                        Permiso.NEGOCIO_VER_PERFIL
                )
        );
    }

    private void crearRolSiNoExiste(Negocio negocio, String nombre, String descripcion, Set<Permiso> permisos) {
        if (rolNegocioRepository.findByNegocioIdAndNombre(negocio.getId(), nombre).isEmpty()) {
            RolNegocio rol = new RolNegocio();
            rol.setNegocio(negocio);
            rol.setNombre(nombre);
            rol.setDescripcion(descripcion);
            rol.setEsSistema(true); // Plantillas de sistema
            rol.setPermisos(permisos);
            rolNegocioRepository.save(rol);
        }
    }
}
