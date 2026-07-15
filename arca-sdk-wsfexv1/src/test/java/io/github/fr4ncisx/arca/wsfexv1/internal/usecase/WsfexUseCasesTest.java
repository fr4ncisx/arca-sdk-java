package io.github.fr4ncisx.arca.wsfexv1.internal.usecase;

import io.github.fr4ncisx.arca.core.config.ArcaConfig;
import io.github.fr4ncisx.arca.core.config.ArcaEnvironment;
import io.github.fr4ncisx.arca.core.tax.Cuit;
import io.github.fr4ncisx.arca.soap.spi.ArcaSoapPort;
import io.github.fr4ncisx.arca.wsaa.internal.auth.AuthProvider;
import io.github.fr4ncisx.arca.wsaa.model.ArcaAccessTicket;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXGetCMPR;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXLastCMP;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXLastCMPResponse;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.ClsFEXOutAuthorize;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.FEXAuthorize;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.FEXGetCMP;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.FEXGetCMPResponseDataType;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.FEXResponseAuthorize;
import io.github.fr4ncisx.arca.wsfexv1.internal.generated.FEXResponseLastCMP;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportVoucherConsultRequest;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportVoucherConsultResponse;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportVoucherRequest;
import io.github.fr4ncisx.arca.wsfexv1.model.ExportVoucherResponse;
import io.github.fr4ncisx.arca.wsfexv1.model.LastExportVoucherRequest;
import io.github.fr4ncisx.arca.wsfexv1.model.LastExportVoucherResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WsfexUseCasesTest {

    private ArcaConfig config;
    private AuthProvider authProvider;
    private ArcaAccessTicket ticket;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        config = new ArcaConfig(
                Cuit.parse("20-33333333-4"),
                ArcaEnvironment.HOMOLOGACION,
                Duration.ofSeconds(5),
                Duration.ofSeconds(5)
        );
        authProvider = mock(AuthProvider.class);
        ticket = new ArcaAccessTicket("token-123", "sign-456", Instant.now(), Instant.now().plusSeconds(3600));
        when(authProvider.authenticate("wsfex")).thenReturn(ticket);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getLastExportVoucherUseCaseExecutesCorrectly() {
        ArcaSoapPort<ClsFEXLastCMP, FEXResponseLastCMP> soapPort = mock(ArcaSoapPort.class);

        FEXResponseLastCMP soapResponse = new FEXResponseLastCMP();
        ClsFEXLastCMPResponse result = new ClsFEXLastCMPResponse();
        result.setCbteNro(125L);
        result.setCbteFecha("20260714");
        soapResponse.setFEXResultLastCMP(result);

        when(soapPort.invoke(any(ClsFEXLastCMP.class))).thenReturn(soapResponse);

        GetLastExportVoucherUseCase useCase = new GetLastExportVoucherUseCase(config, authProvider, soapPort);
        LastExportVoucherRequest request = new LastExportVoucherRequest(1, (short) 19);
        LastExportVoucherResponse response = useCase.execute(request);

        assertThat(response).isNotNull();
        assertThat(response.lastNumber()).isEqualTo(125L);
        assertThat(response.lastDate()).isEqualTo("20260714");
    }

    @Test
    @SuppressWarnings("unchecked")
    void authorizeExportVoucherUseCaseExecutesCorrectly() {
        ArcaSoapPort<FEXAuthorize, FEXResponseAuthorize> soapPort = mock(ArcaSoapPort.class);

        FEXResponseAuthorize authResult = new FEXResponseAuthorize();
        ClsFEXOutAuthorize out = new ClsFEXOutAuthorize();
        out.setId(1L);
        out.setCae("CAE-12345");
        out.setFchVencCae("20260831");
        authResult.setFEXResultAuth(out);

        when(soapPort.invoke(any(FEXAuthorize.class))).thenReturn(authResult);

        AuthorizeExportVoucherUseCase useCase = new AuthorizeExportVoucherUseCase(config, authProvider, soapPort);
        ExportVoucherRequest request = new ExportVoucherRequest(
                1L, "20260714", (short) 19, 1, 126L, (short) 2, "N", List.of(), (short) 200,
                "Client", 0L, "Address", "TaxId", "DOL", BigDecimal.ONE, "N", "", BigDecimal.TEN, "",
                List.of(), "", "", "", (short) 1, List.of(), List.of(), "20260714", List.of()
        );
        ExportVoucherResponse response = useCase.execute(request);

        assertThat(response).isNotNull();
        assertThat(response.cae()).isEqualTo("CAE-12345");
        assertThat(response.caeExpirationDate()).isEqualTo("20260831");
        assertThat(response.isApproved()).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getExportVoucherUseCaseExecutesCorrectly() {
        ArcaSoapPort<FEXGetCMP, FEXGetCMPResponseDataType> soapPort = mock(ArcaSoapPort.class);

        FEXGetCMPResponseDataType dataType = new FEXGetCMPResponseDataType();
        ClsFEXGetCMPR get = new ClsFEXGetCMPR();
        get.setId(5L);
        get.setCbteNro(10L);
        get.setCae("CAE-8888");
        get.setResultado("A");
        dataType.setFEXResultGet(get);

        when(soapPort.invoke(any(FEXGetCMP.class))).thenReturn(dataType);

        GetExportVoucherUseCase useCase = new GetExportVoucherUseCase(config, authProvider, soapPort);
        ExportVoucherConsultRequest request = new ExportVoucherConsultRequest(1, (short) 19, 10L);
        ExportVoucherConsultResponse response = useCase.execute(request);

        assertThat(response).isNotNull();
        assertThat(response.detail()).isPresent();
        assertThat(response.detail().get().cae()).isEqualTo("CAE-8888");
        assertThat(response.detail().get().status()).isEqualTo("A");
    }
}
