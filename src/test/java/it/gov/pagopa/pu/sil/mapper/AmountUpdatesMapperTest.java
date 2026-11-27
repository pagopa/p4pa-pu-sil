package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.sil.dto.generated.ActualizationResultDTO;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.gov.pagopa.sil.actualization.dto.generated.UpdatedPayment;
import it.gov.pagopa.sil.actualizationlegacy.dto.generated.PagamentoAggiornato;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AmountUpdatesMapperTest {
  @Mock
  private BalanceMapper balanceMapperMock;
  private AmountUpdatesMapper mapper;
  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  private final String jsonBalance = "[{" +
    "\"capitolo\": \"Dionysus\"," +
    "\"ufficio\": \"Prometheus\"," +
    "\"accertamento\": \"Meleager\"," +
    "\"importo\": 9.53" +
    "}]";

  private final String expectedBalanceXml = "<bilancio><capitolo><codCapitolo>Dionysus</codCapitolo>" +
    "<codUfficio>Prometheus</codUfficio>" +
    "<accertamento><codAccertamento>Meleager</codAccertamento>" +
    "<importo>9.53</importo></accertamento></capitolo></bilancio>";

  @BeforeEach
  void setUp() {
    mapper = new AmountUpdatesMapper(balanceMapperMock);
  }

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
    pagamento.setBilancio(jsonBalance);
    pagamento.setCodice(PagamentoAggiornato.CodiceEnum._004);
    pagamento.setDettaglio("Some error description");

    when(balanceMapperMock.mapBalanceFromSil(jsonBalance)).thenReturn(expectedBalanceXml);
    ActualizationResultDTO dto = mapper.pagamentoAggiornato2AmountUpdatesDTO(pagamento);

    assertNotNull(dto);
    assertEquals("NAV123", dto.getNav());
    assertEquals("IUN456", dto.getIun());
    assertEquals(1_00L, dto.getNotificationFeeCents());
    assertEquals(now, dto.getDisplayDate());
    assertEquals(50_00L, dto.getUpdatedAmountCents());
    assertEquals(now.plusDays(1), dto.getCompletionDeadlineDate());
    assertEquals(expectedBalanceXml, dto.getBalance());
  }

  @Test
  void testUpdatedPayment2AmountUpdatesDTO() {
    UpdatedPayment updatedPayment = podamFactory.manufacturePojo(UpdatedPayment.class);
    updatedPayment.setBalance(jsonBalance);

    when(balanceMapperMock.mapBalanceFromSil(jsonBalance)).thenReturn(expectedBalanceXml);
    ActualizationResultDTO dto = mapper.updatedPayment2AmountUpdatesDTO(updatedPayment);

    assertNotNull(dto);
    assertEquals(updatedPayment.getNav(), dto.getNav());
    assertEquals(updatedPayment.getIun(), dto.getIun());
    assertEquals(updatedPayment.getNotificationFeeCents(), dto.getNotificationFeeCents());
    assertNull(dto.getDisplayDate());
    assertEquals(updatedPayment.getAmountCents(), dto.getUpdatedAmountCents());
    assertEquals(updatedPayment.getRetentionDate(), dto.getCompletionDeadlineDate());
    assertEquals(expectedBalanceXml, dto.getBalance());

    TestUtils.checkAllNotNullFields(dto, "displayDate", "errorCode", "errorDescription");
  }
}
