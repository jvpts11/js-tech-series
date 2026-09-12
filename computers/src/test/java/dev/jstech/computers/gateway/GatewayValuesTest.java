/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.jstech.computers.cannon.run.Values;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GatewayValuesTest {

    private static Values.ListValue listOf(final Object... items) {
        final Values.ListValue list = new Values.ListValue();
        list.items().addAll(List.of(items));
        return list;
    }

    @Test
    void toLua_sendsNumbersAsLuaHasThemAndLettersAsText() {
        assertEquals(3.0, GatewayValues.toLua(3L));
        assertEquals(2.5, GatewayValues.toLua(2.5));
        assertEquals("x", GatewayValues.toLua('x'));
        assertEquals(true, GatewayValues.toLua(true));
        assertEquals("said", GatewayValues.toLua("said"));
        assertNull(GatewayValues.toLua(null));
    }

    @Test
    void toLua_sendsAListAsATableCountedFromOne() {
        final Object sent = GatewayValues.toLua(listOf("a", 2L));
        final Map<?, ?> table = assertInstanceOf(Map.class, sent);
        assertEquals(2, table.size());
        assertEquals("a", table.get(1.0));
        assertEquals(2.0, table.get(2.0));
    }

    @Test
    void toLua_sendsAMapAsATableWithItsOwnKeys() {
        final Values.MapValue map = new Values.MapValue();
        map.entries().put("left", 4L);
        map.entries().put(2L, "two");
        final Map<?, ?> table = assertInstanceOf(Map.class, GatewayValues.toLua(map));
        assertEquals(4.0, table.get("left"));
        assertEquals("two", table.get(2.0));
    }

    @Test
    void fromLua_bringsAWholeNumberBackWhole() {
        assertEquals(7L, GatewayValues.fromLua(7.0));
        assertEquals(2.5, GatewayValues.fromLua(2.5));
        assertEquals(true, GatewayValues.fromLua(true));
        assertEquals("said", GatewayValues.fromLua("said"));
        assertNull(GatewayValues.fromLua(null));
    }

    @Test
    void fromLua_bringsARunFromOneBackAsAList() {
        final Map<Object, Object> table = new LinkedHashMap<>();
        table.put(1.0, "a");
        table.put(2.0, 3.0);
        final Values.ListValue list = assertInstanceOf(Values.ListValue.class, GatewayValues.fromLua(table));
        assertEquals(List.of("a", 3L), list.items());
    }

    @Test
    void fromLua_bringsAnyOtherTableBackAsAMap() {
        final Map<Object, Object> table = new LinkedHashMap<>();
        table.put("width", 51.0);
        table.put("height", 19.0);
        final Values.MapValue map = assertInstanceOf(Values.MapValue.class, GatewayValues.fromLua(table));
        assertEquals(51L, map.entries().get("width"));
        assertEquals(19L, map.entries().get("height"));
    }

    @Test
    void fromLua_bringsSeveralAnswersBackAsAList() {
        final Values.ListValue list =
                assertInstanceOf(Values.ListValue.class, GatewayValues.fromLua(new Object[] {51.0, 19.0}));
        assertEquals(List.of(51L, 19L), list.items());
    }

    @Test
    void bothWays_carryATableInsideATable() {
        final Values.ListValue nested = listOf(listOf("a", "b"), 1L);
        final Object sent = GatewayValues.toLua(nested);
        final Values.ListValue back = assertInstanceOf(Values.ListValue.class, GatewayValues.fromLua(sent));
        assertEquals(2, back.items().size());
        assertEquals(List.of("a", "b"),
                assertInstanceOf(Values.ListValue.class, back.items().getFirst()).items());
        assertEquals(1L, back.items().get(1));
    }
}
