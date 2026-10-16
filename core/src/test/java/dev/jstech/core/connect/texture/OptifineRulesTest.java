/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.connect.texture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OptifineRulesTest {

    private static final String GLASS = "minecraft:optifine/ctm/glass/glass.properties";

    @Test
    void parse_readsAFullConnectedGlass() {
        final OptifineRule rule = OptifineRules.parse(GLASS, "method=ctm\ntiles=0-46\nmatchBlocks=glass\n");
        assertEquals(OptifineRule.Method.CTM, rule.method());
        assertEquals(47, rule.tiles().size());
        assertEquals("minecraft:optifine/ctm/glass/12.png", OptifineRules.tileFile(GLASS, rule.tiles().get(12)));
        assertEquals("minecraft:glass", rule.matchBlocks().getFirst().id());
        assertEquals(OptifineRule.Connect.BLOCK, rule.connect(), "a rule by blocks joins by block");
        assertEquals(OptifineRule.ALL_FACES, rule.faces());
    }

    @Test
    void parse_takesTheTexturesFromTheFileNameWhenNoneAreNamed() {
        final OptifineRule rule = OptifineRules.parse(GLASS, "method=ctm_compact\ntiles=0 1 2 3 4\n");
        assertEquals(List.of("minecraft:block/glass"), rule.matchTiles());
        assertEquals(OptifineRule.Connect.TILE, rule.connect(), "a rule by textures joins by texture");
        final OptifineRule byBlock = OptifineRules.parse("minecraft:optifine/ctm/x/block_stone.properties",
                "method=fixed\ntiles=stone_alt\n");
        assertEquals("minecraft:stone", byBlock.matchBlocks().getFirst().id());
    }

    @Test
    void parse_readsTheOldNamesOfMethods() {
        assertEquals(OptifineRule.Method.HORIZONTAL, OptifineRules.parse(GLASS,
                "method=bookshelf\ntiles=0-3\n").method());
        assertEquals(OptifineRule.Method.TOP, OptifineRules.parse(GLASS, "method=sandstone\ntiles=0\n").method());
        assertEquals(OptifineRule.Method.CTM, OptifineRules.parse(GLASS, "tiles=0-46\n").method(),
                "no method is the full one");
    }

    @Test
    void parse_readsEveryKindOfTile() {
        final OptifineRule rule = OptifineRules.parse(GLASS,
                "method=random\ntiles=1-2 moss <skip> <default> textures/block/stone.png botania:block/x.png\n");
        final List<OptifineRule.Tile> tiles = rule.tiles();
        assertEquals(7, tiles.size(), "a range of two, a name, nothing, the face's own and two paths");
        assertEquals("minecraft:optifine/ctm/glass/moss.png", OptifineRules.tileFile(GLASS, tiles.get(2)));
        assertEquals(OptifineRule.TileKind.SKIP, tiles.get(3).kind());
        assertEquals(OptifineRule.TileKind.DEFAULT, tiles.get(4).kind());
        assertEquals("minecraft:textures/block/stone.png", OptifineRules.tileFile(GLASS, tiles.get(5)));
        assertEquals("botania:block/x.png", OptifineRules.tileFile(GLASS, tiles.get(6)));
    }

    @Test
    void spriteOf_isATexturesOwnIdOrItsPath() {
        assertEquals("minecraft:block/stone", OptifineRules.spriteOf("minecraft:textures/block/stone.png"));
        assertEquals("minecraft:optifine/ctm/glass/3", OptifineRules.spriteOf("minecraft:optifine/ctm/glass/3.png"));
    }

    @Test
    void blockFilter_readsPropertiesAndTags() {
        final OptifineRule.BlockFilter log = OptifineRules.blockFilter("minecraft:oak_log:axis=x,z");
        assertEquals("minecraft:oak_log", log.id());
        assertEquals(Map.of("axis", List.of("x", "z")), log.properties());
        assertEquals("minecraft:oak_log", OptifineRules.blockFilter("oak_log:axis=y").id(), "no namespace");
        final OptifineRule.BlockFilter tag = OptifineRules.blockFilter("#c:glass_blocks");
        assertTrue(tag.tag());
        assertEquals("c:glass_blocks", tag.id());
    }

    @Test
    void tileId_readsATextureAsPacksNameIt() {
        assertEquals("minecraft:block/glass", OptifineRules.tileId("glass"));
        assertEquals("botania:block/blazeblock", OptifineRules.tileId("botania:blazeblock"));
        assertEquals("minecraft:block/glass", OptifineRules.tileId("textures/block/glass.png"));
        assertEquals("minecraft:item/apple", OptifineRules.tileId("item/apple"));
    }

    @Test
    void parse_readsWhereARuleIsDrawn() {
        final OptifineRule rule = OptifineRules.parse(GLASS, "method=fixed\ntiles=0\nfaces=top sides\n"
                + "biomes=!desert\nheights=(-64)-0 100\nweight=5\ninnerSeams=true\n");
        assertTrue(rule.onFace(1), "the top");
        assertTrue(rule.onFace(4), "a side");
        assertFalse(rule.onFace(0), "not the bottom");
        assertTrue(rule.where().atHeight(-30));
        assertTrue(rule.where().atHeight(100));
        assertFalse(rule.where().atHeight(50));
        assertFalse(rule.where().inBiome("minecraft:desert"));
        assertTrue(rule.where().inBiome("minecraft:plains"));
        assertEquals(5, rule.weight());
        assertTrue(rule.innerSeams());
        assertTrue(OptifineRules.parse(GLASS, "method=fixed\ntiles=0\nminHeight=10\n").where().atHeight(10));
    }

    @Test
    void parse_readsHowRandomAndRepeatingTilesPick() {
        final OptifineRule random = OptifineRules.parse(GLASS,
                "method=random\ntiles=0-2\nweights=1 2 7\nrandomLoops=12\nsymmetry=opposite\nlinked=true\n");
        assertEquals(List.of(1, 2, 7), random.random().weights());
        assertEquals(9, random.random().loops(), "at most nine");
        assertEquals(TilePicks.OPPOSITE_FACES, random.random().symmetry());
        assertTrue(random.random().linked());
        final OptifineRule repeat = OptifineRules.parse(GLASS, "method=repeat\nwidth=2\nheight=3\ntiles=0-5\n");
        assertEquals(2, repeat.random().width());
        assertEquals(3, repeat.random().height());
    }

    @Test
    void parse_readsTheCompactOverridesAndTheOverlay() {
        final OptifineRule compact = OptifineRules.parse(GLASS, "method=ctm_compact\ntiles=0-5\nctm.26=5\n");
        assertEquals(Map.of(26, 5), compact.overrides());
        final OptifineRule overlay = OptifineRules.parse(GLASS, "method=overlay\ntiles=0-16\nconnectBlocks=grass_block"
                + "\ntintIndex=0\ntintBlock=minecraft:grass_block\nlayer=translucent\n");
        assertTrue(overlay.method().overlays());
        assertEquals("minecraft:grass_block", overlay.connectTo().getFirst().id());
        assertEquals(0, overlay.overlay().tintIndex());
        assertEquals("translucent", overlay.overlay().layer());
    }

    @Test
    void parse_refusesWhatDoesNotRead() {
        assertThrows(IllegalArgumentException.class, () -> OptifineRules.parse(GLASS, "method=ctm\ntiles=0-3\n"),
                "too few tiles for the full method");
        assertThrows(IllegalArgumentException.class, () -> OptifineRules.parse(GLASS, "method=spiral\ntiles=0\n"));
        assertThrows(IllegalArgumentException.class, () -> OptifineRules.parse(GLASS, "method=ctm\n"),
                "no tiles");
        assertThrows(IllegalArgumentException.class, () -> OptifineRules.parse(GLASS,
                "method=repeat\nwidth=2\nheight=2\ntiles=0-2\n"), "a repeat of two by two takes four");
        assertThrows(IllegalArgumentException.class, () -> OptifineRules.parse(GLASS,
                "method=fixed\ntiles=0\nfaces=inside\n"));
    }
}
