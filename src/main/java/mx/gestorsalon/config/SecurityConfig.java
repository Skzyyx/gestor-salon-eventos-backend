package mx.gestorsalon.config;

import lombok.RequiredArgsConstructor;
import mx.gestorsalon.security.CustomUserDetailsService;
import mx.gestorsalon.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.Customizer;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults()) // Habilitamos CORS usando el Bean de abajo
                .csrf(AbstractHttpConfigurer::disable) // Deshabilitamos protección contra sitios cruzados
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Configuramos a Spring Security para que, si un usuario no está autenticado
                // (no tiene token o el token es inválido),
                // devuelva explícitamente un error HTTP 401 (Unauthorized) en lugar del 403
                // (Forbidden) por defecto.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                new org.springframework.security.web.authentication.HttpStatusEntryPoint(
                                        org.springframework.http.HttpStatus.UNAUTHORIZED)))
                // --- CONFIGURACIÓN DE RUTAS ---
                .authorizeHttpRequests(auth -> auth
                        // Ruta pública SOLO para el Login: es el único endpoint que tiene
                        // sentido llamar sin token (aún no lo tienes). El resto de /auth
                        // (ej. /auth/me) cae en anyRequest().authenticated() de abajo, porque
                        // necesita un usuario autenticado. Usar "/auth/**" dejaría /auth/me
                        // público y, al llegar sin Principal, reventaría con NPE -> 500.
                        .requestMatchers("/auth/login").permitAll()
                        // Endpoint interno de errores de Spring. DEBE ser público: cuando un
                        // controlador lanza una excepción (400, 409, 500...), Spring reenvía a
                        // /error para construir la respuesta. Como es un reenvío de tipo ERROR,
                        // el filtro JWT no corre y (al ser STATELESS) no hay sesión guardada, así
                        // que sin esto el error se enmascararía como un 403 genérico.
                        .requestMatchers("/error").permitAll()
                        // Zona de plataforma: SOLO el SUPERADMIN puede provisionar negocios/usuarios.
                        .requestMatchers("/admin/**").hasRole("SUPERADMIN")
                        // Cualquier otra ruta solo requiere estar autenticado (la usan los ADMIN).
                        .anyRequest().authenticated())

                // Conectamos nuestro Buscador de MySQL
                .authenticationProvider(authenticationProvider())

                // Ponemos a nuestro policía JWT justo en la puerta de entrada principal
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Le decimos a Spring quién es el buscador oficial y qué encriptador de
    // contraseñas usamos
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // Herramienta que usaremos más adelante en el Login para comparar correos y
    // contraseñas
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Configuración de CORS para permitir que React (puerto 5173) hable con Spring
    // Boot (puerto 8080)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173")); // URL de Vite
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Aplica a toda la API
        return source;
    }
}
