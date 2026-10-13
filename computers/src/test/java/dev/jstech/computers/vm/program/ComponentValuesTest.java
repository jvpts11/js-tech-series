/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** What a generic component may hold, and the run of letters it travels as between the server and the screens. */
class ComponentValuesTest {

    @Test
    void encode_writesEveryKindOfValueTaggedByWhatItIs() {
        final Values.ListValue list = new Values.ListValue();
        list.items().addAll(Arrays.asList(null, true, 7, 8L, 1.5, 'c', "hi"));

        assertEquals("[nti7;l8;d1.5;ccs2:hi]", ComponentValues.encode(list));
    }

    @Test
    void decode_readsBackWhatEncodeWrote() {
        final Values.MapValue map = new Values.MapValue();
        final Values.ListValue inner = new Values.ListValue();
        inner.items().addAll(List.of(1, "two"));
        map.entries().put("list", inner);
        map.entries().put("flag", false);

        final Object read = ComponentValues.decode(ComponentValues.encode(map));

        final Map<Object, Object> expected = new LinkedHashMap<>();
        expected.put("list", List.of(1, "two"));
        expected.put("flag", false);
        assertEquals(expected, read);
    }

    @Test
    void decode_readsATextHoldingTheLettersTheTagsAreMadeOf() {
        assertEquals("[s3:}]{]", ComponentValues.encodePlain(List.of("}]{")));
        assertEquals(List.of("}]{"), ComponentValues.decode("[s3:}]{]"));
    }

    @Test
    void encodePlain_writesPlainValuesAsTheProgramsOwnWouldBe() {
        final Values.ListValue list = new Values.ListValue();
        list.items().addAll(List.of(1, "a"));

        assertEquals(ComponentValues.encode(list), ComponentValues.encodePlain(List.of(1, "a")));
    }

    @Test
    void decode_refusesWhatIsNotAValue() {
        assertThrows(IllegalArgumentException.class, () -> ComponentValues.decode("x"));
        assertThrows(IllegalArgumentException.class, () -> ComponentValues.decode("i1;i2;"), "two values");
        assertThrows(IllegalArgumentException.class, () -> ComponentValues.decode("s9:short"));
        assertThrows(IllegalArgumentException.class, () -> ComponentValues.decode("[i1;"));
    }

    @Test
    void decode_refusesAValuePastTheBounds() {
        final String deep = "[".repeat(ComponentValues.MOST_DEPTH + 2) + "]".repeat(ComponentValues.MOST_DEPTH + 2);
        assertThrows(IllegalArgumentException.class, () -> ComponentValues.decode(deep));
        final String many = "[" + "n".repeat(ComponentValues.MOST_NODES + 1) + "]";
        assertThrows(IllegalArgumentException.class, () -> ComponentValues.decode(many));
        assertThrows(IllegalArgumentException.class,
                () -> ComponentValues.decode("s" + (ComponentValues.MOST_BYTES + 1) + ":"));
    }

    @Test
    void copy_isMadeOfPartsOfItsOwnOnTheHeap() {
        final Heap heap = new Heap(64 * 1024);
        final Values.ListValue list = new Values.ListValue();
        list.items().add(heap.text("word", 0));

        final ComponentValues.Copied copied = ComponentValues.copy(list, heap, 0);

        final Values.ListValue made = (Values.ListValue) copied.value();
        assertNotSame(list, made);
        assertNotSame(list.items().getFirst(), made.items().getFirst());
        assertTrue(heap.bytesOf(made) > 0, "the copy is the program's to hold, and weighs what it weighs");
        assertTrue(copied.bytes() > 0);
    }

    @Test
    void copy_refusesAValueThatHoldsItself() {
        final Heap heap = new Heap(64 * 1024);
        final Values.ListValue loop = new Values.ListValue();
        loop.items().add(loop);

        final Halt halt = assertThrows(Halt.class, () -> ComponentValues.copy(loop, heap, 3));

        assertEquals(Halt.Reason.REFUSED, halt.reason());
        assertEquals(3, halt.line());
        assertTrue(halt.text().english().contains("holds itself"), halt.text().english());
    }

    @Test
    void copy_refusesAValueHeavierThanTheBound() {
        final Heap heap = new Heap(1024 * 1024);
        final String big = "x".repeat(ComponentValues.MOST_BYTES);

        assertThrows(Halt.class, () -> ComponentValues.copy(big, heap, 1));
    }
}
