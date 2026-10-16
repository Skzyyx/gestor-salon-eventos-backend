package mx.gestorsalon.service;

import mx.gestorsalon.dto.negocio.NegocioUpdateRequest;
import mx.gestorsalon.mapper.NegocioMapper;
import mx.gestorsalon.model.Negocio;
import mx.gestorsalon.repository.NegocioRepository;
import mx.gestorsalon.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
}
