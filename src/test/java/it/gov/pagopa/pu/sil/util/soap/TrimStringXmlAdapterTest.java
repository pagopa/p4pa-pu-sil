package it.gov.pagopa.pu.sil.util.soap;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TrimStringXmlAdapterTest {

    private final TrimStringXmlAdapter adapter = new TrimStringXmlAdapter();

    @Test
    void givenInputIsNullWhenMarshalThenReturnsNull() {
        assertNull(adapter.marshal(null));
    }

    @Test
    void givenInputHasSpacesWhenMarshalThenReturnsTrimmedString() {
        assertEquals("abc", adapter.marshal("  abc  "));
    }

    @Test
    void givenNoSpacesWhenMarshalThenReturnsSameString() {
        assertEquals("abc", adapter.marshal("abc"));
    }

    @Test
    void givenInputIsEmptyWhenMarshalThenReturnsEmptyString() {
        assertEquals("", adapter.marshal(""));
    }

    @Test
    void givenInputIsNullWhenUnmarshalThenReturnsNull() {
        assertNull(adapter.unmarshal(null));
    }

    @Test
    void givenInputHasSpacesWhenUnmarshalThenReturnsTrimmedString() {
        assertEquals("xyz", adapter.unmarshal(" xyz "));
    }

    @Test
    void givenNoSpacesWhenUnmarshalThenReturnsSameString() {
        assertEquals("xyz", adapter.unmarshal("xyz"));
    }

    @Test
    void unmarshal_ReturnsEmptyStringWhenInputIsEmpty() {
        assertEquals("", adapter.unmarshal(""));
    }
}
