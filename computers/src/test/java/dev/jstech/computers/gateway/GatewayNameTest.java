/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gateway;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GatewayNameTest {

    @Test
    void defaultFor_countsFromOne() {
        assertEquals("gateway-1", GatewayName.defaultFor(1));
        assertEquals("gateway-3", GatewayName.defaultFor(3));
        assertEquals("gateway-1", GatewayName.defaultFor(0));
    }

    @Test
    void clean_lowersAndKeepsLettersDigitsDashesAndUnderscores() {
        assertEquals("cc-bridge", GatewayName.clean("CC-Bridge"));
        assertEquals("farm_link2", GatewayName.clean("farm_link2"));
    }

    @Test
    void clean_foldsOtherRunsIntoOneDash() {
        assertEquals("my-gateway", GatewayName.clean("my   gateway"));
        assertEquals("a-b", GatewayName.clean("a!!!b"));
    }

    @Test
    void clean_dropsDashesAtTheEnds() {
        assertEquals("edge", GatewayName.clean("--edge--"));
        assertEquals("", GatewayName.clean("   "));
        assertEquals("", GatewayName.clean(null));
    }

    @Test
    void clean_cutsToTheLimit() {
        final String longName = "abcdefghijklmnopqrstuvwxyz0123456789";
        assertEquals(GatewayName.MAX, GatewayName.clean(longName).length());
    }

    @Test
    void clean_cutDoesNotLeaveADashAtTheEnd() {
        final String typed = "a".repeat(GatewayName.MAX - 1) + " b";
        assertEquals("a".repeat(GatewayName.MAX - 1), GatewayName.clean(typed));
    }

    @Test
    void peripheralName_prefixesAndUsesUnderscores() {
        assertEquals("jsc_gateway_cc_bridge", GatewayName.peripheralName("cc-bridge"));
        assertEquals("jsc_gateway_gateway_1", GatewayName.peripheralName(GatewayName.defaultFor(1)));
    }

    @Test
    void freeDefault_skipsTakenNames() {
        final Set<String> taken = Set.of("gateway-1", "gateway-2");
        assertEquals("gateway-3", GatewayName.freeDefault(1, taken::contains));
        assertEquals("gateway-1", GatewayName.freeDefault(1, name -> false));
    }

    @Test
    void peripheralName_dashAndUnderscoreCollideOnPurpose() {
        assertEquals(GatewayName.peripheralName("a-b"), GatewayName.peripheralName("a_b"));
    }
}
