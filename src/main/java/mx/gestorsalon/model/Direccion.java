package mx.gestorsalon.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

/**
 * Objeto de valor reutilizable para direcciones.
 * Se embebe (@Embedded) en las entidades que necesitan dirección (Negocio y, más
 * adelante, Cliente). Sus columnas viven en la tabla de cada entidad, así que NO
 * hay tabla ni JOIN extra: es la forma idiomática en JPA de modelar un value object.
 */
@Data
@Embeddable
public class Direccion {

    @Column(length = 150)
    private String calle;

    @Column(length = 20)
    private String numero;

    @Column(length = 100)
    private String colonia;

    @Column(length = 100)
    private String ciudad;

    @Column(length = 100)
    private String estado;

    @Column(name = "codigo_postal", length = 10)
    private String codigoPostal;
}
