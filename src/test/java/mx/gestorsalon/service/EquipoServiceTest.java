package mx.gestorsalon.service;

import mx.gestorsalon.dto.equipo.ActualizarTrabajadorRequest;
import mx.gestorsalon.dto.equipo.TrabajadorRequest;
import mx.gestorsalon.dto.equipo.TrabajadorResponse;
import mx.gestorsalon.exception.AccesoDenegadoException;
import mx.gestorsalon.exception.RecursoDuplicadoException;
import mx.gestorsalon.exception.RecursoNoEncontradoException;
import mx.gestorsalon.model.Negocio;
import mx.gestorsalon.model.RolNegocio;
import mx.gestorsalon.model.Usuario;
import mx.gestorsalon.model.enums.Permiso;
import mx.gestorsalon.model.enums.Rol;
import mx.gestorsalon.repository.UsuarioRepository;
import mx.gestorsalon.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquipoServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolNegocioService rolNegocioService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EquipoService equipoService;

    private Negocio negocio;
    private CustomUserDetails actor;

    @BeforeEach
    void setUp() {
        negocio = negocioConId(1L);
        actor = gerenteDe(negocio);
    }

    private static Negocio negocioConId(Long id) {
        Negocio n = new Negocio();
        n.setId(id);
        return n;
    }

    private static RolNegocio rol(Long id, String nombre, Permiso... permisos) {
        Set<Permiso> suyos = EnumSet.noneOf(Permiso.class);
        Collections.addAll(suyos, permisos);
        RolNegocio rol = new RolNegocio();
        rol.setId(id);
        rol.setNombre(nombre);
        rol.setPermisos(suyos);
        return rol;
    }

    /** Carmen (id 10) es EMPLEADO con rol Gerente: gestiona el equipo, pero no los roles. */
    private static CustomUserDetails gerenteDe(Negocio negocio) {
        Usuario carmen = new Usuario();
        carmen.setId(10L);
        carmen.setNombre("Carmen Ruiz");
        carmen.setCorreo("carmen.ruiz@laspalmas.mx");
        carmen.setRol(Rol.EMPLEADO);
        carmen.setRolNegocio(rol(3L, "Gerente",
                Permiso.EQUIPO_VER, Permiso.EQUIPO_GESTIONAR, Permiso.ROL_VER, Permiso.CLIENTE_VER, Permiso.TURNO_VER));
        carmen.setNegocio(negocio);
        return new CustomUserDetails(carmen);
    }

    private Usuario empleado(Long id, String nombre, RolNegocio rolNegocio) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre(nombre);
        usuario.setRol(Rol.EMPLEADO);
        usuario.setRolNegocio(rolNegocio);
        usuario.setNegocio(negocio);
        return usuario;
    }

    private static TrabajadorRequest solicitudAlta(String correo, Long rolNegocioId) {
        TrabajadorRequest request = new TrabajadorRequest();
        request.setNombre("Luis Pérez");
        request.setCorreo(correo);
        request.setPassword("Secreta123!");
        request.setRolNegocioId(rolNegocioId);
        return request;
    }

    private static ActualizarTrabajadorRequest solicitudCambio(String nombre, Long rolNegocioId) {
        ActualizarTrabajadorRequest request = new ActualizarTrabajadorRequest();
        request.setNombre(nombre);
        request.setRolNegocioId(rolNegocioId);
        return request;
    }

    @Test
    @DisplayName("listarEquipo consulta solo a los EMPLEADO del negocio y muestra sin rol a quien no tiene uno")
    void listarEquipo_negocioConEmpleados_devuelveSoloSusEmpleados() {
        // Arrange
        Usuario luis = empleado(20L, "Luis Pérez", rol(5L, "Recepcionista", Permiso.CLIENTE_VER, Permiso.TURNO_VER));
        Usuario marta = empleado(21L, "Marta Soto", null);
        when(usuarioRepository.findByNegocioIdAndRol(1L, Rol.EMPLEADO)).thenReturn(List.of(luis, marta));

        // Act
        List<TrabajadorResponse> equipo = equipoService.listarEquipo(1L);

        // Assert
        assertEquals(List.of(20L, 21L), equipo.stream().map(TrabajadorResponse::getId).toList());
        assertEquals("Recepcionista", equipo.get(0).getRol().getNombre());
        assertNull(equipo.get(1).getRol());
        verifyNoInteractions(rolNegocioService, passwordEncoder);
    }

    @Test
    @DisplayName("registrarTrabajador crea un EMPLEADO activo en el negocio del actor, con el rol pedido, el correo normalizado y la contraseña cifrada")
    void registrarTrabajador_datosValidos_creaEmpleadoActivoDelNegocioDelActor() {
        // Arrange
        RolNegocio recepcionista = rol(5L, "Recepcionista", Permiso.CLIENTE_VER, Permiso.TURNO_VER);
        TrabajadorRequest request = solicitudAlta("  Luis.Perez@LasPalmas.MX ", 5L);
        when(rolNegocioService.obtenerPorIdYNegocio(5L, 1L)).thenReturn(recepcionista);
        when(passwordEncoder.encode("Secreta123!")).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        TrabajadorResponse respuesta = equipoService.registrarTrabajador(actor, request);

        // Assert
        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        Usuario luis = guardado.getValue();
        assertEquals(Rol.EMPLEADO, luis.getRol());
        assertSame(negocio, luis.getNegocio());
        assertSame(recepcionista, luis.getRolNegocio());
        assertEquals("luis.perez@laspalmas.mx", luis.getCorreo());
        assertEquals("hash-bcrypt", luis.getPasswordHash());
        assertTrue(luis.getActivo());
        assertEquals("Recepcionista", respuesta.getRol().getNombre());
        verify(rolNegocioService).validarTechoDePrivilegio(actor, recepcionista.getPermisos());
    }

    @Test
    @DisplayName("registrarTrabajador responde con un mensaje genérico cuando la base rechaza el correo")
    void registrarTrabajador_correoYaRegistrado_lanzaRecursoDuplicado() {
        // Arrange
        TrabajadorRequest request = solicitudAlta("luis.perez@laspalmas.mx", 5L);
        when(rolNegocioService.obtenerPorIdYNegocio(5L, 1L))
                .thenReturn(rol(5L, "Recepcionista", Permiso.CLIENTE_VER, Permiso.TURNO_VER));
        when(usuarioRepository.save(any(Usuario.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry 'luis.perez@laspalmas.mx'"));

        // Act
        RecursoDuplicadoException error = assertThrows(RecursoDuplicadoException.class,
                () -> equipoService.registrarTrabajador(actor, request));

        // Assert
        assertEquals("El correo ya se encuentra registrado o no es válido.", error.getMessage());
    }

    @Test
    @DisplayName("registrarTrabajador rechaza un rol con permisos que el actor no tiene y no crea la cuenta")
    void registrarTrabajador_rolSobreElTechoDelActor_lanzaAccesoDenegado() {
        // Arrange
        RolNegocio encargadoDeRoles = rol(6L, "Encargado de roles", Permiso.ROL_VER, Permiso.ROL_GESTIONAR);
        TrabajadorRequest request = solicitudAlta("luis.perez@laspalmas.mx", 6L);
        when(rolNegocioService.obtenerPorIdYNegocio(6L, 1L)).thenReturn(encargadoDeRoles);
        doThrow(new AccesoDenegadoException("No puedes conceder permisos que no tienes: ROL_GESTIONAR"))
                .when(rolNegocioService).validarTechoDePrivilegio(actor, encargadoDeRoles.getPermisos());

        // Act
        AccesoDenegadoException error = assertThrows(AccesoDenegadoException.class,
                () -> equipoService.registrarTrabajador(actor, request));

        // Assert
        assertEquals("No puedes conceder permisos que no tienes: ROL_GESTIONAR", error.getMessage());
        verifyNoInteractions(passwordEncoder, usuarioRepository);
    }

    @Test
    @DisplayName("registrarTrabajador busca el rol solo dentro del negocio del actor: el de otro negocio no existe")
    void registrarTrabajador_rolDeOtroNegocio_lanzaRecursoNoEncontrado() {
        // Arrange
        TrabajadorRequest request = solicitudAlta("luis.perez@laspalmas.mx", 9L);
        when(rolNegocioService.obtenerPorIdYNegocio(9L, 1L))
                .thenThrow(new RecursoNoEncontradoException("Rol no encontrado en este negocio"));

        // Act
        RecursoNoEncontradoException error = assertThrows(RecursoNoEncontradoException.class,
                () -> equipoService.registrarTrabajador(actor, request));

        // Assert
        assertEquals("Rol no encontrado en este negocio", error.getMessage());
        verify(rolNegocioService, never()).validarTechoDePrivilegio(any(), any());
        verifyNoInteractions(passwordEncoder, usuarioRepository);
    }

    @Test
    @DisplayName("actualizarTrabajador no encuentra a un trabajador de otro negocio y no toca su rol")
    void actualizarTrabajador_trabajadorDeOtroNegocio_lanzaRecursoNoEncontrado() {
        // Arrange
        ActualizarTrabajadorRequest request = solicitudCambio("Luis Pérez", 5L);
        when(usuarioRepository.findByIdAndNegocioId(30L, 1L)).thenReturn(Optional.empty());

        // Act
        RecursoNoEncontradoException error = assertThrows(RecursoNoEncontradoException.class,
                () -> equipoService.actualizarTrabajador(30L, actor, request));

        // Assert
        assertEquals("Trabajador no encontrado", error.getMessage());
        verifyNoInteractions(rolNegocioService);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarTrabajador impide que el actor cambie su propio rol (sin auto-escalada)")
    void actualizarTrabajador_propioUsuario_lanzaAccesoDenegado() {
        // Arrange
        ActualizarTrabajadorRequest request = solicitudCambio("Carmen Ruiz", 6L);
        when(usuarioRepository.findByIdAndNegocioId(10L, 1L)).thenReturn(Optional.of(actor.getUsuario()));

        // Act
        AccesoDenegadoException error = assertThrows(AccesoDenegadoException.class,
                () -> equipoService.actualizarTrabajador(10L, actor, request));

        // Assert
        assertEquals("No puedes editar tu propio rol.", error.getMessage());
        verifyNoInteractions(rolNegocioService);
        verify(usuarioRepository, never()).save(any());
    }

    @ParameterizedTest(name = "activo = {0}")
    @DisplayName("cambiarEstado da de baja o reactiva a un empleado del propio negocio y lo guarda")
    @ValueSource(booleans = {false, true})
    void cambiarEstado_empleadoDelNegocio_guardaElEstadoPedido(boolean activo) {
        // Arrange
        Usuario luis = empleado(20L, "Luis Pérez", rol(5L, "Recepcionista", Permiso.CLIENTE_VER, Permiso.TURNO_VER));
        luis.setActivo(!activo);
        when(usuarioRepository.findByIdAndNegocioId(20L, 1L)).thenReturn(Optional.of(luis));
        when(usuarioRepository.save(luis)).thenReturn(luis);

        // Act
        TrabajadorResponse respuesta = equipoService.cambiarEstado(20L, actor, activo);

        // Assert
        assertEquals(activo, luis.getActivo());
        assertEquals(activo, respuesta.getActivo());
        verifyNoInteractions(rolNegocioService, passwordEncoder);
    }

    @Test
    @DisplayName("cambiarEstado no deja dar de baja al ADMIN (dueño) del negocio desde el módulo de equipo")
    void cambiarEstado_cuentaAdminDelNegocio_lanzaExcepcion() {
        // Arrange
        Usuario ana = new Usuario();
        ana.setId(2L);
        ana.setNombre("Ana López");
        ana.setRol(Rol.ADMIN);
        ana.setNegocio(negocio);
        when(usuarioRepository.findByIdAndNegocioId(2L, 1L)).thenReturn(Optional.of(ana));

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> equipoService.cambiarEstado(2L, actor, false));

        // Assert
        assertEquals("Solo se puede gestionar usuarios con rol EMPLEADO a través de este módulo", error.getMessage());
        assertTrue(ana.getActivo());
        verify(usuarioRepository, never()).save(any());
    }
}