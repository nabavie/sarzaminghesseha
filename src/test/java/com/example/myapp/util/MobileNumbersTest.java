package com.example.myapp.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MobileNumbersTest {

    @Test
    void normalizeOptionalBlankIsNull() {
        assertNull(MobileNumbers.normalizeOptional(""));
        assertNull(MobileNumbers.normalizeOptional("   "));
        assertNull(MobileNumbers.normalizeOptional(null));
    }

    @Test
    void normalizeRequiredRejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> MobileNumbers.normalizeRequired(""));
    }

    @Test
    void acceptsLocalPlusAndIntl() {
        assertEquals("09123456789", MobileNumbers.normalizeRequired("09123456789"));
        assertEquals("09123456789", MobileNumbers.normalizeRequired("+989123456789"));
        assertEquals("09123456789", MobileNumbers.normalizeRequired("989123456789"));
        assertEquals("09123456789", MobileNumbers.normalizeRequired("۰۹۱۲۳۴۵۶۷۸۹"));
    }

    @Test
    void toSmsIrKeepsLocal09Format() {
        assertEquals("09123456789", MobileNumbers.toSmsIr("09123456789"));
    }
}
