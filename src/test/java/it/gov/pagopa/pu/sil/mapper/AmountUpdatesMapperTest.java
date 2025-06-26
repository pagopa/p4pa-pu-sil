package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.pu.sil.dto.generated.AmountUpdatesDTO;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import static org.junit.jupiter.api.Assertions.*;

class AmountUpdatesMapperTest {
    private final AmountUpdatesMapper mapper = new AmountUpdatesMapper();

    @Test
    void testPagamentoAggiornato2AmountUpdatesDTO() {
        PagamentoAggiornato pagamento = new PagamentoAggiornato();
        pagamento.setNumeroAvviso("NAV123");
        pagamento.setIun("IUN456");
        pagamento.setSpeseNotifica(100L);
        OffsetDateTime now = OffsetDateTime.now();
        pagamento.setDataVisualizzazione(now);
        pagamento.setImportoPosizione(5000L);
        pagamento.setDataPerfezionamentoDecorrenzaTermini(now.plusDays(1));
        pagamento.setBilancio("BILANCIO-JSON");
        pagamento.setCodice(PagamentoAggiornato.CodiceEnum._004);
        pagamento.setDettaglio("Some error description");

        AmountUpdatesDTO dto = mapper.pagamentoAggiornato2AmountUpdatesDTO(pagamento);

        assertNotNull(dto);
        assertEquals("NAV123", dto.getNav());
        assertEquals("IUN456", dto.getIun());
        assertEquals(100L, dto.getNotificationFee());
        assertEquals(now, dto.getDisplayDate());
        assertEquals(5000L, dto.getUpdatedAmount());
        assertEquals(now.plusDays(1), dto.getCompletionDeadlineDate());
        assertEquals("BILANCIO-JSON", dto.getBalance());
        assertEquals("004", dto.getErrorCode());
        assertEquals("Some error description", dto.getErrorDescription());
        assertTrue(dto.getIsBlockingError()); // should be true for code _004
    }

    @Test
    void testPagamentoAggiornato2AmountUpdatesDTONullInput() {
        AmountUpdatesDTO dto = mapper.mapToKoAmountUpdatesDTO();
        assertNotNull(dto);
        assertEquals(AmountUpdatesDTO.OutcomeEnum.KO, dto.getOutcome());
        assertFalse(dto.getIsBlockingError());
    }

    @Test
    void testPagamentoAggiornato2AmountUpdatesDTONullCodice() {
        PagamentoAggiornato pagamento = new PagamentoAggiornato();
        pagamento.setNumeroAvviso("NAV999");
        pagamento.setIun("IUN999");
        pagamento.setSpeseNotifica(200L);
        OffsetDateTime now = OffsetDateTime.now();
        pagamento.setDataVisualizzazione(now);
        pagamento.setImportoPosizione(10000L);
        pagamento.setDataPerfezionamentoDecorrenzaTermini(now.plusDays(2));
        pagamento.setBilancio("BILANCIO-NULL-CODE");
        pagamento.setCodice(null);
        pagamento.setDettaglio("No error code");

        AmountUpdatesDTO dto = mapper.pagamentoAggiornato2AmountUpdatesDTO(pagamento);

        assertNotNull(dto);
        assertEquals("NAV999", dto.getNav());
        assertEquals("IUN999", dto.getIun());
        assertEquals(200L, dto.getNotificationFee());
        assertEquals(now, dto.getDisplayDate());
        assertEquals(10000L, dto.getUpdatedAmount());
        assertEquals(now.plusDays(2), dto.getCompletionDeadlineDate());
        assertEquals("BILANCIO-NULL-CODE", dto.getBalance());
        assertNull(dto.getErrorCode()); // should be null if codice is null
        assertEquals("No error code", dto.getErrorDescription());
        assertFalse(dto.getIsBlockingError()); // should be false if codice is null
    }
}
