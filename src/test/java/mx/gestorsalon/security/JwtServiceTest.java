package mx.gestorsalon.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    // Base64 de un texto de 48 bytes (384 bits). JwtService lo decodifica con Decoders.BASE64 y firma con
    // HS256, que exige una clave de al menos 256 bits (32 bytes); con menos, jjwt lanza WeakKeyException.
    private static final String SECRETO = "Y2xhdmUtZGUtcHJ1ZWJhcy1kZWwtZ2VzdG9yLWRlLXNhbG9uZXMtMjAyNi1vayEh";
    // Otra clave igual de larga: la de un servidor que no es el nuestro.
    private static final String OTRO_SECRETO = "b3RyYS1jbGF2ZS1kZS1vdHJvLXNlcnZpZG9yLXF1ZS1uby1lcy1sYS1udWVzdHJh";

    private static final long UNA_HORA_MS = 3_600_000L;

    private static final String ANA = "ana.lopez@laspalmas.mx";
    private static final String LUIS = "luis.ramirez@quintareal.mx";
    private static final Long NEGOCIO_LAS_PALMAS = 7L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = jwtServiceCon(SECRETO, UNA_HORA_MS);
    }

    /** Arma el servicio sin levantar Spring: los dos campos que Spring llena con @Value se ponen a mano. */
    private static JwtService jwtServiceCon(String secreto, long expiracionMs) {
        JwtService servicio = new JwtService();
        ReflectionTestUtils.setField(servicio, "secretKey", secreto);
        ReflectionTestUtils.setField(servicio, "jwtExpiration", expiracionMs);
        return servicio;
    }

    @ParameterizedTest(name = "{0} con negocio {1}")
    @DisplayName("generateToken guarda el correo y el negocio, y se leen iguales del token")
    @CsvSource({
            "ana.lopez@laspalmas.mx,     7",
            "luis.ramirez@quintareal.mx, 3000000000"
    })
    void generateToken_correoYNegocio_seLeenIgualesDelToken(String correo, Long negocioId) {
        // Arrange: cada fila es un caso (correo, id del negocio). El 7 viaja en el JSON como entero chico
        // y 3000000000 no cabe en un int: extractTenantId debe devolver Long en los dos casos.

        // Act
        String token = jwtService.generateToken(correo, negocioId);

        // Assert
        assertEquals(correo, jwtService.extractUsername(token));
        assertEquals(negocioId, jwtService.extractTenantId(token));
    }

    @Test
    @DisplayName("generateToken no agrega tenantId cuando el usuario es SUPERADMIN y no tiene negocio")
    void generateToken_superadminSinNegocio_tenantIdEsNull() {
        // Arrange
        String correoSuperadmin = "super@gestorsalon.com";

        // Act
        String token = jwtService.generateToken(correoSuperadmin, null);

        // Assert
        assertEquals(correoSuperadmin, jwtService.extractUsername(token));
        assertNull(jwtService.extractTenantId(token));
    }

    @Test
    @DisplayName("isTokenValid acepta un token vigente para el mismo usuario que lo recibió")
    void isTokenValid_mismoUsuario_devuelveTrue() {
        // Arrange
        String token = jwtService.generateToken(ANA, NEGOCIO_LAS_PALMAS);

        // Act
        boolean valido = jwtService.isTokenValid(token, ANA);

        // Assert
        assertTrue(valido);
    }

    @Test
    @DisplayName("isTokenValid rechaza con false el token de Ana si se revisa para otro usuario")
    void isTokenValid_otroUsuario_devuelveFalse() {
        // Arrange
        String tokenDeAna = jwtService.generateToken(ANA, NEGOCIO_LAS_PALMAS);

        // Act
        boolean valido = jwtService.isTokenValid(tokenDeAna, LUIS);

        // Assert
        assertFalse(valido);
    }

    @Test
    @DisplayName("isTokenValid no devuelve false con un token vencido: jjwt lanza ExpiredJwtException")
    void isTokenValid_tokenVencido_lanzaExpiredJwtException() {
        // Arrange: una expiración negativa hace que el token nazca vencido hace un minuto, sin Thread.sleep
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -60_000L);
        String tokenVencido = jwtService.generateToken(ANA, NEGOCIO_LAS_PALMAS);

        // Act
        ExpiredJwtException error = assertThrows(ExpiredJwtException.class,
                () -> jwtService.isTokenValid(tokenVencido, ANA));

        // Assert: la firma era buena (jjwt la revisa antes que la fecha), por eso la excepción trae los datos
        assertEquals(ANA, error.getClaims().getSubject());
    }

    @Test
    @DisplayName("isTokenValid rechaza con SignatureException un token firmado con otra clave")
    void isTokenValid_firmadoConOtraClave_lanzaSignatureException() {
        // Arrange: mismo correo y negocio, pero sellado por un servidor con otro secreto
        JwtService otroServidor = jwtServiceCon(OTRO_SECRETO, UNA_HORA_MS);
        String tokenAjeno = otroServidor.generateToken(ANA, NEGOCIO_LAS_PALMAS);

        // Act
        SignatureException error = assertThrows(SignatureException.class,
                () -> jwtService.isTokenValid(tokenAjeno, ANA));

        // Assert: es exactamente io.jsonwebtoken.security.SignatureException, no otra JwtException
        assertEquals(SignatureException.class, error.getClass());
    }

    @Test
    @DisplayName("extractUsername rechaza con SignatureException un token al que le cambiaron el contenido")
    void extractUsername_contenidoAlterado_lanzaSignatureException() {
        // Arrange: encabezado y firma del token de Ana, pero con el contenido (payload) del token de Luis
        String[] partesAna = jwtService.generateToken(ANA, NEGOCIO_LAS_PALMAS).split("\\.");
        String[] partesLuis = jwtService.generateToken(LUIS, NEGOCIO_LAS_PALMAS).split("\\.");
        String tokenAlterado = partesAna[0] + "." + partesLuis[1] + "." + partesAna[2];

        // Act
        SignatureException error = assertThrows(SignatureException.class,
                () -> jwtService.extractUsername(tokenAlterado));

        // Assert: es exactamente io.jsonwebtoken.security.SignatureException, no otra JwtException
        assertEquals(SignatureException.class, error.getClass());
    }

    @Test
    @DisplayName("extractUsername rechaza con MalformedJwtException un texto que no es un JWT")
    void extractUsername_textoQueNoEsJwt_lanzaMalformedJwtException() {
        // Arrange: lo que llegaría con el encabezado "Authorization: Bearer token-inventado"
        String tokenInventado = "token-inventado";

        // Act
        MalformedJwtException error = assertThrows(MalformedJwtException.class,
                () -> jwtService.extractUsername(tokenInventado));

        // Assert: es exactamente MalformedJwtException, no otra JwtException
        assertEquals(MalformedJwtException.class, error.getClass());
    }
}