package mx.gestorsalon;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import mx.gestorsalon.util.SecurityUtils;

@SpringBootTest
@ActiveProfiles("test")
class GestorSalonApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
        // SecurityUtils es un @Component con inyección por constructor. Si alguien le
        // agrega un constructor sin argumentos que lance excepción (historial,
        // caf524b),
        // el contexto no levanta y este test lo detecta.
        assertThat(context.getBean(SecurityUtils.class)).isNotNull();
    }
}
