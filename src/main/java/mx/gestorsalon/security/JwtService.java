package mx.gestorsalon.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    // -------------------------------------------------------------------------
    // @Value: Le dice a Spring "Ve a application.yml y tráeme este valor".
    // Esto asegura que la clave secreta nunca esté escrita directamente en el
    // código.
    // -------------------------------------------------------------------------
    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpiration;

    // =========================================================================
    // 1. LEER DATOS DEL TOKEN (El cadenero leyendo el gafete)
    // =========================================================================

    // "Subject" es el sujeto dueño del token. Por convención, siempre guardamos el
    // Correo ahí.
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Leemos un dato personalizado ("tenantId") que nosotros le metimos a mano al
    // crearlo.
    public Long extractTenantId(String token) {
        return extractClaim(token, claims -> claims.get("tenantId", Long.class));
    }

    // =========================================================================
    // 2. FABRICAR EL TOKEN (Imprimir y sellar el gafete)
    // =========================================================================

    public String generateToken(String username, Long tenantId) {
        // "Claims" es simplemente la palabra elegante para "Los datos impresos en el
        // token"
        Map<String, Object> extraClaims = new HashMap<>();
        // El SUPERADMIN no pertenece a ningún negocio (tenantId == null).
        // Solo metemos el claim cuando SÍ hay negocio, para no guardar un valor nulo en
        // el token.
        if (tenantId != null) {
            extraClaims.put("tenantId", tenantId); // Guardamos la magia del Multi-tenant aquí
        }

        return Jwts.builder()
                .claims(extraClaims) // Metemos el tenantId
                .subject(username) // Metemos el correo
                .issuedAt(new Date(System.currentTimeMillis())) // Fecha de hoy
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration)) // Fecha de expiración (hoy + 24 hrs)
                .signWith(getSignInKey(), Jwts.SIG.HS256) // FIRMA CRIPTOGRÁFICA (El sello inviolable)
                .compact(); // Convertimos todo a un String largo (ej. "eyJhbGciOiJIUzI1...")
    }

    // =========================================================================
    // 3. REVISAR SI EL TOKEN ES VÁLIDO O FALSO
    // =========================================================================

    public boolean isTokenValid(String token, String username) {
        final String extractedUsername = extractUsername(token);
        // Es válido si el correo coincide Y el token no ha expirado
        return (extractedUsername.equals(username)) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        // Verifica si la fecha de caducidad ya pasó
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // =========================================================================
    // MÉTODOS DE APOYO INTERNO (Magia criptográfica)
    // =========================================================================

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Este es el método más crítico de seguridad.
    // Intenta abrir el token usando nuestra "Clave Secreta".
    // Si alguien modificó el token (aunque sea una letra), verifyWith() tirará un
    // error gigante.
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Transforma nuestra clave de String a un objeto criptográfico SecretKey
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
