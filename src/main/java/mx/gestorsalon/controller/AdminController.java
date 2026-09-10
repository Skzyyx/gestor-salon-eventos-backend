package mx.gestorsalon.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.gestorsalon.dto.admin.CrearNegocioRequest;
import mx.gestorsalon.dto.admin.NegocioAdminResponse;
import mx.gestorsalon.dto.negocio.NegocioResponse;
import mx.gestorsalon.service.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de PLATAFORMA, exclusivos del SUPERADMIN.
 *
 * No necesita validar el rol aquí: SecurityConfig ya protege toda la ruta
 * "/admin/**" con hasRole("SUPERADMIN"). Si un ADMIN normal intenta entrar,
 * Spring Security lo corta ANTES de llegar a este controlador (403).
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * Da de alta un negocio nuevo junto con su usuario administrador inicial.
     * 
     * @Valid dispara las validaciones del DTO (campos obligatorios, email, etc.):
     *        si fallan, responde 400 automáticamente sin entrar al servicio.
     *        Devuelve 201 CREATED porque estamos creando un recurso nuevo.
     */
    @PostMapping("/negocios")
    public ResponseEntity<NegocioAdminResponse> crearNegocio(@Valid @RequestBody CrearNegocioRequest request) {
        return new ResponseEntity<>(adminService.crearNegocioConAdmin(request), HttpStatus.CREATED);
    }

    /**
     * Lista todos los negocios registrados en la plataforma.
     */
    @GetMapping("/negocios")
    public ResponseEntity<List<NegocioResponse>> listarNegocios() {
        return ResponseEntity.ok(adminService.listarNegocios());
    }
}
