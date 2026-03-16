package it.gov.pagopa.pu.sil.connector.pagopapayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.sil.connector.pagopapayments.client.PagopaPaymentsClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Lazy
@Service
@Slf4j
public class PagopaPaymentsServiceImpl implements PagopaPaymentsService {
	private final PagopaPaymentsClient pagopaPaymentsClient;

	public PagopaPaymentsServiceImpl(PagopaPaymentsClient pagopaPaymentsClient) {
		this.pagopaPaymentsClient = pagopaPaymentsClient;
	}

	@Override
	public Resource generateNotice(String nav, DebtPositionDTO debtPosition, String accessToken) {
		return pagopaPaymentsClient.generateNotice(nav, debtPosition, accessToken);
	}
}
