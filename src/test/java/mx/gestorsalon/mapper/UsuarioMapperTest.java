package mx.gestorsalon.mapper;

import mx.gestorsalon.dto.admin.UsuarioResponse;
import mx.gestorsalon.model.Usuario;
import mx.gestorsalon.model.enums.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioMapperTest {

    private UsuarioMapper usuarioMapper;

    @BeforeEach
    void setUp() {
        usuarioMapper = new UsuarioMapper();
    }

    @Test
    @DisplayName("toResponse debe retornar null cuando usuario es null")
    void toResponse_conUsuarioNull_retornaNull() {
        UsuarioResponse response = usuarioMapper.toResponse(null);

        assertNull(response);
    }

    @Test
    @DisplayName("toResponse debe mapear datos de usuario y su rol correctamente")
    void toResponse_conUsuarioValido_mapeaCorrectamente() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Admin General");
        usuario.setCorreo("admin@gestorsalon.mx");
        usuario.setRol(Rol.ADMIN);

        UsuarioResponse response = usuarioMapper.toResponse(usuario);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Admin General", response.getNombre());
        assertEquals("admin@gestorsalon.mx", response.getCorreo());
        assertEquals("ADMIN", response.getRol());
    }

    @Test
    @DisplayName("toResponse debe manejar rol null defensivamente")
    void toResponse_conRolNull_noLanzaExcepcion() {
        Usuario usuario = new Usuario();
        usuario.setId(2L);
        usuario.setNombre("Usuario Sin Rol");
        usuario.setCorreo("sinrol@test.com");
        usuario.setRol(null);

        UsuarioResponse response = usuarioMapper.toResponse(usuario);

        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals("Usuario Sin Rol", response.getNombre());
        assertEquals("sinrol@test.com", response.getCorreo());
        assertNull(response.getRol());
    }
}
