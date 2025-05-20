package it.gov.pagopa.pu.sil.util.soap;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TrimStringXmlAdapterTest {

    private final TrimStringXmlAdapter adapter = new TrimStringXmlAdapter();

    static Stream<Arguments> provideStrings() {
        return Stream.of(
            org.junit.jupiter.params.provider.Arguments.of(null, null),
            org.junit.jupiter.params.provider.Arguments.of(" xyz ", "xyz"),
            org.junit.jupiter.params.provider.Arguments.of("xyz", "xyz"),
            org.junit.jupiter.params.provider.Arguments.of("", "")
        );
    }

    @ParameterizedTest
    @MethodSource("provideStrings")
    void testMarshal(String input, String expected) {
        assertEquals(expected, adapter.marshal(input));
    }

    @ParameterizedTest
    @MethodSource("provideStrings")
    void testUnmarshal(String input, String expected) {
        assertEquals(expected, adapter.unmarshal(input));
    }
}
