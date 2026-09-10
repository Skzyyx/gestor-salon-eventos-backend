package mx.gestorsalon.service;

import lombok.RequiredArgsConstructor;
import mx.gestorsalon.dto.equipo.ActualizarTrabajadorRequest;
import mx.gestorsalon.dto.equipo.TrabajadorRequest;
import mx.gestorsalon.dto.equipo.TrabajadorResponse;
import mx.gestorsalon.dto.equipo.RolNegocioResponse;
import mx.gestorsalon.exception.RecursoDuplicadoException;
import mx.gestorsalon.exception.RecursoNoEncontradoException;
import mx.gestorsalon.exception.AccesoDenegadoException;
import mx.gestorsalon.model.Rol;
import mx.gestorsalon.model.RolNegocio;
import mx.gestorsalon.model.Usuario;
import mx.gestorsalon.repository.UsuarioRepository;
import mx.gestorsalon.security.CustomUserDetails;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EquipoService {

    private final UsuarioRepository usuarioRepository;
    private final RolNegocioService rolNegocioService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<TrabajadorResponse> listarEquipo(Long negocioId) {
        return usuarioRepository.findByNegocioIdAndRol(negocioId, Rol.EMPLEADO).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TrabajadorResponse registrarTrabajador(CustomUserDetails actor, TrabajadorRequest request) {
        Long negocioId = actor.getUsuario().getNegocio().getId();

        // 1. Obtener y validar el rol objetivo
        RolNegocio rolObjetivo = rolNegocioService.obtenerPorIdYNegocio(request.getRolNegocioId(), negocioId);

        // 2. Invariante: Techo de privilegio
        rolNegocioService.validarTechoDePrivilegio(actor, rolObjetivo.getPermisos());

        // 3. Normalizar correo
        String correoNormalizado = request.getCorreo().trim().toLowerCase();

        // 4. Crear usuario
        Usuario trabajador = new Usuario();
        trabajador.setNombre(request.getNombre());
        trabajador.setCorreo(correoNormalizado);
        trabajador.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        trabajador.setRol(Rol.EMPLEADO);
        trabajador.setRolNegocio(rolObjetivo);
        trabajador.setNegocio(actor.getUsuario().getNegocio());
        trabajador.setActivo(true);

        try {
            Usuario guardado = usuarioRepository.save(trabajador);
            return mapToResponse(guardado);
        } catch (DataIntegrityViolationException e) {
            // Correo opaco: no revelamos si existe en otro negocio o en el mismo, mensaje genérico
            throw new RecursoDuplicadoException("El correo ya se encuentra registrado o no es válido.");
        }
    }

    @Transactional
    public TrabajadorResponse actualizarTrabajador(Long id, CustomUserDetails actor, ActualizarTrabajadorRequest request) {
        Long negocioId = actor.getUsuario().getNegocio().getId();

        // 1. Invariante: Aislamiento por tenant y Sin auto-escalada
        Usuario trabajador = obtenerTrabajador(id, negocioId);
        validarNoEsMismoUsuario(id, actor.getUsuario().getId(), "No puedes editar tu propio rol.");

        // 2. Obtener y validar el rol objetivo
        RolNegocio rolObjetivo = rolNegocioService.obtenerPorIdYNegocio(request.getRolNegocioId(), negocioId);

        // 3. Invariante: Techo de privilegio
        rolNegocioService.validarTechoDePrivilegio(actor, rolObjetivo.getPermisos());

        // 4. Actualizar
        trabajador.setNombre(request.getNombre());
        trabajador.setRolNegocio(rolObjetivo);

        return mapToResponse(usuarioRepository.save(trabajador));
    }

    @Transactional
    public TrabajadorResponse cambiarEstado(Long id, CustomUserDetails actor, boolean activo) {
        Long negocioId = actor.getUsuario().getNegocio().getId();

        Usuario trabajador = obtenerTrabajador(id, negocioId);
        validarNoEsMismoUsuario(id, actor.getUsuario().getId(), "No puedes cambiar tu propio estado.");

        trabajador.setActivo(activo);
        return mapToResponse(usuarioRepository.save(trabajador));
    }

    private Usuario obtenerTrabajador(Long id, Long negocioId) {
        Usuario usuario = usuarioRepository.findByIdAndNegocioId(id, negocioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Trabajador no encontrado"));

        if (usuario.getRol() != Rol.EMPLEADO) {
            throw new IllegalArgumentException("Solo se puede gestionar usuarios con rol EMPLEADO a través de este módulo");
        }
        return usuario;
    }

    private void validarNoEsMismoUsuario(Long idObjetivo, Long idActor, String mensaje) {
        if (idObjetivo.equals(idActor)) {
            throw new AccesoDenegadoException(mensaje);
        }
    }

    private TrabajadorResponse mapToResponse(Usuario usuario) {
        RolNegocioResponse rolResponse = null;
        if (usuario.getRolNegocio() != null) {
            rolResponse = RolNegocioResponse.builder()
                    .id(usuario.getRolNegocio().getId())
                    .nombre(usuario.getRolNegocio().getNombre())
                    .descripcion(usuario.getRolNegocio().getDescripcion())
                    .esSistema(usuario.getRolNegocio().getEsSistema())
                    .build();
        }

        return TrabajadorResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .correo(usuario.getCorreo())
                .activo(usuario.getActivo())
                .rol(rolResponse)
                .build();
    }
}
