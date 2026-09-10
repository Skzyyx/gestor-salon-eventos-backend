package mx.gestorsalon.service;

import lombok.RequiredArgsConstructor;
import mx.gestorsalon.dto.admin.CrearNegocioRequest;
import mx.gestorsalon.dto.admin.NegocioAdminResponse;
import mx.gestorsalon.dto.negocio.NegocioResponse;
import mx.gestorsalon.exception.RecursoDuplicadoException;
import mx.gestorsalon.mapper.NegocioMapper;
import mx.gestorsalon.mapper.UsuarioMapper;
import mx.gestorsalon.model.Negocio;
import mx.gestorsalon.model.Usuario;
import mx.gestorsalon.model.enums.Rol;
import mx.gestorsalon.repository.NegocioRepository;
import mx.gestorsalon.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final NegocioRepository negocioRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolNegocioService rolNegocioService;
    private final PasswordEncoder passwordEncoder; // el bean BCrypt definido en tu SecurityConfig

    /**
     * Da de alta un negocio NUEVO junto con su primer usuario ADMIN.
     * Es la operación central de la "provisión por plataforma" (CU-01).
     *
     * @Transactional: si algo falla a la mitad (ej. el correo ya existía,
     *                 pero el negocio ya se había guardado), se revierte TODO. Así
     *                 nunca
     *                 queda un negocio huérfano sin administrador.
     */
    @Transactional
    public NegocioAdminResponse crearNegocioConAdmin(CrearNegocioRequest request) {

        // 1. Normalizamos el slug igual que en tu NegocioService (minúsculas, sin
        // símbolos raros)
        String slug = request.getSlug().toLowerCase().replaceAll("[^a-z0-9]+", "-");

        // 2. Validamos que no se repitan slug ni correo -> 409 CONFLICT si ya existen.
        // Reutilizamos tus métodos de repositorio findBySlug / findByCorreo.
        if (negocioRepository.findBySlug(slug).isPresent()) {
            throw new RecursoDuplicadoException("Ya existe un negocio con ese slug");
        }
        if (usuarioRepository.findByCorreo(request.getAdminCorreo()).isPresent()) {
            throw new RecursoDuplicadoException("Ya existe un usuario con ese correo");
        }

        // 3. Creamos y guardamos el NEGOCIO
        Negocio negocio = new Negocio();
        negocio.setNombre(request.getNombre());
        negocio.setSlug(slug);
        negocio.setActivo(true);
        Negocio negocioGuardado = negocioRepository.save(negocio);

        // Sembrar los roles plantilla base del RBAC (CU-27) para este nuevo negocio
        rolNegocioService.sembrarPlantillasSiNoExisten(negocioGuardado);

        // 4. Creamos y guardamos su ADMIN inicial
        Usuario admin = new Usuario();
        admin.setNombre(request.getAdminNombre());
        admin.setCorreo(request.getAdminCorreo());
        // REGLA DE ORO: nunca guardamos la contraseña en texto plano -> BCrypt
        admin.setPasswordHash(passwordEncoder.encode(request.getAdminPassword()));
        admin.setRol(Rol.ADMIN); // dueño de SU negocio (no de la plataforma)
        admin.setNegocio(negocioGuardado); // lo amarramos al negocio recién creado
        admin.setActivo(true);
        Usuario adminGuardado = usuarioRepository.save(admin);

        // 5. Devolvemos un resumen SEGURO (sin password) usando nuestros DTOs + tu
        // Mapper
        return NegocioAdminResponse.builder()
                .negocio(NegocioMapper.toResponse(negocioGuardado))
                .admin(UsuarioMapper.toResponse(adminGuardado))
                .build();
    }

    /**
     * Lista TODOS los negocios de la plataforma (vista exclusiva del SUPERADMIN).
     */
    @Transactional(readOnly = true)
    public List<NegocioResponse> listarNegocios() {
        return negocioRepository.findAll().stream()
                .map(NegocioMapper::toResponse)
                .toList();
    }
}
