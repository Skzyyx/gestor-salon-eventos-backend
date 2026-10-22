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
import org.junit.jupiter.params.provider.CsvSource;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
    @DisplayName("obtenerRolesPorNegocio devuelve únicamente los roles que el repositorio tiene para ese negocio")
    void obtenerRolesPorNegocio_negocioConRoles_devuelveSusRoles() {
        // Arrange
        RolNegocio gerente = new RolNegocio();
        gerente.setNombre("Gerente");
        RolNegocio recepcionista = new RolNegocio();
        recepcionista.setNombre("Recepcionista");
        when(rolNegocioRepository.findByNegocioId(1L)).thenReturn(List.of(gerente, recepcionista));

        // Act
        List<RolNegocio> roles = rolNegocioService.obtenerRolesPorNegocio(1L);

        // Assert
        assertEquals(List.of("Gerente", "Recepcionista"), roles.stream().map(RolNegocio::getNombre).toList());
        verify(rolNegocioRepository).findByNegocioId(1L);
        verifyNoMoreInteractions(rolNegocioRepository);
    }

    @Test
    @DisplayName("obtenerRolesPorNegocio devuelve una lista vacía para un negocio sin roles")
    void obtenerRolesPorNegocio_negocioSinRoles_devuelveListaVacia() {
        // Arrange
        when(rolNegocioRepository.findByNegocioId(2L)).thenReturn(List.of());

        // Act
        List<RolNegocio> roles = rolNegocioService.obtenerRolesPorNegocio(2L);

        // Assert
        assertTrue(roles.isEmpty());
    }

    @Test
    @DisplayName("obtenerPorIdYNegocio devuelve el rol cuando pertenece al negocio pedido")
    void obtenerPorIdYNegocio_rolDelNegocio_devuelveElRol() {
        // Arrange
        RolNegocio recepcionista = new RolNegocio();
        recepcionista.setId(3L);
        recepcionista.setNombre("Recepcionista");
        when(rolNegocioRepository.findByIdAndNegocioId(3L, 1L)).thenReturn(Optional.of(recepcionista));

        // Act
        RolNegocio rol = rolNegocioService.obtenerPorIdYNegocio(3L, 1L);

        // Assert
        assertSame(recepcionista, rol);
        verify(rolNegocioRepository).findByIdAndNegocioId(3L, 1L);
        verifyNoMoreInteractions(rolNegocioRepository);
    }

    @ParameterizedTest(name = "rol {0} en negocio {1}")
    @DisplayName("obtenerPorIdYNegocio responde 'Rol no encontrado en este negocio' si el rol no existe o es de otro negocio")
    @CsvSource({"3, 2", "999, 1"})
    void obtenerPorIdYNegocio_rolInexistenteOAjeno_lanzaConMensaje(Long rolId, Long negocioId) {
        // Arrange
        when(rolNegocioRepository.findByIdAndNegocioId(rolId, negocioId)).thenReturn(Optional.empty());

        // Act
        RecursoNoEncontradoException error = assertThrows(RecursoNoEncontradoException.class,
                () -> rolNegocioService.obtenerPorIdYNegocio(rolId, negocioId));

        // Assert
        assertEquals("Rol no encontrado en este negocio", error.getMessage());
        verify(rolNegocioRepository, never()).findById(any());
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