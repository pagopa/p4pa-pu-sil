package it.gov.pagopa.pu.sil.connector.pagopapayments.client;


import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.sil.connector.pagopapayments.config.PagoPaPaymentsApisHolder;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;


@Service
public class PagopaPaymentsClient {
	private final PagoPaPaymentsApisHolder pagoPaPaymentsApisHolder;

	public PagopaPaymentsClient(PagoPaPaymentsApisHolder pagoPaPaymentsApisHolder) {
		this.pagoPaPaymentsApisHolder = pagoPaPaymentsApisHolder;
	}

	public Resource generateNotice(String nav, DebtPositionDTO debtPosition, String accessToken) {
		return pagoPaPaymentsApisHolder.getPrintPaymentNoticeApi(accessToken).generateNotice(nav, debtPosition);
	}
}
