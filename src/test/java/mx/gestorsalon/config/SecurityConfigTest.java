package mx.gestorsalon.config;

import mx.gestorsalon.security.CustomUserDetailsService;
import mx.gestorsalon.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Preflight de CORS tal como lo manda el navegador: OPTIONS con Origin y
 * Access-Control-Request-Method. DefaultCorsProcessor es lo que usa Spring para
 * aceptarlo o rechazarlo con 403.
 */
class SecurityConfigTest {

    // Lo que llega con APP_CORS_ALLOWED_ORIGINS=http://localhost:5173,https://salones.example.com
    private static final List<String> ORIGENES = List.of("http://localhost:5173", "https://salones.example.com");

    private static CorsConfiguration corsCon(List<String> origenes) {
        SecurityConfig config = new SecurityConfig(mock(JwtAuthenticationFilter.class),
                mock(CustomUserDetailsService.class));
        ReflectionTestUtils.setField(config, "allowedOrigins", origenes);
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) config.corsConfigurationSource();
        return source.getCorsConfigurations().get("/**");
    }

    @ParameterizedTest(name = "{1} desde {0} -> permitido: {2}")
    @CsvSource({
            "http://localhost:5173, GET, true",
            "http://localhost:5173, PATCH, true",
            "https://salones.example.com, DELETE, true",
            "https://otro-sitio.example.com, GET, false",
            "http://localhost:5173, TRACE, false"
    })
    @DisplayName("El preflight acepta solo los orígenes configurados y los métodos que usa el frontend")
    void preflight_origenYMetodo_aceptaSoloLoConfigurado(String origen, String metodo, boolean permitido)
            throws IOException {
        // Arrange
        CorsConfiguration cors = corsCon(ORIGENES);
        MockHttpServletRequest preflight = new MockHttpServletRequest("OPTIONS", "/equipo/7/estado");
        preflight.addHeader(HttpHeaders.ORIGIN, origen);
        preflight.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, metodo);
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        // Act
        boolean aceptado = new DefaultCorsProcessor().processRequest(cors, preflight, respuesta);

        // Assert
        assertEquals(permitido, aceptado);
        assertEquals(permitido ? 200 : 403, respuesta.getStatus());
    }
}