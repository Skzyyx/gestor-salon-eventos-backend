package mx.gestorsalon.service;

import mx.gestorsalon.dto.admin.CrearNegocioRequest;
import mx.gestorsalon.dto.negocio.NegocioResponse;
import mx.gestorsalon.exception.RecursoDuplicadoException;
import mx.gestorsalon.mapper.NegocioMapper;
import mx.gestorsalon.mapper.UsuarioMapper;
import mx.gestorsalon.model.Negocio;
import mx.gestorsalon.model.Usuario;
import mx.gestorsalon.model.enums.Rol;
import mx.gestorsalon.repository.NegocioRepository;
import mx.gestorsalon.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private NegocioRepository negocioRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolNegocioService rolNegocioService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private NegocioMapper negocioMapper;
    @Mock
    private UsuarioMapper usuarioMapper;

    @InjectMocks
    private AdminService adminService;

    private static CrearNegocioRequest solicitud(String slug) {
        CrearNegocioRequest request = new CrearNegocioRequest();
        request.setNombre("Salón Las Palmas");
        request.setSlug(slug);
        request.setAdminNombre("Ana López");
        request.setAdminCorreo("ana@laspalmas.mx");
        request.setAdminPassword("Secreta123!");
        return request;
    }

    /** Ni el slug ni el correo existen; los save devuelven lo que reciben. */
    private void repositoriosSinDuplicados() {
        when(negocioRepository.findBySlug(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.findByCorreo(anyString())).thenReturn(Optional.empty());
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("hash-bcrypt");
    }

    @Test
    @DisplayName("crearNegocioConAdmin convierte un slug con acentos y espacios en una dirección limpia")
    void crearNegocioConAdmin_slugConAcentosYEspacios_guardaSlugLimpio() {
        // Arrange
        repositoriosSinDuplicados();
        CrearNegocioRequest request = solicitud("  Salón Las Palmas  ");

        // Act
        adminService.crearNegocioConAdmin(request);

        // Assert
        ArgumentCaptor<Negocio> guardado = ArgumentCaptor.forClass(Negocio.class);
        verify(negocioRepository).save(guardado.capture());
        assertEquals("salon-las-palmas", guardado.getValue().getSlug());
    }

    @Test
    @DisplayName("crearNegocioConAdmin rechaza un slug sin letras ni números y no guarda nada")
    void crearNegocioConAdmin_slugSinLetrasNiNumeros_lanzaExcepcion() {
        // Arrange
        CrearNegocioRequest request = solicitud("¡¡!!");

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> adminService.crearNegocioConAdmin(request));
        verifyNoInteractions(negocioRepository, usuarioRepository, rolNegocioService);
    }

    @Test
    @DisplayName("crearNegocioConAdmin rechaza un slug que ya usa otro negocio")
    void crearNegocioConAdmin_slugExistente_lanzaRecursoDuplicado() {
        // Arrange
        when(negocioRepository.findBySlug("salon-las-palmas")).thenReturn(Optional.of(new Negocio()));
        CrearNegocioRequest request = solicitud("salon-las-palmas");

        // Act + Assert
        assertThrows(RecursoDuplicadoException.class, () -> adminService.crearNegocioConAdmin(request));
        verify(negocioRepository, never()).save(any());
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    @DisplayName("crearNegocioConAdmin rechaza un correo de administrador ya registrado")
    void crearNegocioConAdmin_correoExistente_lanzaRecursoDuplicado() {
        // Arrange
        when(negocioRepository.findBySlug("salon-las-palmas")).thenReturn(Optional.empty());
        when(usuarioRepository.findByCorreo("ana@laspalmas.mx")).thenReturn(Optional.of(new Usuario()));
        CrearNegocioRequest request = solicitud("salon-las-palmas");

        // Act + Assert
        assertThrows(RecursoDuplicadoException.class, () -> adminService.crearNegocioConAdmin(request));
        verify(negocioRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearNegocioConAdmin guarda al admin con contraseña cifrada, rol ADMIN y su negocio")
    void crearNegocioConAdmin_datosValidos_creaAdminDelNegocio() {
        // Arrange
        repositoriosSinDuplicados();
        CrearNegocioRequest request = solicitud("salon-las-palmas");

        // Act
        adminService.crearNegocioConAdmin(request);

        // Assert
        ArgumentCaptor<Usuario> admin = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(admin.capture());
        assertEquals("hash-bcrypt", admin.getValue().getPasswordHash());
        assertEquals(Rol.ADMIN, admin.getValue().getRol());
        assertEquals("salon-las-palmas", admin.getValue().getNegocio().getSlug());
        verify(rolNegocioService).sembrarPlantillasSiNoExisten(admin.getValue().getNegocio());
    }

    @ParameterizedTest(name = "{0} negocios en la plataforma -> {0} respuestas en el mismo orden")
    @ValueSource(ints = {0, 1, 3})
    @DisplayName("listarNegocios mapea cada negocio del repositorio, en orden y sin omitir ninguno")
    void listarNegocios_negociosRegistrados_devuelveUnaRespuestaPorNegocio(int cantidad) {
        // Arrange
        List<Negocio> negocios = new ArrayList<>();
        List<NegocioResponse> esperadas = new ArrayList<>();
        for (long id = 1; id <= cantidad; id++) {
            Negocio negocio = new Negocio();
            negocio.setId(id);
            negocio.setSlug("salon-" + id);
            NegocioResponse respuesta = NegocioResponse.builder().id(id).slug("salon-" + id).build();
            negocios.add(negocio);
            esperadas.add(respuesta);
            when(negocioMapper.toResponse(negocio)).thenReturn(respuesta);
        }
        when(negocioRepository.findAll()).thenReturn(negocios);

        // Act
        List<NegocioResponse> resultado = adminService.listarNegocios();

        // Assert
        assertEquals(esperadas, resultado);
        verifyNoInteractions(usuarioRepository, rolNegocioService, passwordEncoder);
    }
}