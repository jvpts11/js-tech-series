/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InsideRuleTest {

    private static final String CELL = "test:cell";
    private static final String COOLER = "test:cooler";
    private static final String MODERATOR = "test:moderator";
    private static final String CASING = "test:casing";

    @Test
    void read_givesWhatEachWorkingPartGives() {
        final InsideRule rule = InsideRule.empty().with(InsidePart.of("cell", BlockMatch.blocks(CELL))
                .gives("heat", 10));
        final Map<Long, String> world = new HashMap<>();
        world.put(pos(0, 0, 0), CELL);
        world.put(pos(1, 0, 0), CELL);
        final ShapeTally tally = new ShapeTally();
        assertNull(rule.read(provider(world), List.of(pos(0, 0, 0), pos(1, 0, 0), pos(2, 0, 0)), pos(9, 9, 9),
                tally));
        assertEquals(2, tally.counts().get("cell"));
        assertEquals(20.0, tally.values().get("heat"), 1e-9);
        assertEquals(3, tally.matched());
    }

    @Test
    void read_givesMoreForEachNeighbourOfAKind() {
        final InsideRule rule = InsideRule.empty()
                .with(InsidePart.of("cell", BlockMatch.blocks(CELL)).gives("heat", 10)
                        .bonus("heat", InsidePart.Neighbour.block(BlockMatch.blocks(MODERATOR)), 5))
                .with(InsidePart.of("moderator", BlockMatch.blocks(MODERATOR)));
        final Map<Long, String> world = new HashMap<>();
        world.put(pos(0, 0, 0), CELL);
        world.put(pos(1, 0, 0), MODERATOR);
        world.put(pos(-1, 0, 0), MODERATOR);
        final ShapeTally tally = new ShapeTally();
        rule.read(provider(world), List.of(pos(-1, 0, 0), pos(0, 0, 0), pos(1, 0, 0)), pos(9, 9, 9), tally);
        assertEquals(20.0, tally.values().get("heat"), 1e-9);
        assertEquals(2, tally.counts().get("moderator"));
    }

    @Test
    void read_letsAPartNeedingAWorkingPartWorkOnlyBesideOne() {
        final InsideRule rule = InsideRule.empty()
                .with(InsidePart.of("cell", BlockMatch.blocks(CELL)))
                .with(InsidePart.of("cooler", BlockMatch.blocks(COOLER)).gives("cooling", 4)
                        .needs(InsidePart.Neighbour.part("cell"), 1));
        final Map<Long, String> world = new HashMap<>();
        world.put(pos(0, 0, 0), CELL);
        world.put(pos(1, 0, 0), COOLER);
        world.put(pos(5, 0, 0), COOLER);
        final ShapeTally tally = new ShapeTally();
        rule.read(provider(world), List.of(pos(0, 0, 0), pos(1, 0, 0), pos(5, 0, 0)), pos(9, 9, 9), tally);
        assertEquals(1, tally.counts().get("cooler"));
        assertEquals(4.0, tally.values().get("cooling"), 1e-9);
    }

    @Test
    void read_settlesAChainOfNeedsFromThePartsThatNeedNothing() {
        final InsideRule rule = InsideRule.empty()
                .with(InsidePart.of("cell", BlockMatch.blocks(CELL)))
                .with(InsidePart.of("cooler", BlockMatch.blocks(COOLER))
                        .needs(InsidePart.Neighbour.part("cell"), 1))
                .with(InsidePart.of("relay", BlockMatch.blocks("test:relay"))
                        .needs(InsidePart.Neighbour.part("cooler"), 1));
        final Map<Long, String> world = new HashMap<>();
        world.put(pos(0, 0, 0), "test:relay");
        world.put(pos(1, 0, 0), COOLER);
        world.put(pos(2, 0, 0), CELL);
        final ShapeTally tally = new ShapeTally();
        rule.read(provider(world), List.of(pos(0, 0, 0), pos(1, 0, 0), pos(2, 0, 0)), pos(9, 9, 9), tally);
        assertEquals(1, tally.counts().get("relay"));
        assertEquals(1, tally.counts().get("cooler"));
    }

    @Test
    void read_neverWorksTwoPartsThatOnlyNeedEachOther() {
        final InsideRule rule = InsideRule.empty()
                .with(InsidePart.of("a", BlockMatch.blocks("test:a")).needs(InsidePart.Neighbour.part("b"), 1))
                .with(InsidePart.of("b", BlockMatch.blocks("test:b")).needs(InsidePart.Neighbour.part("a"), 1));
        final Map<Long, String> world = new HashMap<>();
        world.put(pos(0, 0, 0), "test:a");
        world.put(pos(1, 0, 0), "test:b");
        final ShapeTally tally = new ShapeTally();
        rule.read(provider(world), List.of(pos(0, 0, 0), pos(1, 0, 0)), pos(9, 9, 9), tally);
        assertEquals(Map.of(), tally.counts());
    }

    @Test
    void read_countsAShellBlockBesideAPartAsANeighbour() {
        final InsideRule rule = InsideRule.empty().with(InsidePart.of("vent", BlockMatch.blocks("test:vent"))
                .needs(InsidePart.Neighbour.block(BlockMatch.blocks(CASING)), 1));
        final Map<Long, String> world = new HashMap<>();
        world.put(pos(0, 0, 0), "test:vent");
        world.put(pos(0, -1, 0), CASING);
        final ShapeTally tally = new ShapeTally();
        rule.read(provider(world), List.of(pos(0, 0, 0)), pos(9, 9, 9), tally);
        assertEquals(1, tally.counts().get("vent"));
    }

    @Test
    void read_failsAtTheFirstPlaceHoldingABlockNotAllowed() {
        final InsideRule rule = InsideRule.empty();
        final Map<Long, String> world = new HashMap<>();
        world.put(pos(1, 0, 0), "minecraft:dirt");
        final ShapeMatch.Failure failure = rule.read(provider(world), List.of(pos(0, 0, 0), pos(1, 0, 0)),
                pos(0, 1, 0), new ShapeTally());
        assertNotNull(failure);
        assertEquals(pos(1, 0, 0), failure.at());
        assertEquals(ShapeMatch.INSIDE, failure.wanted());
        assertEquals(-1, failure.relY());
    }

    @Test
    void constructor_refusesTwoPartsOfOneName() {
        assertThrows(IllegalArgumentException.class, () -> InsideRule.empty()
                .with(InsidePart.of("cell", BlockMatch.blocks(CELL)))
                .with(InsidePart.of("cell", BlockMatch.blocks(COOLER))));
        assertThrows(IllegalArgumentException.class, () -> new InsidePart.Need(InsidePart.Neighbour.part("x"), 7));
    }

    @Test
    void requireKnownParts_refusesANeedOfAPartTheRuleDoesNotHave() {
        final InsideRule misspelt = InsideRule.empty()
                .with(InsidePart.of("cooler", BlockMatch.blocks(COOLER)).needs(InsidePart.Neighbour.part("cel"), 1))
                .with(InsidePart.of("cell", BlockMatch.blocks(CELL)));
        assertThrows(IllegalArgumentException.class, misspelt::requireKnownParts);
        final InsideRule later = InsideRule.empty()
                .with(InsidePart.of("cooler", BlockMatch.blocks(COOLER)).needs(InsidePart.Neighbour.part("cell"), 1))
                .with(InsidePart.of("cell", BlockMatch.blocks(CELL)));
        assertEquals(later, later.requireKnownParts());
    }

    private static IBlockProvider provider(final Map<Long, String> world) {
        return pos -> world.getOrDefault(pos, "minecraft:air");
    }

    private static long pos(final int x, final int y, final int z) {
        return PatternMatcher.encodePosition(x, y, z);
    }
}
