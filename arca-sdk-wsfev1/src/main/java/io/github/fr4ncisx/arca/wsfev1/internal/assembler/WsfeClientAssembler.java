package io.github.fr4ncisx.arca.wsfev1.internal.assembler;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.exception.ArcaValidationException;
import io.github.fr4ncisx.arca.soap.internal.adapter.ArcaSoapClient;
import io.github.fr4ncisx.arca.soap.internal.config.SoapConfig;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsfev1.internal.client.DefaultWsfeClient;
import io.github.fr4ncisx.arca.wsfev1.internal.generated.*;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.batch.BatchProcessUseCase;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.cae.RequestCaeUseCase;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.caea.*;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.catalog.*;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.lastvoucher.GetLastVoucherUseCase;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.salespoint.GetSalesPointsUseCase;
import io.github.fr4ncisx.arca.wsfev1.internal.usecase.voucher.GetVoucherUseCase;
import io.github.fr4ncisx.arca.wsfev1.spi.WsfeClient;
import jakarta.xml.ws.BindingProvider;

/**
 * Assembler to construct and wire internal dependencies of the WSFEv1 client.
 *
 * @author fr4ncisx
 * @since 0.3.0-M1
 */
public final class WsfeClientAssembler {

    private WsfeClientAssembler() {
    }

    /**
     * Assembles a WSFEv1 client using official environment endpoints.
     *
     * @param config       the SDK configuration
     * @param authProvider the WSAA auth provider
     * @return the fully wired WsfeClient instance
     */
    public static WsfeClient assemble(ArcaConfig config, AuthProvider authProvider) {
        if (config == null) {
            throw new ArcaValidationException("config must not be null");
        }
        return assemble(config, authProvider, config.environment().getWsfev1Url().toString());
    }

    /**
     * Assembles a WSFEv1 client using a custom endpoint URL.
     *
     * @param config       the SDK configuration
     * @param authProvider the WSAA auth provider
     * @param endpointUrl  the target WSFEv1 SOAP endpoint URL
     * @return the fully wired WsfeClient instance
     */
    public static WsfeClient assemble(ArcaConfig config, AuthProvider authProvider, String endpointUrl) {
        if (config == null) {
            throw new ArcaValidationException("config must not be null");
        }
        if (authProvider == null) {
            throw new ArcaValidationException("authProvider must not be null");
        }
        if (endpointUrl == null || endpointUrl.trim().isEmpty()) {
            throw new ArcaValidationException("endpointUrl must not be null or blank");
        }

        SoapConfig soapConfig = SoapConfig.from(config);
        ServiceSoap port = new Service().getServiceSoap();

        BindingProvider bp = (BindingProvider) port;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);

        ArcaSoapPort<FECompUltimoAutorizado, FERecuperaLastCbteResponse> lastVoucherSoapPort =
                new ArcaSoapClient<>(bp, req -> port.feCompUltimoAutorizado(req.getAuth(), req.getPtoVta(), req.getCbteTipo()), soapConfig);

        ArcaSoapPort<FECAESolicitar, FECAEResponse> requestCaeSoapPort =
                new ArcaSoapClient<>(bp, req -> port.fecaeSolicitar(req.getAuth(), req.getFeCAEReq()), soapConfig);

        ArcaSoapPort<FECompConsultar, FECompConsultaResponse> getVoucherSoapPort =
                new ArcaSoapClient<>(bp, req -> port.feCompConsultar(req.getAuth(), req.getFeCompConsReq()), soapConfig);

        ArcaSoapPort<FEParamGetPtosVenta, FEPtoVentaResponse> getSalesPointsSoapPort =
                new ArcaSoapClient<>(bp, req -> port.feParamGetPtosVenta(req.getAuth()), soapConfig);

        ArcaSoapPort<FEParamGetTiposCbte, CbteTipoResponse> getVoucherTypesSoapPort =
                new ArcaSoapClient<>(bp, req -> port.feParamGetTiposCbte(req.getAuth()), soapConfig);

        ArcaSoapPort<FEParamGetTiposDoc, DocTipoResponse> getDocumentTypesSoapPort =
                new ArcaSoapClient<>(bp, req -> port.feParamGetTiposDoc(req.getAuth()), soapConfig);

        ArcaSoapPort<FEParamGetTiposIva, IvaTipoResponse> getVatTypesSoapPort =
                new ArcaSoapClient<>(bp, req -> port.feParamGetTiposIva(req.getAuth()), soapConfig);

        ArcaSoapPort<FEParamGetTiposMonedas, MonedaResponse> getCurrenciesSoapPort =
                new ArcaSoapClient<>(bp, req -> port.feParamGetTiposMonedas(req.getAuth()), soapConfig);

        ArcaSoapPort<FEParamGetCotizacion, FECotizacionResponse> getExchangeRateSoapPort =
                new ArcaSoapClient<>(bp, req -> port.feParamGetCotizacion(req.getAuth(), req.getMonId(), req.getFchCotiz()), soapConfig);

        ArcaSoapPort<FECompTotXRequest, FERegXReqResponse> getMaxRecordsSoapPort =
                new ArcaSoapClient<>(bp, req -> port.feCompTotXRequest(req.getAuth()), soapConfig);

        ArcaSoapPort<FEParamGetTiposConcepto, ConceptoTipoResponse> getConceptTypesSoapPort =
                new ArcaSoapClient<>(bp, req -> port.feParamGetTiposConcepto(req.getAuth()), soapConfig);

        ArcaSoapPort<FEParamGetTiposOpcional, OpcionalTipoResponse> getOptionalFieldTypesSoapPort =
                new ArcaSoapClient<>(bp, req -> port.feParamGetTiposOpcional(req.getAuth()), soapConfig);

        ArcaSoapPort<FEParamGetTiposPaises, FEPaisResponse> getCountriesSoapPort =
                new ArcaSoapClient<>(bp, req -> port.feParamGetTiposPaises(req.getAuth()), soapConfig);

        ArcaSoapPort<FEParamGetTiposTributos, FETributoResponse> getTaxTypesSoapPort =
                new ArcaSoapClient<>(bp, req -> port.feParamGetTiposTributos(req.getAuth()), soapConfig);

        ArcaSoapPort<FEParamGetActividades, FEActividadesResponse> getActivitiesSoapPort =
                new ArcaSoapClient<>(bp, req -> port.feParamGetActividades(req.getAuth()), soapConfig);

        ArcaSoapPort<FEParamGetCondicionIvaReceptor, CondicionIvaReceptorResponse> getReceiverVatConditionsSoapPort =
                new ArcaSoapClient<>(bp, req -> port.feParamGetCondicionIvaReceptor(req.getAuth(), req.getClaseCmp()), soapConfig);

        ArcaSoapPort<FECAEASolicitar, FECAEAGetResponse> requestCaeaSoapPort =
                new ArcaSoapClient<>(bp, req -> port.fecaeaSolicitar(req.getAuth(), req.getPeriodo(), req.getOrden()), soapConfig);

        ArcaSoapPort<FECAEARegInformativo, FECAEAResponse> reportCaeaSoapPort =
                new ArcaSoapClient<>(bp, req -> port.fecaeaRegInformativo(req.getAuth(), req.getFeCAEARegInfReq()), soapConfig);

        ArcaSoapPort<FECAEAConsultar, FECAEAGetResponse> queryCaeaSoapPort =
                new ArcaSoapClient<>(bp, req -> port.fecaeaConsultar(req.getAuth(), req.getPeriodo(), req.getOrden()), soapConfig);

        ArcaSoapPort<FECAEASinMovimientoInformar, FECAEASinMovResponse> reportCaeaNoMovementSoapPort =
                new ArcaSoapClient<>(bp, req -> port.fecaeaSinMovimientoInformar(req.getAuth(), req.getPtoVta(), req.getCAEA()), soapConfig);

        ArcaSoapPort<FECAEASinMovimientoConsultar, FECAEASinMovConsResponse> queryCaeaNoMovementSoapPort =
                new ArcaSoapClient<>(bp, req -> port.fecaeaSinMovimientoConsultar(req.getAuth(), req.getCAEA(), req.getPtoVta()), soapConfig);

        GetLastVoucherUseCase getLastVoucherUseCase =
                new GetLastVoucherUseCase(config, authProvider, lastVoucherSoapPort);

        RequestCaeUseCase requestCaeUseCase =
                new RequestCaeUseCase(config, authProvider, requestCaeSoapPort);

        GetSalesPointsUseCase getSalesPointsUseCase =
                new GetSalesPointsUseCase(config, authProvider, getSalesPointsSoapPort);

        GetVoucherUseCase getVoucherUseCase =
                new GetVoucherUseCase(config, authProvider, getVoucherSoapPort);

        GetVoucherTypesUseCase getVoucherTypesUseCase =
                new GetVoucherTypesUseCase(config, authProvider, getVoucherTypesSoapPort);

        GetDocumentTypesUseCase getDocumentTypesUseCase =
                new GetDocumentTypesUseCase(config, authProvider, getDocumentTypesSoapPort);

        GetVatTypesUseCase getVatTypesUseCase =
                new GetVatTypesUseCase(config, authProvider, getVatTypesSoapPort);

        GetCurrenciesUseCase getCurrenciesUseCase =
                new GetCurrenciesUseCase(config, authProvider, getCurrenciesSoapPort);

        GetExchangeRateUseCase getExchangeRateUseCase =
                new GetExchangeRateUseCase(config, authProvider, getExchangeRateSoapPort);

        GetMaxRecordsUseCase getMaxRecordsUseCase =
                new GetMaxRecordsUseCase(config, authProvider, getMaxRecordsSoapPort);

        GetConceptTypesUseCase getConceptTypesUseCase =
                new GetConceptTypesUseCase(config, authProvider, getConceptTypesSoapPort);

        GetOptionalFieldTypesUseCase getOptionalFieldTypesUseCase =
                new GetOptionalFieldTypesUseCase(config, authProvider, getOptionalFieldTypesSoapPort);

        GetCountriesUseCase getCountriesUseCase =
                new GetCountriesUseCase(config, authProvider, getCountriesSoapPort);

        GetTaxTypesUseCase getTaxTypesUseCase =
                new GetTaxTypesUseCase(config, authProvider, getTaxTypesSoapPort);

        GetActivitiesUseCase getActivitiesUseCase =
                new GetActivitiesUseCase(config, authProvider, getActivitiesSoapPort);

        GetReceiverVatConditionsUseCase getReceiverVatConditionsUseCase =
                new GetReceiverVatConditionsUseCase(config, authProvider, getReceiverVatConditionsSoapPort);

        RequestCaeaUseCase requestCaeaUseCase =
                new RequestCaeaUseCase(config, authProvider, requestCaeaSoapPort);

        ReportCaeaUseCase reportCaeaUseCase =
                new ReportCaeaUseCase(config, authProvider, reportCaeaSoapPort);

        QueryCaeaUseCase queryCaeaUseCase =
                new QueryCaeaUseCase(config, authProvider, queryCaeaSoapPort);

        ReportCaeaNoMovementUseCase reportCaeaNoMovementUseCase =
                new ReportCaeaNoMovementUseCase(config, authProvider, reportCaeaNoMovementSoapPort);

        QueryCaeaNoMovementUseCase queryCaeaNoMovementUseCase =
                new QueryCaeaNoMovementUseCase(config, authProvider, queryCaeaNoMovementSoapPort);

        java.util.concurrent.ExecutorService executorService = java.util.concurrent.Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("arca-wsfev1-batch-thread");
            return t;
        });

        BatchProcessUseCase batchProcessUseCase =
                new BatchProcessUseCase(requestCaeUseCase, executorService);

        io.github.fr4ncisx.arca.wsfev1.internal.client.FedummyClient fedummyClient =
                new io.github.fr4ncisx.arca.wsfev1.internal.client.FedummyClient(port);

        return new DefaultWsfeClient(
                config,
                getLastVoucherUseCase,
                requestCaeUseCase,
                getSalesPointsUseCase,
                getVoucherUseCase,
                batchProcessUseCase,
                getVoucherTypesUseCase,
                getDocumentTypesUseCase,
                getVatTypesUseCase,
                getCurrenciesUseCase,
                getExchangeRateUseCase,
                getMaxRecordsUseCase,
                getConceptTypesUseCase,
                getOptionalFieldTypesUseCase,
                getCountriesUseCase,
                getTaxTypesUseCase,
                getActivitiesUseCase,
                getReceiverVatConditionsUseCase,
                requestCaeaUseCase,
                reportCaeaUseCase,
                queryCaeaUseCase,
                reportCaeaNoMovementUseCase,
                queryCaeaNoMovementUseCase,
                fedummyClient
        );
    }
}
