package it.gov.pagopa.pu.sil.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CryptoUtilsTest {

    @Test
    void whenSha256Base64ThenOk(){
        Assertions.assertEquals(
                "f9SGUoWD/kZFYdz81VpXWA9SCqyEw0hZXvnSdwuRRG8=",
                CryptoUtils.sha256Base64("PROVA"));
    }

    @Test
    void whenSha256HEXThenOk(){
        Assertions.assertEquals(
                "7FD486528583FE464561DCFCD55A57580F520AAC84C348595EF9D2770B91446F",
                CryptoUtils.sha256HEX("PROVA"));
    }
}
