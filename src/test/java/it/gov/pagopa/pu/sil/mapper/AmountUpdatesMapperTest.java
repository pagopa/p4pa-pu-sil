package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.actualization.dto.generated.UpdatedPayment;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.pu.sil.dto.generated.ActualizationResultDTO;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.junit.jupiter.api.Test;
import uk.co.jemos.podam.api.PodamFactory;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AmountUpdatesMapperTest {
    private final AmountUpdatesMapper mapper = new AmountUpdatesMapper();

    private final PodamFactory podamFactory = TestUtils.getPodamFactory();

    @Test
    void testPagamentoAggiornato2AmountUpdatesDTO() {
        PagamentoAggiornato pagamento = new PagamentoAggiornato();
        pagamento.setNumeroAvviso("NAV123");
        pagamento.setIun("IUN456");
        pagamento.setSpeseNotifica(1_00L);
        OffsetDateTime now = OffsetDateTime.now();
        pagamento.setDataVisualizzazione(now);
        pagamento.setImportoPosizione(50_00L);
        pagamento.setDataPerfezionamentoDecorrenzaTermini(now.plusDays(1));
        pagamento.setBilancio("BILANCIO-JSON");
        pagamento.setCodice(PagamentoAggiornato.CodiceEnum._004);
        pagamento.setDettaglio("Some error description");

      ActualizationResultDTO dto = mapper.pagamentoAggiornato2AmountUpdatesDTO(pagamento);

        assertNotNull(dto);
        assertEquals("NAV123", dto.getNav());
        assertEquals("IUN456", dto.getIun());
        assertEquals(1_00L, dto.getNotificationFeeCents());
        assertEquals(now, dto.getDisplayDate());
        assertEquals(50_00L, dto.getUpdatedAmountCents());
        assertEquals(now.plusDays(1), dto.getCompletionDeadlineDate());
        assertEquals("BILANCIO-JSON", dto.getBalance());
    }

  @Test
  void testUpdatedPayment2AmountUpdatesDTO() {
    UpdatedPayment updatedPayment = podamFactory.manufacturePojo(UpdatedPayment.class);

    ActualizationResultDTO dto = mapper.updatedPayment2AmountUpdatesDTO(updatedPayment);

    assertNotNull(dto);
    assertEquals(updatedPayment.getNav(), dto.getNav());
    assertEquals(updatedPayment.getIun(), dto.getIun());
    assertEquals(updatedPayment.getNotificationFeeCents(), dto.getNotificationFeeCents());
    assertNull(dto.getDisplayDate());
    assertEquals(updatedPayment.getAmountCents(), dto.getUpdatedAmountCents());
    assertEquals(updatedPayment.getRetentionDate(), dto.getCompletionDeadlineDate());
    assertEquals(updatedPayment.getBalance(), dto.getBalance());

    TestUtils.checkAllNotNullFields(dto, "displayDate", "errorCode", "errorDescription");
  }
}
