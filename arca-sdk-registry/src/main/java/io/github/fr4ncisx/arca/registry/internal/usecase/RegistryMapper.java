package io.github.fr4ncisx.arca.registry.internal.usecase;

import io.github.fr4ncisx.arca.registry.internal.generated.*;
import io.github.fr4ncisx.arca.registry.model.*;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Technical mapper to convert JAX-WS generated XML types to public domain records.
 *
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
final class RegistryMapper {

    private RegistryMapper() {
        // Prevent instantiation
    }

    /**
     * Maps a JAX-WS PersonaReturn payload to public TaxpayerData.
     *
     * @param personaReturn the technical persona return structure
     * @return the mapped domain TaxpayerData
     */
    static TaxpayerData toDomain(PersonaReturn personaReturn) {
        Persona p = personaReturn.getPersona();
        long cuit = p.getIdPersona() != null ? p.getIdPersona() : 0L;

        return new TaxpayerData(
            cuit,
            p.getNombre(),
            p.getApellido(),
            p.getRazonSocial(),
            p.getEstadoClave(),
            p.getTipoPersona(),
            p.getSexo(),
            toLocalDateTime(p.getFechaContratoSocial()),
            toLocalDateTime(p.getFechaFallecimiento()),
            toLocalDateTime(p.getFechaInscripcion()),
            toLocalDateTime(p.getFechaNacimiento()),
            p.getFormaJuridica(),
            p.getLocalidadInscripcion(),
            p.getProvinciaInscripcion(),
            p.getMesCierre(),
            toAddresses(p.getDomicilio()),
            toTaxes(p.getImpuesto()),
            toActivities(p.getActividad()),
            toRegimes(p.getRegimen())
        );
    }

    private static List<TaxpayerAddress> toAddresses(List<Domicilio> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return list.stream()
            .map(d -> new TaxpayerAddress(
                d.getCodPostal(),
                d.getDireccion(),
                d.getDescripcionProvincia(),
                d.getIdProvincia(),
                d.getLocalidad(),
                d.getTipoDomicilio(),
                d.getDatoAdicional(),
                d.getTipoDatoAdicional(),
                d.getOrden()
            ))
            .collect(Collectors.toList());
    }

    private static List<TaxpayerTax> toTaxes(List<Impuesto> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return list.stream()
            .map(i -> new TaxpayerTax(
                i.getIdImpuesto(),
                i.getDescripcionImpuesto(),
                i.getEstado(),
                toLocalDateTime(i.getFfInscripcion()),
                i.getPeriodo()
            ))
            .collect(Collectors.toList());
    }

    private static List<TaxpayerActivity> toActivities(List<Actividad> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return list.stream()
            .map(a -> new TaxpayerActivity(
                a.getIdActividad(),
                a.getDescripcionActividad(),
                a.getNomenclador(),
                a.getOrden(),
                a.getPeriodo()
            ))
            .collect(Collectors.toList());
    }

    private static List<TaxpayerRegime> toRegimes(List<Regimen> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return list.stream()
            .map(r -> new TaxpayerRegime(
                r.getIdImpuesto(),
                r.getIdRegimen(),
                r.getDescripcionRegimen(),
                r.getEstado(),
                r.getPeriodo(),
                r.getTipoRegimen()
            ))
            .collect(Collectors.toList());
    }

    private static LocalDateTime toLocalDateTime(XMLGregorianCalendar xmlCal) {
        if (xmlCal == null) {
            return null;
        }
        return xmlCal.toGregorianCalendar().toZonedDateTime().toLocalDateTime();
    }
}
