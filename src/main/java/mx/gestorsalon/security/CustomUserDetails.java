package mx.gestorsalon.security;

import mx.gestorsalon.model.Usuario;
import mx.gestorsalon.model.enums.Permiso;
import mx.gestorsalon.model.enums.Rol;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * EL TRADUCTOR: Envuelve a nuestro "Usuario" de MySQL para que
 * Spring Security lo entienda como un "UserDetails".
 */
public class CustomUserDetails implements UserDetails {

    private final Usuario usuario;

    public CustomUserDetails(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    @Override
    public String getUsername() {
        return usuario.getCorreo();
    }

    @Override
    public String getPassword() {
        return usuario.getPasswordHash();
    }

    /**
     * EL CORAZÓN DEL RBAC: traduce el usuario a las authorities que Spring evalúa.
     *
     * Devuelve SIEMPRE una authority de rol (ROLE_*) y, según el rol, las de
     * permiso (PERM_*). Los dos prefijos son convención de Spring para separar
     * "qué tipo de cuenta es" (ROLE_) de "qué puede hacer" (PERM_):
     *
     * - SUPERADMIN -> solo ROLE_SUPERADMIN. Opera la plataforma, no un negocio:
     * cero permisos de negocio (no debe tocar datos de tenants).
     * - ADMIN -> ROLE_ADMIN + TODOS los PERM_ del catálogo, derivados en
     * caliente de Permiso.values(). Es el comodín del dueño; NO se
     * persisten filas, así que un permiso nuevo lo gana gratis.
     * - EMPLEADO -> ROLE_EMPLEADO + un PERM_ por cada permiso de su rolNegocio.
     *
     * Esto se ejecuta en CADA request (el filtro JWT llama a loadUserByUsername),
     * así que los permisos son siempre FRESCOS desde la BD: revocar surte efecto al
     * siguiente request, sin re-login. El JWT nunca lleva permisos.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        Rol rol = usuario.getRol();

        // 1) La authority de ROL (eje de cuenta) siempre va.
        authorities.add(new SimpleGrantedAuthority("ROLE_" + rol.name()));

        // 2) Las authorities de PERMISO dependen del rol.
        switch (rol) {
            case SUPERADMIN:
                // Sin permisos de negocio a propósito.
                break;
            case ADMIN:
                // Comodín: todo el catálogo, derivado dinámicamente.
                for (Permiso p : Permiso.values()) {
                    authorities.add(new SimpleGrantedAuthority("PERM_" + p.name()));
                }
                break;
            case EMPLEADO:
                // Solo lo que conceda su rolNegocio (puede ser null si quedó sin rol).
                if (usuario.getRolNegocio() != null) {
                    for (Permiso p : usuario.getRolNegocio().getPermisos()) {
                        authorities.add(new SimpleGrantedAuthority("PERM_" + p.name()));
                    }
                }
                break;
        }
        return authorities;
    }

    // --- Configuraciones de cuenta activa/bloqueada ---
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return usuario.getActivo();
    }
}
