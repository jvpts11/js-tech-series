/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ShapeMatcherTest {

    private static final String CASING = "test:casing";
    private static final String GLASS = "test:glass";
    private static final String HATCH = "test:hatch";
    private static final String POWER_ENTRY = "test:power_entry";
    private static final String INLET = "test:coolant_inlet";
    private static final String OUTLET = "test:coolant_outlet";

    @Test
    void check_countsThePlacesThatHoldACountedBlock() {
        final MultiblockPattern pattern = MultiblockPattern.builder("counted")
                .layer("H#H")
                .where('H', Place.of(BlockMatch.blocks(CASING, HATCH)).counting("hatch", BlockMatch.blocks(HATCH)))
                .build();
        final Map<Long, String> world = new HashMap<>();
        world.put(pos(-1, 0, 0), HATCH);
        world.put(pos(1, 0, 0), CASING);
        final ShapeMatch.Formed formed = assertInstanceOf(ShapeMatch.Formed.class,
                pattern.check(provider(world), pos(0, 0, 0)));
        assertEquals(1, formed.count("hatch"));
        assertEquals(0, formed.count("nothing"));
    }

    @Test
    void check_failsACountOutsideItsLimitAtTheTerminal() {
        final MultiblockPattern pattern = MultiblockPattern.builder("limited")
                .layer("H#H")
                .where('H', Place.of(BlockMatch.blocks(CASING, HATCH)).counting("hatch", BlockMatch.blocks(HATCH)))
                .limit("hatch", 1, 1)
                .build();
        final Map<Long, String> world = new HashMap<>();
        world.put(pos(-1, 0, 0), HATCH);
        world.put(pos(1, 0, 0), HATCH);
        final ShapeMatch.Failure failure = assertInstanceOf(ShapeMatch.Failure.class,
                pattern.check(provider(world), pos(0, 0, 0)));
        assertEquals(ShapeMatch.Wrong.COUNT, failure.wrong());
        assertEquals("hatch", failure.wanted());
        assertEquals("2", failure.found());
        assertEquals(pos(0, 0, 0), failure.at());
    }

    @Test
    void check_gathersACoilsNumberAsItsCoolestCoil() {
        final MultiblockPattern pattern = MultiblockPattern.builder("furnace")
                .layer("C#C")
                .where('C', Place.of(BlockMatch.blocks("test:copper_coil", "test:nichrome_coil"))
                        .valued("temperature", Map.of("test:copper_coil", 1800.0, "test:nichrome_coil", 2700.0),
                                Place.Gather.LEAST))
                .build();
        final Map<Long, String> world = new HashMap<>();
        world.put(pos(-1, 0, 0), "test:nichrome_coil");
        world.put(pos(1, 0, 0), "test:copper_coil");
        final ShapeMatch.Formed formed = assertInstanceOf(ShapeMatch.Formed.class,
                pattern.check(provider(world), pos(0, 0, 0)));
        assertEquals(1800.0, formed.value("temperature", 0), 1e-9);
    }

    @Test
    void check_findsAMirroredShapeOnlyWhenThePatternMirrors() {
        final MultiblockPattern.Builder shape = MultiblockPattern.builder("hook")
                .layer("#CC", "  C")
                .where('C', BlockMatch.blocks(CASING));
        final Map<Long, String> world = new HashMap<>();
        world.put(pos(-1, 0, 0), CASING);
        world.put(pos(-2, 0, 0), CASING);
        world.put(pos(-2, 0, 1), CASING);
        assertInstanceOf(ShapeMatch.Failure.class, shape.build().check(provider(world), pos(0, 0, 0)));
        final ShapeMatch.Formed formed = assertInstanceOf(ShapeMatch.Formed.class,
                shape.mirrors().build().check(provider(world), pos(0, 0, 0)));
        assertFalse(formed.orientation().proper());
    }

    @Test
    void check_findsAShapeLyingOnItsSideOnlyWhenThePatternLiesDown() {
        final MultiblockPattern.Builder tower = MultiblockPattern.builder("tower")
                .layer("#")
                .layer("C")
                .layer("G")
                .where('C', BlockMatch.blocks(CASING))
                .where('G', BlockMatch.blocks(GLASS));
        final Map<Long, String> world = new HashMap<>();
        world.put(pos(1, 0, 0), CASING);
        world.put(pos(2, 0, 0), GLASS);
        assertInstanceOf(ShapeMatch.Failure.class, tower.build().check(provider(world), pos(0, 0, 0)));
        final ShapeMatch.Formed formed = assertInstanceOf(ShapeMatch.Formed.class,
                tower.liesDown().build().check(provider(world), pos(0, 0, 0)));
        assertFalse(formed.orientation().upright());
        assertEquals(3, formed.sizeX());
        assertEquals(1, formed.sizeY());
    }

    @Test
    void check_growsARepeatedLayerAsHighAsTheWorldHoldsIt() {
        final MultiblockPattern pattern = MultiblockPattern.builder("column")
                .layer("#")
                .layer("G")
                .layer("C")
                .where('G', BlockMatch.blocks(GLASS))
                .where('C', BlockMatch.blocks(CASING))
                .repeatLayer(1, 1, 8)
                .build();
        final Map<Long, String> world = new HashMap<>();
        for (int y = 1; y <= 5; y++) {
            world.put(pos(0, y, 0), GLASS);
        }
        world.put(pos(0, 6, 0), CASING);
        final ShapeMatch.Formed formed = assertInstanceOf(ShapeMatch.Formed.class,
                pattern.check(provider(world), pos(0, 0, 0)));
        assertEquals(5, formed.repeats());
        assertEquals(7, formed.sizeY());
        assertEquals(6, formed.parts().size());
    }

    @Test
    void check_growsARepeatedRowTowardsATerminalStandingAfterIt() {
        final MultiblockPattern pattern = MultiblockPattern.builder("battery")
                .layer("C", "O", "#")
                .where('C', BlockMatch.blocks(CASING))
                .where('O', BlockMatch.blocks("test:oven"))
                .repeatRow(1, 2, 6)
                .build();
        final Map<Long, String> world = new HashMap<>();
        world.put(pos(0, 0, -1), "test:oven");
        world.put(pos(0, 0, -2), "test:oven");
        world.put(pos(0, 0, -3), "test:oven");
        world.put(pos(0, 0, -4), CASING);
        final ShapeMatch.Formed formed = assertInstanceOf(ShapeMatch.Formed.class,
                pattern.check(provider(world), pos(0, 0, 0)));
        assertEquals(3, formed.repeats());
    }

    @Test
    void check_failsARepeatedSliceThereFewerThanItsLeastTimes() {
        final MultiblockPattern pattern = MultiblockPattern.builder("column")
                .layer("#")
                .layer("G")
                .layer("C")
                .where('G', BlockMatch.blocks(GLASS))
                .where('C', BlockMatch.blocks(CASING))
                .repeatLayer(1, 3, 8)
                .build();
        final Map<Long, String> world = new HashMap<>();
        world.put(pos(0, 1, 0), GLASS);
        world.put(pos(0, 2, 0), CASING);
        final ShapeMatch.Failure failure = assertInstanceOf(ShapeMatch.Failure.class,
                pattern.check(provider(world), pos(0, 0, 0)));
        assertEquals(ShapeMatch.Wrong.BLOCK, failure.wrong());
        assertEquals("G", failure.wanted());
    }

    @Test
    void check_readsTheInsideLetterByItsRule() {
        final MultiblockPattern pattern = MultiblockPattern.builder("tank")
                .layer("CCC", "C#C", "CCC")
                .layer("CCC", "CxC", "CCC")
                .layer("CCC", "CCC", "CCC")
                .where('C', BlockMatch.blocks(CASING))
                .inside('x', InsideRule.empty().with(InsidePart.of("rod", BlockMatch.blocks("test:rod"))
                        .gives("heat", 10)))
                .build();
        final Map<Long, String> world = new HashMap<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    world.put(pos(x, y, z), CASING);
                }
            }
        }
        world.put(pos(0, 1, 0), "test:rod");
        final ShapeMatch.Formed formed = assertInstanceOf(ShapeMatch.Formed.class,
                pattern.check(provider(world), pos(0, 0, 0)));
        assertEquals(1, formed.inside().size());
        assertEquals(1, formed.count("rod"));
        assertEquals(10.0, formed.value("heat", 0), 1e-9);
        world.put(pos(0, 1, 0), "minecraft:dirt");
        final ShapeMatch.Failure failure = assertInstanceOf(ShapeMatch.Failure.class,
                pattern.check(provider(world), pos(0, 0, 0)));
        assertEquals(ShapeMatch.INSIDE, failure.wanted());
    }

    @Test
    void check_namesTheWrongPlaceOfTheWayThePlayerMostLikelyBuiltIt() {
        final MultiblockPattern pattern = MultiblockPattern.builder("line")
                .layer("#CCG")
                .where('C', BlockMatch.blocks(CASING))
                .where('G', BlockMatch.blocks(GLASS))
                .build();
        final Map<Long, String> world = new HashMap<>();
        world.put(pos(0, 0, 1), CASING);
        world.put(pos(0, 0, 2), CASING);
        world.put(pos(0, 0, 3), "minecraft:dirt");
        final ShapeMatch.Failure failure = assertInstanceOf(ShapeMatch.Failure.class,
                pattern.check(provider(world), pos(0, 0, 0)));
        assertEquals(pos(0, 0, 3), failure.at());
        assertEquals("G", failure.wanted());
        assertEquals("minecraft:dirt", failure.found());
        assertEquals(3, failure.relZ());
    }

    @Test
    void check_reportsThePortsAndTheSizeOfAFormedPattern() {
        final MultiblockPattern pattern = MultiblockPattern.builder("furnace")
                .layer("I#O", "CCC")
                .where('I', BlockMatch.blocks(HATCH))
                .where('O', BlockMatch.blocks(HATCH))
                .where('C', BlockMatch.blocks(CASING))
                .port('I', PortKind.ITEM_INPUT)
                .port('O', PortKind.ITEM_OUTPUT)
                .build();
        final Map<Long, String> world = new HashMap<>();
        world.put(pos(-1, 0, 0), HATCH);
        world.put(pos(1, 0, 0), HATCH);
        for (int x = -1; x <= 1; x++) {
            world.put(pos(x, 0, 1), CASING);
        }
        final ShapeMatch.Formed formed = assertInstanceOf(ShapeMatch.Formed.class,
                pattern.check(provider(world), pos(0, 0, 0)));
        assertEquals(Orientation.NORTH, formed.orientation());
        assertEquals(PortKind.ITEM_INPUT, formed.ports().get(pos(-1, 0, 0)));
        assertEquals(3, formed.sizeX());
        assertEquals(2, formed.sizeZ());
        assertEquals(6, formed.blocks());
    }

    @Test
    void check_countsAnEntryAndMakesItsPlaceAPortOfItsKind() {
        final MultiblockPattern pattern = MultiblockPattern.builder("entries")
                .layer("R#R")
                .where('R', BlockMatch.blocks(CASING))
                .entry('R', "power_entry", BlockMatch.blocks(POWER_ENTRY), PortKind.ENERGY_INPUT)
                .limit("power_entry", 1, 1)
                .build();
        final Map<Long, String> world = new HashMap<>();
        world.put(pos(-1, 0, 0), POWER_ENTRY);
        world.put(pos(1, 0, 0), CASING);
        final ShapeMatch.Formed formed = assertInstanceOf(ShapeMatch.Formed.class,
                pattern.check(provider(world), pos(0, 0, 0)));
        assertEquals(1, formed.count("power_entry"));
        assertEquals(PortKind.ENERGY_INPUT, formed.ports().get(pos(-1, 0, 0)));
        assertNull(formed.ports().get(pos(1, 0, 0)));
        world.put(pos(1, 0, 0), POWER_ENTRY);
        final ShapeMatch.Failure failure = assertInstanceOf(ShapeMatch.Failure.class,
                pattern.check(provider(world), pos(0, 0, 0)));
        assertEquals(ShapeMatch.Wrong.COUNT, failure.wrong());
        assertEquals("power_entry", failure.wanted());
        assertEquals("2", failure.found());
    }

    @Test
    void check_refusesAnEntryWhereItsMultiblockTakesNone() {
        final MultiblockPattern pattern = MultiblockPattern.builder("rear")
                .layer("R#R", "CCC")
                .where('R', BlockMatch.blocks(CASING))
                .where('C', BlockMatch.blocks(CASING))
                .entry('R', "power_entry", BlockMatch.blocks(POWER_ENTRY), PortKind.ENERGY_INPUT)
                .build();
        final Map<Long, String> world = new HashMap<>();
        for (int x = -1; x <= 1; x++) {
            world.put(pos(x, 0, 0), CASING);
            world.put(pos(x, 0, 1), CASING);
        }
        world.remove(pos(0, 0, 0));
        world.put(pos(0, 0, 1), POWER_ENTRY);
        final ShapeMatch.Failure failure = assertInstanceOf(ShapeMatch.Failure.class,
                pattern.check(provider(world), pos(0, 0, 0)));
        assertEquals(ShapeMatch.Wrong.BLOCK, failure.wrong());
        assertEquals("C", failure.wanted());
        assertEquals(POWER_ENTRY, failure.found());
    }

    @Test
    void check_formsCountsThatGoTogetherOnlyWhenTheyAgree() {
        final MultiblockPattern pattern = MultiblockPattern.builder("cooled")
                .layer("R#R")
                .where('R', BlockMatch.blocks(CASING))
                .entry('R', "inlet", BlockMatch.blocks(INLET), PortKind.FLUID_INPUT)
                .entry('R', "outlet", BlockMatch.blocks(OUTLET), PortKind.FLUID_OUTPUT)
                .together("inlet", "outlet")
                .build();
        final Map<Long, String> world = new HashMap<>();
        world.put(pos(-1, 0, 0), INLET);
        world.put(pos(1, 0, 0), CASING);
        final ShapeMatch.Failure failure = assertInstanceOf(ShapeMatch.Failure.class,
                pattern.check(provider(world), pos(0, 0, 0)));
        assertEquals(ShapeMatch.Wrong.APART, failure.wrong());
        assertEquals("inlet", failure.wanted());
        assertEquals("outlet", failure.found());
        world.put(pos(1, 0, 0), OUTLET);
        final ShapeMatch.Formed formed = assertInstanceOf(ShapeMatch.Formed.class,
                pattern.check(provider(world), pos(0, 0, 0)));
        assertEquals(1, formed.count("inlet"));
        assertEquals(1, formed.count("outlet"));
        assertEquals(PortKind.FLUID_INPUT, formed.ports().get(pos(-1, 0, 0)));
        assertEquals(PortKind.FLUID_OUTPUT, formed.ports().get(pos(1, 0, 0)));
        world.put(pos(-1, 0, 0), CASING);
        world.put(pos(1, 0, 0), CASING);
        assertInstanceOf(ShapeMatch.Formed.class, pattern.check(provider(world), pos(0, 0, 0)));
    }

    private static IBlockProvider provider(final Map<Long, String> world) {
        return pos -> world.getOrDefault(pos, "minecraft:air");
    }

    private static long pos(final int x, final int y, final int z) {
        return PatternMatcher.encodePosition(x, y, z);
    }
}
