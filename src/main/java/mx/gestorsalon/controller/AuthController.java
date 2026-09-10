package mx.gestorsalon.controller;

import lombok.RequiredArgsConstructor;
import mx.gestorsalon.dto.auth.AuthRequest;
import mx.gestorsalon.dto.auth.AuthResponse;
import mx.gestorsalon.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/auth") // El application.yml ya le pone /api/v1 automáticamente
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // Cuando el frontend mande un POST a /api/v1/auth/login, caerá aquí.
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // Devuelve los datos reales del usuario basándose en su token actual
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(Principal principal) {
        // principal.getName() nos da el correo extraído del token por Spring Security
        return ResponseEntity.ok(authService.getMe(principal.getName()));
    }
}
