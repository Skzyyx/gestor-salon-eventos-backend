package mx.gestorsalon.service;

import lombok.RequiredArgsConstructor;
import mx.gestorsalon.dto.auth.AuthRequest;
import mx.gestorsalon.dto.auth.AuthResponse;
import mx.gestorsalon.model.Usuario;
import mx.gestorsalon.repository.UsuarioRepository;
import mx.gestorsalon.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse login(AuthRequest request) {
        // 1. Spring Security hace el trabajo sucio por nosotros:
        // Va a la base de datos, busca el correo y comprueba si el password coincide
        try {
            // Spring valida correo + contraseña contra la BD.
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getCorreo(), request.getPassword()));
        } catch (AuthenticationException e) {
            // Si las credenciales son malas, traducimos el fallo a un 401 limpio.
            // Si dejáramos propagar la AuthenticationException tal cual, la capa de
            // seguridad respondería 403. Con ResponseStatusException(401) el error
            // viaja por /error (que ya es público) y sale con el código correcto.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos");
        }

        // 2. Si llegamos aquí, es porque la contraseña era correcta. Buscamos al
        // usuario real.
        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow(); // Ya sabemos que existe porque el authenticate lo acaba de validar

        // 3. Le fabricamos su Token V.I.P.
        // El SUPERADMIN no tiene negocio: usamos null en ese caso para no reventar con
        // NullPointerException.
        Long negocioId = (usuario.getNegocio() != null) ? usuario.getNegocio().getId() : null;
        String jwtToken = jwtService.generateToken(usuario.getCorreo(), negocioId);

        // 4. Se lo mandamos de regreso al Frontend en el formato del DTO
        return AuthResponse.builder()
                .token(jwtToken)
                .nombre(usuario.getNombre())
                // .name() convierte el enum Rol a su texto ("ADMIN" / "SUPERADMIN").
                // Así el frontend sigue recibiendo un String y no hay que tocar React.
                .rol(usuario.getRol().name())
                .build();
    }

    public AuthResponse getMe(String correo) {
        // Buscamos al usuario en la BD (el correo viene del SecurityContext, así que 100% existe)
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Long negocioId = (usuario.getNegocio() != null) ? usuario.getNegocio().getId() : null;
        String jwtToken = jwtService.generateToken(usuario.getCorreo(), negocioId);

        return AuthResponse.builder()
                .token(jwtToken)
                .nombre(usuario.getNombre())
                .rol(usuario.getRol().name())
                .build();
    }
}
