/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.vm.listing.AsmReader;
import java.util.List;
import org.junit.jupiter.api.Test;

class JsonFunctionsTest {

    private static final int LINE = 3;
    private static final Heap HEAP = new Heap(1024L * 1024);

    @Test
    void parse_makesATreeReadByNameAndNumber() {
        final Values.Obj doc = parse("{\"items\": [{\"name\": \"ore\", \"count\": 64}], \"on\": true}");
        final Values.Obj first = (Values.Obj) call(call(doc, "Get", List.of("string"), "items"), "At",
                List.of("int"), 0);
        assertEquals("ore", call(call(first, "Get", List.of("string"), "name"), "AsString", List.of()));
        assertEquals(64.0, call(call(first, "Get", List.of("string"), "count"), "AsNumber", List.of()));
        assertEquals("64", call(call(first, "Get", List.of("string"), "count"), "AsString", List.of()));
        assertEquals(true, call(call(doc, "Get", List.of("string"), "on"), "AsBool", List.of()));
        assertEquals(JsonFunctions.OBJECT, doc.get(JsonFunctions.KIND));
        assertEquals(2, JsonFunctions.count(doc));
        assertEquals(List.of("items", "on"), ((Values.ListValue) call(doc, "Keys", List.of())).items());
    }

    @Test
    void get_givesNullForAMemberThatIsNotThere() {
        final Values.Obj doc = parse("{\"a\": 1}");
        assertNull(call(doc, "Get", List.of("string"), "b"));
        assertEquals(false, call(doc, "Has", List.of("string"), "b"));
        assertNull(call(parse("[1]"), "Get", List.of("string"), "a"));
    }

    @Test
    void at_haltsOutsideTheArray() {
        final Halt halt = assertThrows(Halt.class, () -> call(parse("[1]"), "At", List.of("int"), 2));
        assertEquals(Halt.Reason.OUT_OF_RANGE, halt.reason(), halt.getMessage());
    }

    @Test
    void parse_haltsWithAFormatExceptionOnWhatIsNotJson() {
        final Halt halt = assertThrows(Halt.class, () -> parse("{\"a\": }"));
        assertEquals(Halt.Thrown.FORMAT, halt.thrown(), halt.getMessage());
    }

    @Test
    void asNumber_haltsOnAnObject() {
        final Halt halt = assertThrows(Halt.class, () -> call(parse("{}"), "AsNumber", List.of()));
        assertEquals(Halt.Thrown.FORMAT, halt.thrown(), halt.getMessage());
    }

    @Test
    void indexer_readsByNameAndByNumberAndIsNeverWritten() {
        final String prelude = "using System.*; using System.IO.*; using System.Text.*; using Standard.Script; "
                + "namespace Tests; class Says : Script { public override void OnTick() { ";
        final SigmaCompiler.Result built = SigmaCompiler.compile(List.of(new SourceFile("Says.sgs", prelude
                + "JsonValue doc = Json.Parse(\"{\\\"items\\\": [{\\\"name\\\": \\\"ore\\\"}]}\"); "
                + "Console.PrintLine(doc[\"items\"][0][\"name\"].AsString() + \" \" + doc.Count + \" \" + doc.Kind); "
                + "} }")));
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        final AsmReader reader = new AsmReader(built.assembly());
        final ProgramImage program = ProgramImage.of(reader.read());
        final Process process = new Process(program, 256L * 1024, IHost.still());
        process.begin(process.create(program.entryPoint()), "OnTick");
        process.step(1_000_000);
        assertEquals(Process.State.FINISHED, process.state(), () -> String.valueOf(process.message()));
        assertEquals(List.of("ore 1 object"), process.console());

        final SigmaCompiler.Result written = SigmaCompiler.compile(List.of(new SourceFile("Says.sgs", prelude
                + "JsonValue doc = Json.Parse(\"{}\"); doc[\"a\"] = doc; } }")));
        assertFalse(written.ok(), "a value of JSON is only read");
    }

    private static Values.Obj parse(final String text) {
        return (Values.Obj) PureFunctions.REGISTRY.find("Json", "Parse", List.of("string")).function()
                .call(HEAP, null, new Object[]{text}, LINE);
    }

    private static Object call(final Object value, final String name, final List<String> parameters,
                               final Object... arguments) {
        return PureFunctions.REGISTRY.find(JsonFunctions.TYPE, name, parameters).function()
                .call(HEAP, value, arguments, LINE);
    }
}
