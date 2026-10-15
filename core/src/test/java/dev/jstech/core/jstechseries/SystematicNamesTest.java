/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SystematicNamesTest {

    @Test
    void name_joinsARootForEachDigitAndIum() {
        assertEquals("ununennium", SystematicNames.name(119));
        assertEquals("unbinilium", SystematicNames.name(120));
        assertEquals("unbiunium", SystematicNames.name(121));
        assertEquals("unhexquadium", SystematicNames.name(164));
    }

    @Test
    void name_dropsTheIOfBiAndTriBeforeIum() {
        assertEquals("unbibium", SystematicNames.name(122));
        assertEquals("unbitrium", SystematicNames.name(123));
        assertEquals("bipentbium", SystematicNames.name(252));
    }

    @Test
    void name_dropsAnNOfEnnBeforeNil() {
        assertEquals("unennilium", SystematicNames.name(190));
        assertEquals("ennilnilium", SystematicNames.name(900));
    }

    @Test
    void title_capitalisesTheFirstLetter() {
        assertEquals("Ununennium", SystematicNames.title(119));
    }

    @Test
    void symbol_takesEachRootsFirstLetter() {
        assertEquals("Uue", SystematicNames.symbol(119));
        assertEquals("Ubn", SystematicNames.symbol(120));
        assertEquals("Bnn", SystematicNames.symbol(200));
        assertEquals("Bpn", SystematicNames.symbol(250));
    }
}
