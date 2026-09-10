package mx.gestorsalon.security;

import mx.gestorsalon.model.Usuario;
import mx.gestorsalon.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * EL BUSCADOR: Spring Security usará esta clase cada vez que alguien
 * intente iniciar sesión, para ir a buscar si el correo existe en MySQL.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Buscamos en nuestra tabla de MySQL por correo
        Usuario usuario = usuarioRepository.findByCorreoConPermisos(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        // 2. Si lo encontramos, lo envolvemos en el Traductor y se lo damos a Spring
        // Security
        return new CustomUserDetails(usuario);
    }
}
