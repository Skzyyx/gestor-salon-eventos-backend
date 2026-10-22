package mx.gestorsalon.service;

import mx.gestorsalon.dto.DireccionDTO;
import mx.gestorsalon.dto.negocio.NegocioResponse;
import mx.gestorsalon.dto.negocio.NegocioUpdateRequest;
import mx.gestorsalon.mapper.NegocioMapper;
import mx.gestorsalon.model.Direccion;
import mx.gestorsalon.model.Negocio;
import mx.gestorsalon.repository.NegocioRepository;
import mx.gestorsalon.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CU-04: el slug es la dirección pública del salón. Al crear el negocio (AdminService)
 * se normaliza con SlugUtils; al editar el perfil (NegocioService) debe valer la misma regla.
 */
@ExtendWith(MockitoExtension.class)
class NegocioServiceTest {

    @Mock
    private NegocioRepository negocioRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private NegocioMapper negocioMapper;

    @InjectMocks
    private NegocioService negocioService;

    private Negocio negocio;

    @BeforeEach
    void setUp() {
        negocio = new Negocio();
        negocio.setId(1L);
        negocio.setNombre("Salón Las Palmas");
        negocio.setSlug("salon-las-palmas");
    }

    private static NegocioUpdateRequest conSlug(String slug) {
        NegocioUpdateRequest request = new NegocioUpdateRequest();
        request.setSlug(slug);
        return request;
    }

    /** El negocio de la sesión es el 1, el slug nuevo está libre y save devuelve lo que recibe. */
    private void slugLibre() {
        when(securityUtils.getNegocioActual()).thenReturn(negocio);
        when(negocioRepository.existsBySlugAndIdNot(anyString(), eq(1L))).thenReturn(false);
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Negocio negocioGuardado() {
        ArgumentCaptor<Negocio> captor = ArgumentCaptor.forClass(Negocio.class);
        verify(negocioRepository).save(captor.capture());
        return captor.getValue();
    }

    @ParameterizedTest(name = "\"{0}\" se guarda como \"{1}\"")
    @DisplayName("updateMiNegocio guarda el slug con la misma regla que al crear el negocio")
    @CsvSource({
            "quinta-real,           quinta-real",
            "'  -quinta-real-  ',   quinta-real",
            "'--quinta--real--',    quinta-real"
    })
    void updateMiNegocio_slugConGuiones_guardaSlugNormalizado(String entrada, String esperado) {
        // Arrange
        slugLibre();
        NegocioUpdateRequest request = conSlug(entrada);

        // Act
        negocioService.updateMiNegocio(request);

        // Assert
        assertEquals(esperado, negocioGuardado().getSlug());
    }

    @Test
    @DisplayName("updateMiNegocio convierte acentos, eñes y mayúsculas igual que al crear el negocio")
    void updateMiNegocio_slugConAcentosYMayusculas_guardaSlugNormalizado() {
        // Arrange
        slugLibre();
        NegocioUpdateRequest request = conSlug("Quinta Real Peñasco");

        // Act
        negocioService.updateMiNegocio(request);

        // Assert
        assertEquals("quinta-real-penasco", negocioGuardado().getSlug());
    }

    @ParameterizedTest(name = "\"{0}\" se rechaza")
    @DisplayName("updateMiNegocio rechaza un slug que no tiene letras ni números")
    @ValueSource(strings = {"---", "¡¡!!"})
    void updateMiNegocio_slugSinLetrasNiNumeros_lanzaExcepcion(String entrada) {
        // Arrange
        when(securityUtils.getNegocioActual()).thenReturn(negocio);
        NegocioUpdateRequest request = conSlug(entrada);

        // Act
        assertThrows(IllegalArgumentException.class, () -> negocioService.updateMiNegocio(request));

        // Assert
        verify(negocioRepository, never()).save(any(Negocio.class));
    }

    @Test
    @DisplayName("updateMiNegocio rechaza un slug que ya usa otro negocio")
    void updateMiNegocio_slugDeOtroNegocio_lanzaExcepcion() {
        // Arrange
        when(securityUtils.getNegocioActual()).thenReturn(negocio);
        when(negocioRepository.existsBySlugAndIdNot("quinta-real", 1L)).thenReturn(true);
        NegocioUpdateRequest request = conSlug("quinta-real");

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> negocioService.updateMiNegocio(request));

        // Assert
        assertEquals("Esta dirección ya está en uso por otro negocio", error.getMessage());
        verify(negocioRepository, never()).save(any(Negocio.class));
    }

    @Test
    @DisplayName("updateMiNegocio sin slug conserva la dirección actual y no la vuelve a validar")
    void updateMiNegocio_sinSlug_conservaSlugActual() {
        // Arrange
        when(securityUtils.getNegocioActual()).thenReturn(negocio);
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(inv -> inv.getArgument(0));
        NegocioUpdateRequest request = new NegocioUpdateRequest();
        request.setNombre("Salón Las Palmas Norte");

        // Act
        negocioService.updateMiNegocio(request);

        // Assert
        Negocio guardado = negocioGuardado();
        assertEquals("salon-las-palmas", guardado.getSlug());
        assertEquals("Salón Las Palmas Norte", guardado.getNombre());
        verify(negocioRepository, never()).existsBySlugAndIdNot(anyString(), anyLong());
    }

    // -------------------- getMiNegocio --------------------

    @Test
    @DisplayName("getMiNegocio devuelve el negocio de la sesión ya mapeado a respuesta")
    void getMiNegocio_usuarioConNegocio_devuelveNegocioMapeado() {
        // Arrange
        NegocioResponse esperado = NegocioResponse.builder().id(1L).nombre("Salón Las Palmas").build();
        when(securityUtils.getNegocioActual()).thenReturn(negocio);
        when(negocioMapper.toResponse(negocio)).thenReturn(esperado);

        // Act
        NegocioResponse respuesta = negocioService.getMiNegocio();

        // Assert
        assertSame(esperado, respuesta);
        verify(negocioRepository, never()).save(any(Negocio.class));
    }

    // -------------------- updateMiNegocio: campos opcionales --------------------

    /** El negocio de la sesión con TODOS sus datos de contacto ya llenos. */
    private void conContactoCompleto() {
        negocio.setLogoUrl("https://cdn/logo-viejo.png");
        negocio.setTelefono("6870000000");
        negocio.setEmail("viejo@laspalmas.mx");
        negocio.setDescripcion("Descripción anterior");
    }

    private static Map<String, String> contactoDe(Negocio n) {
        return Map.of(
                "logoUrl", n.getLogoUrl(),
                "telefono", n.getTelefono(),
                "email", n.getEmail(),
                "descripcion", n.getDescripcion());
    }

    static Stream<Arguments> camposOpcionales() {
        return Stream.of(
                arguments("logoUrl", "https://cdn/logo-nuevo.png",
                        (BiConsumer<NegocioUpdateRequest, String>) NegocioUpdateRequest::setLogoUrl),
                arguments("telefono", "6871234567",
                        (BiConsumer<NegocioUpdateRequest, String>) NegocioUpdateRequest::setTelefono),
                arguments("email", "contacto@laspalmas.mx",
                        (BiConsumer<NegocioUpdateRequest, String>) NegocioUpdateRequest::setEmail),
                arguments("descripcion", "Salón para 300 personas con jardín",
                        (BiConsumer<NegocioUpdateRequest, String>) NegocioUpdateRequest::setDescripcion));
    }

    @ParameterizedTest(name = "solo {0} -> cambia {0} y el resto se conserva")
    @MethodSource("camposOpcionales")
    @DisplayName("updateMiNegocio solo sobrescribe los campos de contacto que vienen en la solicitud")
    void updateMiNegocio_unCampoOpcional_soloCambiaEseCampo(String campo, String valorNuevo,
            BiConsumer<NegocioUpdateRequest, String> asignar) {
        // Arrange
        conContactoCompleto();
        Map<String, String> antes = contactoDe(negocio);
        when(securityUtils.getNegocioActual()).thenReturn(negocio);
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(inv -> inv.getArgument(0));
        NegocioUpdateRequest request = new NegocioUpdateRequest();
        asignar.accept(request, valorNuevo);

        // Act
        negocioService.updateMiNegocio(request);

        // Assert
        Map<String, String> despues = contactoDe(negocioGuardado());
        for (Map.Entry<String, String> anterior : antes.entrySet()) {
            String esperado = anterior.getKey().equals(campo) ? valorNuevo : anterior.getValue();
            assertEquals(esperado, despues.get(anterior.getKey()), anterior.getKey());
        }
    }

    @ParameterizedTest(name = "nombre [{0}] -> se conserva el actual")
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("updateMiNegocio con nombre nulo o en blanco conserva el nombre actual")
    void updateMiNegocio_nombreNuloOEnBlanco_conservaNombreActual(String nombre) {
        // Arrange
        when(securityUtils.getNegocioActual()).thenReturn(negocio);
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(inv -> inv.getArgument(0));
        NegocioUpdateRequest request = new NegocioUpdateRequest();
        request.setNombre(nombre);

        // Act
        negocioService.updateMiNegocio(request);

        // Assert
        assertEquals("Salón Las Palmas", negocioGuardado().getNombre());
    }

    @ParameterizedTest(name = "slug [{0}] -> se conserva el actual")
    @ValueSource(strings = {"", "   "})
    @DisplayName("updateMiNegocio con slug en blanco conserva la dirección pública actual y no la valida")
    void updateMiNegocio_slugEnBlanco_conservaSlugActual(String slug) {
        // Arrange
        when(securityUtils.getNegocioActual()).thenReturn(negocio);
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(inv -> inv.getArgument(0));
        NegocioUpdateRequest request = conSlug(slug);

        // Act
        negocioService.updateMiNegocio(request);

        // Assert
        assertEquals("salon-las-palmas", negocioGuardado().getSlug());
        verify(negocioRepository, never()).existsBySlugAndIdNot(anyString(), anyLong());
    }

    // -------------------- updateMiNegocio: dirección --------------------

    private static DireccionDTO direccionNueva() {
        DireccionDTO dto = new DireccionDTO();
        dto.setCalle("Juárez");
        dto.setNumero("120");
        dto.setColonia("Centro");
        dto.setCiudad("Guasave");
        dto.setEstado("Sinaloa");
        dto.setCodigoPostal("81000");
        return dto;
    }

    private static void assertDireccionNueva(Direccion direccion) {
        assertNotNull(direccion);
        assertEquals("Juárez", direccion.getCalle());
        assertEquals("120", direccion.getNumero());
        assertEquals("Centro", direccion.getColonia());
        assertEquals("Guasave", direccion.getCiudad());
        assertEquals("Sinaloa", direccion.getEstado());
        assertEquals("81000", direccion.getCodigoPostal());
    }

    @Test
    @DisplayName("updateMiNegocio crea la dirección si el negocio todavía no tenía una")
    void updateMiNegocio_negocioSinDireccion_creaDireccionCompleta() {
        // Arrange
        when(securityUtils.getNegocioActual()).thenReturn(negocio);
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(inv -> inv.getArgument(0));
        NegocioUpdateRequest request = new NegocioUpdateRequest();
        request.setDireccion(direccionNueva());

        // Act
        negocioService.updateMiNegocio(request);

        // Assert
        assertDireccionNueva(negocioGuardado().getDireccion());
    }

    @Test
    @DisplayName("updateMiNegocio actualiza la MISMA dirección existente y la reemplaza completa")
    void updateMiNegocio_negocioConDireccion_actualizaLaExistente() {
        // Arrange
        Direccion existente = new Direccion();
        existente.setCalle("Calle vieja");
        existente.setCodigoPostal("80000");
        negocio.setDireccion(existente);
        when(securityUtils.getNegocioActual()).thenReturn(negocio);
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(inv -> inv.getArgument(0));
        NegocioUpdateRequest request = new NegocioUpdateRequest();
        request.setDireccion(direccionNueva());

        // Act
        negocioService.updateMiNegocio(request);

        // Assert
        Direccion guardada = negocioGuardado().getDireccion();
        assertSame(existente, guardada);
        assertDireccionNueva(guardada);
    }

    @Test
    @DisplayName("updateMiNegocio sin dirección en la solicitud deja intacta la dirección actual")
    void updateMiNegocio_sinDireccion_conservaDireccionActual() {
        // Arrange
        Direccion existente = new Direccion();
        existente.setCalle("Juárez");
        existente.setCiudad("Guasave");
        negocio.setDireccion(existente);
        when(securityUtils.getNegocioActual()).thenReturn(negocio);
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(inv -> inv.getArgument(0));
        NegocioUpdateRequest request = new NegocioUpdateRequest();
        request.setTelefono("6871234567");

        // Act
        negocioService.updateMiNegocio(request);

        // Assert
        Direccion guardada = negocioGuardado().getDireccion();
        assertSame(existente, guardada);
        assertEquals("Juárez", guardada.getCalle());
        assertEquals("Guasave", guardada.getCiudad());
    }
}