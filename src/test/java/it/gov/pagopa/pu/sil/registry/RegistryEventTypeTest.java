package it.gov.pagopa.pu.sil.registry;

import it.gov.pagopa.pu.registries.dto.generated.RegistrySilEventType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class RegistryEventTypeTest {

  @Test
  void testAlignmentWithRegistries(){
    Assertions.assertEquals(
      RegistryEventType.values().length,
      RegistrySilEventType.values().length);

    Assertions.assertEquals(
      Arrays.stream(RegistryEventType.values()).map(RegistryEventType::name).sorted().toList(),
      Arrays.stream(RegistrySilEventType.values()).map(RegistrySilEventType::getValue).sorted().toList()
    );
  }
}
