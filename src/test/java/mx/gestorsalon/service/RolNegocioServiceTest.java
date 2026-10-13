package mx.gestorsalon.service;

import mx.gestorsalon.exception.AccesoDenegadoException;
import mx.gestorsalon.exception.RecursoNoEncontradoException;
import mx.gestorsalon.model.Negocio;
import mx.gestorsalon.model.RolNegocio;
import mx.gestorsalon.model.Usuario;
import mx.gestorsalon.model.enums.Permiso;
import mx.gestorsalon.model.enums.Rol;
import mx.gestorsalon.repository.RolNegocioRepository;
import mx.gestorsalon.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolNegocioServiceTest {

    @Mock
    private RolNegocioRepository rolNegocioRepository;

    @InjectMocks
    private RolNegocioService rolNegocioService;

    /** Un EMPLEADO cuyas authorities PERM_* salen de su rol de negocio. */
    private static CustomUserDetails empleadoCon(Permiso... permisos) {
        Set<Permiso> suyos = EnumSet.noneOf(Permiso.class);
        Collections.addAll(suyos, permisos);
        RolNegocio rol = new RolNegocio();
        rol.setPermisos(suyos);
        Usuario usuario = new Usuario();
        usuario.setRol(Rol.EMPLEADO);
        usuario.setRolNegocio(rol);
        return new CustomUserDetails(usuario);
    }

    private static CustomUserDetails usuarioConRol(Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        return new CustomUserDetails(usuario);
    }

    private static Negocio negocioConId(Long id) {
        Negocio n = new Negocio();
        n.setId(id);
        return n;
    }

    @Test
    @DisplayName("un empleado puede conceder exactamente los permisos que tiene, menos, o ninguno")
    void validarTecho_permisosIgualesOMenores_noLanzaExcepcion() {
        // Arrange
        CustomUserDetails actor = empleadoCon(Permiso.CLIENTE_VER, Permiso.CLIENTE_CREAR);
        Set<Permiso> iguales = EnumSet.of(Permiso.CLIENTE_VER, Permiso.CLIENTE_CREAR);
        Set<Permiso> menos = EnumSet.of(Permiso.CLIENTE_VER);
        Set<Permiso> ninguno = EnumSet.noneOf(Permiso.class);

        // Act + Assert
        assertAll(
                () -> assertDoesNotThrow(() -> rolNegocioService.validarTechoDePrivilegio(actor, iguales)),
                () -> assertDoesNotThrow(() -> rolNegocioService.validarTechoDePrivilegio(actor, menos)),
                () -> assertDoesNotThrow(() -> rolNegocioService.validarTechoDePrivilegio(actor, ninguno)));
    }

    @ParameterizedTest(name = "conceder {0} se rechaza")
    @DisplayName("un empleado no puede conceder ningún permiso que él no tenga")
    @EnumSource(value = Permiso.class, mode = EnumSource.Mode.EXCLUDE, names = {"CLIENTE_VER", "CLIENTE_CREAR"})
    void validarTecho_permisoQueElActorNoTiene_lanzaAccesoDenegado(Permiso permisoAjeno) {
        // Arrange
        CustomUserDetails actor = empleadoCon(Permiso.CLIENTE_VER, Permiso.CLIENTE_CREAR);
        Set<Permiso> objetivo = EnumSet.of(Permiso.CLIENTE_VER, permisoAjeno);

        // Act
        AccesoDenegadoException error = assertThrows(AccesoDenegadoException.class,
                () -> rolNegocioService.validarTechoDePrivilegio(actor, objetivo));

        // Assert
        assertTrue(error.getMessage().endsWith(permisoAjeno.name()));
    }

    @Test
    @DisplayName("un ADMIN puede conceder cualquier permiso del catálogo")
    void validarTecho_actorAdmin_puedeConcederTodo() {
        // Arrange
        CustomUserDetails admin = usuarioConRol(Rol.ADMIN);
        Set<Permiso> todos = EnumSet.allOf(Permiso.class);

        // Act + Assert
        assertDoesNotThrow(() -> rolNegocioService.validarTechoDePrivilegio(admin, todos));
    }

    @Test
    @DisplayName("un SUPERADMIN no tiene permisos de negocio, así que no puede concederlos")
    void validarTecho_actorSuperadmin_lanzaAccesoDenegado() {
        // Arrange
        CustomUserDetails superadmin = usuarioConRol(Rol.SUPERADMIN);
        Set<Permiso> objetivo = EnumSet.of(Permiso.SERVICIO_VER);

        // Act + Assert
        assertThrows(AccesoDenegadoException.class,
                () -> rolNegocioService.validarTechoDePrivilegio(superadmin, objetivo));
    }

    @Test
    @DisplayName("obtenerPorIdYNegocio no encuentra el rol de otro negocio")
    void obtenerPorIdYNegocio_rolDeOtroNegocio_lanzaRecursoNoEncontrado() {
        // Arrange
        when(rolNegocioRepository.findByIdAndNegocioId(3L, 1L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(RecursoNoEncontradoException.class, () -> rolNegocioService.obtenerPorIdYNegocio(3L, 1L));
    }

    @Test
    @DisplayName("sembrarPlantillas crea Gerente, Coordinador y Recepcionista como roles de sistema")
    void sembrarPlantillas_negocioNuevo_creaTresRolesDeSistema() {
        // Arrange
        Negocio negocio = negocioConId(1L);
        when(rolNegocioRepository.findByNegocioIdAndNombre(eq(1L), anyString())).thenReturn(Optional.empty());

        // Act
        rolNegocioService.sembrarPlantillasSiNoExisten(negocio);

        // Assert
        ArgumentCaptor<RolNegocio> roles = ArgumentCaptor.forClass(RolNegocio.class);
        verify(rolNegocioRepository, times(3)).save(roles.capture());
        assertEquals(List.of("Gerente", "Coordinador de eventos", "Recepcionista"),
                roles.getAllValues().stream().map(RolNegocio::getNombre).toList());
        assertTrue(roles.getAllValues().stream().allMatch(RolNegocio::getEsSistema));
    }

    @Test
    @DisplayName("sembrarPlantillas no duplica roles que ya existen")
    void sembrarPlantillas_rolesExistentes_noGuardaNada() {
        // Arrange
        Negocio negocio = negocioConId(1L);
        when(rolNegocioRepository.findByNegocioIdAndNombre(eq(1L), anyString()))
                .thenReturn(Optional.of(new RolNegocio()));

        // Act
        rolNegocioService.sembrarPlantillasSiNoExisten(negocio);

        // Assert
        verify(rolNegocioRepository, never()).save(any());
    }
}