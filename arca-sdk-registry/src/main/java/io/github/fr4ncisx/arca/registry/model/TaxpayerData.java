package io.github.fr4ncisx.arca.registry.model;

import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import org.jspecify.annotations.Nullable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a taxpayer's registry profile.
 * <p>
 * This record aggregates the taxpayer's identity metadata and lists of registered addresses,
 * activities, taxes, and regimes.
 *
 * @param cuit the unique taxpayer identifier
 * @param name the taxpayer's first name (if a natural person)
 * @param lastName the taxpayer's last name (if a natural person)
 * @param businessName the business or trade name (if a corporate entity)
 * @param keyState the state of the taxpayer key (e.g., active, inactive)
 * @param personType the classification of the taxpayer person (e.g., FISICA, JURIDICA)
 * @param gender the taxpayer's gender (if a natural person)
 * @param socialContractDate the timestamp of the social contract declaration
 * @param deathDate the timestamp of death (if a natural person, nullable)
 * @param enrollmentDate the timestamp of tax registration
 * @param birthDate the timestamp of birth (if a natural person)
 * @param juridicalForm the legal form or organization structure
 * @param activityInscriptionLocation the location of the activity inscription
 * @param activityInscriptionProvince the province of the activity inscription
 * @param closingMonth the month of fiscal year closing
 * @param addresses the list of registered physical addresses
 * @param taxes the list of inscribed taxes
 * @param activities the list of economic activities
 * @param regimes the list of inscribed special regimes
 * @author fr4ncisx
 * @since 0.5.0-M1
 */
public record TaxpayerData(
    long cuit,
    @Nullable String name,
    @Nullable String lastName,
    @Nullable String businessName,
    @Nullable String keyState,
    @Nullable String personType,
    @Nullable String gender,
    @Nullable LocalDateTime socialContractDate,
    @Nullable LocalDateTime deathDate,
    @Nullable LocalDateTime enrollmentDate,
    @Nullable LocalDateTime birthDate,
    @Nullable String juridicalForm,
    @Nullable String activityInscriptionLocation,
    @Nullable String activityInscriptionProvince,
    @Nullable Integer closingMonth,
    List<TaxpayerAddress> addresses,
    List<TaxpayerTax> taxes,
    List<TaxpayerActivity> activities,
    List<TaxpayerRegime> regimes
) {

    /**
     * Creates an immutable snapshot of the supplied collections.
     *
     * @param cuit the unique taxpayer identifier
     * @param name the taxpayer's first name (if a natural person)
     * @param lastName the taxpayer's last name (if a natural person)
     * @param businessName the business or trade name (if a corporate entity)
     * @param keyState the state of the taxpayer key (e.g., active, inactive)
     * @param personType the classification of the taxpayer person (e.g., FISICA, JURIDICA)
     * @param gender the taxpayer's gender (if a natural person)
     * @param socialContractDate the timestamp of the social contract declaration
     * @param deathDate the timestamp of death (if a natural person, nullable)
     * @param enrollmentDate the timestamp of tax registration
     * @param birthDate the timestamp of birth (if a natural person)
     * @param juridicalForm the legal form or organization structure
     * @param activityInscriptionLocation the location of the activity inscription
     * @param activityInscriptionProvince the province of the activity inscription
     * @param closingMonth the month of fiscal year closing
     * @param addresses the list of registered physical addresses
     * @param taxes the list of inscribed taxes
     * @param activities the list of economic activities
     * @param regimes the list of inscribed special regimes
     * @throws ArcaValidationException if a required collection or one of its elements is null.
     */
    public TaxpayerData {
        addresses = snapshot(addresses, "addresses");
        taxes = snapshot(taxes, "taxes");
        activities = snapshot(activities, "activities");
        regimes = snapshot(regimes, "regimes");
    }

    private static <T> List<T> snapshot(List<T> values, String fieldName) {
        if (values == null) {
            throw new ArcaValidationException(fieldName + " must not be null");
        }
        try {
            return List.copyOf(values);
        } catch (NullPointerException exception) {
            throw new ArcaValidationException(fieldName + " must not contain null entries", exception);
        }
    }
}
