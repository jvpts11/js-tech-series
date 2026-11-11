/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextToolFunctionsTest {

    private static final int LINE = 5;
    private static final Heap HEAP = new Heap(1024L * 1024);

    @Test
    void builder_appendsPiecesAndGivesThemBackAsOneText() {
        final Values.Obj builder = TextToolFunctions.newBuilder(HEAP, LINE);
        call(TextToolFunctions.BUILDER, builder, "Append", List.of("string"), "iron ");
        call(TextToolFunctions.BUILDER, builder, "AppendLine", List.of("string"), "ingot");
        call(TextToolFunctions.BUILDER, builder, "Append", List.of("string"), "64");
        assertEquals("iron ingot\n64", call(TextToolFunctions.BUILDER, builder, "ToString", List.of()));
        assertEquals(13, TextToolFunctions.length(builder));
        call(TextToolFunctions.BUILDER, builder, "Clear", List.of());
        assertEquals("", call(TextToolFunctions.BUILDER, builder, "ToString", List.of()));
        assertEquals(0, TextToolFunctions.length(builder));
    }

    @Test
    void csv_readsQuotedFieldsAndWritesThemBack() {
        final List<List<String>> rows =
                TextToolFunctions.CsvText.read("item,count\n\"iron, raw\",64\r\n\"say \"\"hi\"\"\",1");
        assertEquals(List.of(List.of("item", "count"), List.of("iron, raw", "64"), List.of("say \"hi\"", "1")), rows);
        assertEquals("item,count\n\"iron, raw\",64\n\"say \"\"hi\"\"\",1\n", TextToolFunctions.CsvText.write(rows));
        assertEquals(List.of(), TextToolFunctions.CsvText.read(""));
    }

    @Test
    void hash_givesTheKnownDigests() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                call("Hash", null, "Sha256", List.of("string"), "abc"));
        assertEquals(0x352441C2L, call("Hash", null, "Crc32", List.of("string"), "abc"));
    }

    @Test
    void base64_writesAndReadsATextAndHaltsOnWhatIsNotBase64() {
        assertEquals("aXJvbg==", call("Base64", null, "Encode", List.of("string"), "iron"));
        assertEquals("iron", call("Base64", null, "Decode", List.of("string"), "aXJvbg=="));
        final Halt halt = assertThrows(Halt.class, () -> call("Base64", null, "Decode", List.of("string"), "**"));
        assertEquals(Halt.Thrown.FORMAT, halt.thrown(), halt.getMessage());
    }

    @Test
    void path_takesPathsApartWhicheverSeparatorTheyUse() {
        assertEquals("C:\\logs\\stock.log", TextToolFunctions.PathText.combine("C:\\logs", "stock.log"));
        assertEquals("/home/player/stock.log", TextToolFunctions.PathText.combine("/home/player/", "stock.log"));
        assertEquals("/etc/x", TextToolFunctions.PathText.combine("/home", "/etc/x"));
        assertEquals("stock.log", TextToolFunctions.PathText.fileName("C:\\logs\\stock.log"));
        assertEquals(".log", TextToolFunctions.PathText.extension("/home/player/stock.log"));
        assertEquals("", TextToolFunctions.PathText.extension("/home/.config"));
        assertEquals("C:\\logs", TextToolFunctions.PathText.folder("C:\\logs\\stock.log"));
        assertEquals("C:\\", TextToolFunctions.PathText.folder("C:\\stock.log"));
        assertEquals("/", TextToolFunctions.PathText.folder("/stock.log"));
        assertEquals("", TextToolFunctions.PathText.folder("stock.log"));
    }

    private static Object call(final String type, final Object target, final String name,
                               final List<String> parameters, final Object... arguments) {
        return PureFunctions.REGISTRY.find(type, name, parameters).function().call(HEAP, target, arguments, LINE);
    }
}
