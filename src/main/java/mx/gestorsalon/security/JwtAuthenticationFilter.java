package mx.gestorsalon.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    // Aquí sí usamos el constructor explícito para la inyección de dependencias
    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 1. Extraemos el encabezado que se llama "Authorization"
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 2. Si no hay encabezado, o no empieza con "Bearer ", lo dejamos pasar.
        // (Tal vez es una ruta pública como el Login, así que no lo bloqueamos aquí,
        // SecurityConfig decidirá si lo bloquea más adelante).
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extraemos el token (cortamos la palabra "Bearer ")
        jwt = authHeader.substring(7).trim();

        try {
            // 4. Extraemos el correo leyendo el token
            userEmail = jwtService.extractUsername(jwt);

            // 5. Si encontramos un correo y el usuario AÚN NO está autenticado en este
            // hilo...
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Vamos a la base de datos a traer al usuario
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // 6. Revisamos si el sello criptográfico del token sigue siendo válido
                if (jwtService.isTokenValid(jwt, userDetails.getUsername())) {

                    // 7. Si es válido, creamos un "Pase Oficial" de Spring Security
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());
                    // Le agregamos detalles extra de la petición (como la IP)
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 8. Le entregamos el Pase Oficial al cadenero (SecurityContext)
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Si el token es inventado (ej: "token-inventado"), expiró o está corrupto,
            // la librería de JWT lanzará una excepción (MalformedJwtException, ExpiredJwtException).
            // La atrapamos aquí silenciosamente. Como no seteamos nada en el SecurityContext,
            // Spring Security tratará a este usuario como "No Autenticado" y lanzará un 401/403 limpio.
        }

        // 9. Dejamos que la petición siga su camino hacia los controladores
        filterChain.doFilter(request, response);
    }
}