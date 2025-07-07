package it.gov.pagopa.pu.sil.service.queryassessments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.veneto.regione.pagamenti.pivot.ente.PivotSILChiediAccertamento;
import it.veneto.regione.pagamenti.pivot.ente.PivotSILChiediAccertamentoRisposta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class QueryAssessmentsService {
    public PivotSILChiediAccertamentoRisposta handlePivotSILChiediAccertamento(
      UserInfo userInfo,
      String accessToken,
      String orgIpaCode,
      PivotSILChiediAccertamento request) {
        throw new UnsupportedOperationException("pivotSILChiediAccertamento SOAP action is not yet implemented");
    }
}
