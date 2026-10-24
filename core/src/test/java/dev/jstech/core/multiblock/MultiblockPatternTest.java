/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiblockPatternTest {

    @Test
    void minimalPattern_singleTerminalCell() {
        var p = MultiblockPattern.builder("min")
                .layer("#")
                .build();
        assertEquals("min", p.name());
        assertEquals(1, p.sizeX());
        assertEquals(1, p.sizeY());
        assertEquals(1, p.sizeZ());
        assertEquals('#', p.charAt(0, 0, 0));
        assertEquals(0, p.terminalX());
        assertEquals(0, p.terminalY());
        assertEquals(0, p.terminalZ());
    }

    @Test
    void cube3x3x3_buildsCorrectly() {
        var p = MultiblockPattern.builder("cube")
                .layer(
                        "CCC",
                        "CCC",
                        "CCC"
                )
                .layer(
                        "CCC",
                        "C#C",
                        "CCC"
                )
                .layer(
                        "CCC",
                        "CCC",
                        "CCC"
                )
                .where('C', IBlockMatcher.exact("jsc:casing"))
                .build();
        assertEquals(3, p.sizeX());
        assertEquals(3, p.sizeY());
        assertEquals(3, p.sizeZ());
        assertEquals('#', p.charAt(1, 1, 1));
        assertEquals('C', p.charAt(0, 0, 0));
        assertEquals(1, p.terminalX());
        assertEquals(1, p.terminalY());
        assertEquals(1, p.terminalZ());
    }

    @Test
    void empty_throws() {
        assertThrows(IllegalStateException.class,
                () -> MultiblockPattern.builder("empty").build());
    }

    @Test
    void noTerminal_throws() {
        // Layer with valid blocks but no '#' anywhere.
        assertThrows(IllegalStateException.class, () ->
                MultiblockPattern.builder("noctrl")
                        .layer("CCC")
                        .where('C', IBlockMatcher.exact("jsc:casing"))
                        .build()
        );
    }

    @Test
    void multipleTerminals_throws() {
        assertThrows(IllegalStateException.class, () ->
                MultiblockPattern.builder("twoctrl")
                        .layer("# #")
                        .build() // implicit: '#' twice, ignore (' ') in middle
        );
    }

    @Test
    void unmappedChar_throws() {
        assertThrows(IllegalStateException.class, () ->
                MultiblockPattern.builder("unmapped")
                        .layer("X#X") // 'X' is never mapped
                        .build()
        );
    }

    @Test
    void inconsistentRowWidth_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                MultiblockPattern.builder("ragged").layer("###", "##")
        );
    }

    @Test
    void inconsistentLayerSize_throws() {
        var b = MultiblockPattern.builder("badlayers")
                .layer("###",  // 3x1
                        "###",
                        "###");
        // Second layer is different shape:
        assertThrows(IllegalArgumentException.class, () ->
                b.layer("##", "##")
        );
    }

    @Test
    void mappingReservedChar_throws() {
        var b = MultiblockPattern.builder("reserved");
        assertThrows(IllegalArgumentException.class,
                () -> b.where('#', IBlockMatcher.exact("jsc:x")));
        assertThrows(IllegalArgumentException.class,
                () -> b.where(' ', IBlockMatcher.exact("jsc:y")));
    }

    @Test
    void emptyLayer_throws() {
        var b = MultiblockPattern.builder("empty_layer");
        assertThrows(IllegalArgumentException.class, () -> b.layer());
    }

    @Test
    void emptyRow_throws() {
        var b = MultiblockPattern.builder("empty_row");
        assertThrows(IllegalArgumentException.class, () -> b.layer(""));
    }

    @Test
    void mapping_isUnmodifiable() {
        var p = MultiblockPattern.builder("imm")
                .layer("C#")
                .where('C', IBlockMatcher.exact("jsc:casing"))
                .build();
        var map = p.mapping();
        assertEquals(1, map.size());
        assertTrue(map.containsKey('C'));
        assertThrows(UnsupportedOperationException.class,
                () -> map.put('X', IBlockMatcher.any()));
    }

    @Test
    void terminalAtEdge_locatesCorrectly() {
        // Terminal in corner of a 2x1x2 pattern (top of an L-shape).
        var p = MultiblockPattern.builder("corner")
                .layer(
                        "#C",
                        "CC"
                )
                .where('C', IBlockMatcher.exact("jsc:casing"))
                .build();
        assertEquals(0, p.terminalX());
        assertEquals(0, p.terminalY());
        assertEquals(0, p.terminalZ());
        assertEquals('#', p.charAt(0, 0, 0));
        assertEquals('C', p.charAt(1, 0, 0));
        assertEquals('C', p.charAt(0, 0, 1));
    }

    @Test
    void port_needsTheLetterToBeFittedToo() {
        var builder = MultiblockPattern.builder("hatch")
                .layer("#H")
                .port('H', PortKind.FLUID_INPUT);
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void port_isKeptByItsLetter() {
        var p = MultiblockPattern.builder("hatch")
                .layer("#H")
                .where('H', BlockMatch.blocks("jsc:hatch"))
                .port('H', PortKind.ENERGY_INPUT)
                .build();
        assertEquals(PortKind.ENERGY_INPUT, p.ports().get('H'));
        assertTrue(p.ports().get('#') == null, "the terminal is never a port");
    }

    @Test
    void rows_giveTheLayerBackAsWritten() {
        var p = MultiblockPattern.builder("two")
                .layer("#C", "CC")
                .where('C', IBlockMatcher.exact("jsc:casing"))
                .build();
        assertEquals("#C", p.rows(0)[0]);
        assertEquals("CC", p.rows(0)[1]);
    }

    @Test
    void limit_needsACountSomePlaceKeeps() {
        var builder = MultiblockPattern.builder("misspelt")
                .layer("#H")
                .where('H', Place.of(BlockMatch.blocks("jsc:hatch")).counting("hatch", BlockMatch.blocks("jsc:hatch")))
                .limit("hatches", 1, 2);
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void valued_refusesOneNumberGatheredTwoWays() {
        var builder = MultiblockPattern.builder("coils")
                .layer("A#B")
                .where('A', Place.of(BlockMatch.blocks("jsc:coil")).valued("heat", Map.of("jsc:coil", 1.0),
                        Place.Gather.LEAST))
                .where('B', Place.of(BlockMatch.blocks("jsc:coil")).valued("heat", Map.of("jsc:coil", 1.0),
                        Place.Gather.SUM));
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void repeat_refusesTheTerminalsSliceAndASecondSlice() {
        assertThrows(IllegalStateException.class, () -> MultiblockPattern.builder("tower")
                .layer("#").layer("C")
                .where('C', BlockMatch.blocks("jsc:casing"))
                .repeatLayer(0, 1, 4)
                .build());
        assertThrows(IllegalStateException.class, () -> MultiblockPattern.builder("tower")
                .layer("#").layer("C")
                .where('C', BlockMatch.blocks("jsc:casing"))
                .repeatLayer(1, 1, 4)
                .repeatLayer(1, 1, 4));
        assertThrows(IllegalArgumentException.class, () -> new MultiblockPattern.Repeat(MultiblockPattern.AXIS_Y, 1,
                new Limit(1, MultiblockPattern.Repeat.MOST_TIMES + 1)));
    }

    @Test
    void inside_refusesALetterThatIsAlsoAPlace() {
        assertThrows(IllegalArgumentException.class, () -> MultiblockPattern.builder("tank")
                .layer("#x")
                .where('x', BlockMatch.blocks("jsc:casing"))
                .inside('x', InsideRule.empty()));
        assertThrows(IllegalArgumentException.class, () -> MultiblockPattern.builder("tank")
                .layer("#x")
                .inside('x', InsideRule.empty())
                .where('x', BlockMatch.blocks("jsc:casing")));
    }

    @Test
    void entry_needsItsPlaceDeclaredFirst() {
        assertThrows(IllegalArgumentException.class, () -> MultiblockPattern.builder("entry")
                .layer("#R")
                .entry('R', "power_entry", BlockMatch.blocks("jsc:power_entry"), PortKind.ENERGY_INPUT));
    }

    @Test
    void entry_widensWhatFitsItsPlaceAndKeepsItsCount() {
        var p = MultiblockPattern.builder("entry")
                .layer("#R")
                .where('R', BlockMatch.blocks("jsc:casing"))
                .entry('R', "power_entry", BlockMatch.blocks("jsc:power_entry"), PortKind.ENERGY_INPUT)
                .limit("power_entry", 1, 1)
                .build();
        var place = p.places().get('R');
        assertEquals(new BlockMatch(List.of("jsc:casing", "jsc:power_entry"), List.of()), place.fits());
        assertEquals("power_entry", place.entries().getFirst().name());
        assertEquals(PortKind.ENERGY_INPUT, place.entries().getFirst().port());
    }

    @Test
    void together_needsTwoCountsSomePlaceKeeps() {
        assertThrows(IllegalArgumentException.class, () -> new MultiblockPattern.Together("inlet", "inlet"));
        assertThrows(IllegalStateException.class, () -> MultiblockPattern.builder("cooled")
                .layer("#R")
                .where('R', BlockMatch.blocks("jsc:casing"))
                .entry('R', "inlet", BlockMatch.blocks("jsc:inlet"), PortKind.FLUID_INPUT)
                .together("inlet", "outlet")
                .build());
    }

    @Test
    void orientations_growWithWhatThePatternDeclares() {
        var plain = MultiblockPattern.builder("plain").layer("#").build();
        var mirrored = MultiblockPattern.builder("mirrored").layer("#").mirrors().build();
        var lying = MultiblockPattern.builder("lying").layer("#").liesDown().build();
        var both = MultiblockPattern.builder("both").layer("#").mirrors().liesDown().build();
        assertEquals(4, plain.orientations().size());
        assertEquals(8, mirrored.orientations().size());
        assertEquals(24, lying.orientations().size());
        assertEquals(48, both.orientations().size());
    }
}
