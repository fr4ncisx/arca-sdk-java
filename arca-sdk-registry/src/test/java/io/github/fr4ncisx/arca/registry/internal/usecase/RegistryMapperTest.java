package io.github.fr4ncisx.arca.registry.internal.usecase;

import io.github.fr4ncisx.arca.registry.internal.adapter.RegistryMapper;
import io.github.fr4ncisx.arca.registry.internal.generated.*;
import io.github.fr4ncisx.arca.registry.model.TaxpayerData;
import org.junit.jupiter.api.Test;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RegistryMapper}.
 *
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
class RegistryMapperTest {

    @Test
    @SuppressWarnings("null")
    void mapsCorrectlyWithEmptyCollections() {
        PersonaReturn input = new PersonaReturn();
        Persona p = new Persona();
        p.setIdPersona(123L);
        p.setNombre("A");
        p.setApellido("B");
        p.setRazonSocial("A B");
        p.setEstadoClave("ACTIVE");
        p.setTipoPersona("FISICA");
        input.setPersona(p);

        TaxpayerData output = RegistryMapper.toDomain(input);

        assertThat(output).isNotNull();
        assertThat(output.cuit()).isEqualTo(123L);
        assertThat(output.name()).isEqualTo("A");
        assertThat(output.lastName()).isEqualTo("B");
        assertThat(output.businessName()).isEqualTo("A B");
        assertThat(output.keyState()).isEqualTo("ACTIVE");
        assertThat(output.personType()).isEqualTo("FISICA");
        assertThat(output.addresses()).isEmpty();
        assertThat(output.taxes()).isEmpty();
        assertThat(output.activities()).isEmpty();
        assertThat(output.regimes()).isEmpty();
    }

    @Test
    @SuppressWarnings("null")
    void mapsAddressesTaxesActivitiesAndDates() throws Exception {
        PersonaReturn input = new PersonaReturn();
        Persona p = new Persona();
        p.setIdPersona(123L);
        p.setRazonSocial("Test");

        XMLGregorianCalendar cal = DatatypeFactory.newInstance().newXMLGregorianCalendar("2026-05-15T12:00:00");
        p.setFechaInscripcion(cal);

        Domicilio dom = new Domicilio();
        dom.setCodPostal("1000");
        dom.setDireccion("San Martin 123");
        dom.setDescripcionProvincia("CABA");
        dom.setIdProvincia(0);
        dom.setLocalidad("CABA");
        dom.setTipoDomicilio("FISCAL");
        p.getDomicilio().add(dom);

        Impuesto imp = new Impuesto();
        imp.setIdImpuesto(30);
        imp.setDescripcionImpuesto("IVA");
        imp.setEstado("ACTIVO");
        p.getImpuesto().add(imp);

        Actividad act = new Actividad();
        act.setIdActividad(461011L);
        act.setDescripcionActividad("Venta al por mayor");
        p.getActividad().add(act);

        Regimen reg = new Regimen();
        reg.setIdImpuesto(20);
        reg.setIdRegimen(100);
        reg.setDescripcionRegimen("Regimen General");
        reg.setEstado("ACTIVO");
        p.getRegimen().add(reg);

        input.setPersona(p);

        TaxpayerData output = RegistryMapper.toDomain(input);

        assertThat(output.cuit()).isEqualTo(123L);
        assertThat(output.enrollmentDate()).isEqualTo("2026-05-15T12:00:00");
        
        assertThat(output.addresses()).hasSize(1);
        assertThat(output.addresses().get(0).postalCode()).isEqualTo("1000");
        assertThat(output.addresses().get(0).address()).isEqualTo("San Martin 123");
        assertThat(output.addresses().get(0).addressType()).isEqualTo("FISCAL");

        assertThat(output.taxes()).hasSize(1);
        assertThat(output.taxes().get(0).taxId()).isEqualTo(30);
        assertThat(output.taxes().get(0).taxDescription()).isEqualTo("IVA");

        assertThat(output.activities()).hasSize(1);
        assertThat(output.activities().get(0).activityId()).isEqualTo(461011L);

        assertThat(output.regimes()).hasSize(1);
        assertThat(output.regimes().get(0).regimeId()).isEqualTo(100);
        assertThat(output.regimes().get(0).regimeDescription()).isEqualTo("Regimen General");
    }
}
