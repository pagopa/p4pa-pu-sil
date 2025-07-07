package it.gov.pagopa.pu.sil.service.queryassessments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.veneto.regione.pagamenti.pivot.ente.PivotSILChiediAccertamento;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueryAssessmentsServiceTest {
    private final QueryAssessmentsService service = new QueryAssessmentsService();

    @Test
    void testHandlePivotSILChiediAccertamento_ThrowsUnsupportedOperation() {
        UserInfo userInfo = new UserInfo();
        String accessToken = "token";
        String orgIpaCode = "ipa";
        PivotSILChiediAccertamento request = new PivotSILChiediAccertamento();
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () ->
            service.handlePivotSILChiediAccertamento(userInfo, accessToken, orgIpaCode, request)
        );
        assertTrue(ex.getMessage().contains("not yet implemented"));
    }
}

