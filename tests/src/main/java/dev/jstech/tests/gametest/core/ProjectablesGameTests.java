/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.multiblock.BlockMatch;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.core.multiblock.PortKind;
import dev.jstech.core.multiblock.Projectables;
import dev.jstech.core.multiblock.ShapePreview;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestMultiblocks;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Who a multiblock's terminal is, for the views that show it before it is built: the block its pattern names, kept
 * in its file, else the terminal block of a machine declared as data, of its own shape or of a pattern by id. And
 * what else a pattern's file keeps: the entries its places take and the counts that go together.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ProjectablesGameTests {

    private static final String ARENA = "empty";
    private static final String FURNACE_TERMINAL = "jstests:test_furnace_terminal";

    private ProjectablesGameTests() {
    }

    @GameTest(template = ARENA)
    public static void terminalOf_takesThePatternsOwnFirst(final GameTestHelper helper) {
        helper.assertTrue(Projectables.terminalOf(TestMultiblocks.FURNACE_ID).equals(Optional.of(FURNACE_TERMINAL)),
                "the furnace's pattern names its terminal, which a data machine on the same pattern does not"
                        + " replace; got " + Projectables.terminalOf(TestMultiblocks.FURNACE_ID));
        final ShapePreview preview = Projectables.preview(TestMultiblocks.FURNACE_ID, 0).orElseThrow();
        helper.assertTrue(preview.terminalBlock().equals(Optional.of(FURNACE_TERMINAL)),
                "its preview shows the terminal at its place");
        helper.assertTrue(preview.materials().keySet().stream().noneMatch(FURNACE_TERMINAL::equals),
                "the terminal is not among the materials, which are counted apart from it");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void terminalOf_findsTheBlockOfAMachineDeclaredAsData(final GameTestHelper helper) {
        final ResourceLocation baker = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "baker");
        helper.assertTrue(Projectables.terminalOf(baker).equals(Optional.of("jstests:test_baker")),
                "a machine of its own shape has the block built on its file as its terminal; got "
                        + Projectables.terminalOf(baker));
        helper.assertTrue(Projectables.preview(baker, 0).orElseThrow().terminalBlock()
                .equals(Optional.of("jstests:test_baker")), "its preview shows that block at the terminal's place");
        helper.assertTrue(Projectables.terminalOf(ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "nowhere"))
                .isEmpty(), "a block naming a file that is not there is the terminal of nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void patternFile_keepsItsTerminal(final GameTestHelper helper) {
        final JsonElement file = MultiblockPatterns.CODEC.encodeStart(JsonOps.INSTANCE, TestMultiblocks.FURNACE)
                .getOrThrow();
        helper.assertTrue(file.getAsJsonObject().get("terminal").getAsString().equals(FURNACE_TERMINAL),
                "the file names the terminal; got " + file);
        final MultiblockPattern read = MultiblockPatterns.CODEC.parse(JsonOps.INSTANCE, file).getOrThrow();
        helper.assertTrue(read.terminalBlock().equals(Optional.of(FURNACE_TERMINAL)),
                "and reads it back");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void patternFile_keepsItsEntriesAndTheCountsThatGoTogether(final GameTestHelper helper) {
        final MultiblockPattern pattern = MultiblockPattern.builder("cooled")
                .layer("R#R")
                .where('R', BlockMatch.blocks("minecraft:iron_block"))
                .entry('R', "inlet", BlockMatch.blocks("minecraft:blue_wool"), PortKind.FLUID_INPUT)
                .entry('R', "outlet", BlockMatch.blocks("minecraft:red_wool"), PortKind.FLUID_OUTPUT)
                .limit("inlet", 0, 1)
                .together("inlet", "outlet")
                .terminal(FURNACE_TERMINAL)
                .build();
        final JsonElement file = MultiblockPatterns.CODEC.encodeStart(JsonOps.INSTANCE, pattern).getOrThrow();
        helper.assertTrue(file.getAsJsonObject().has("together"), "the file names the pair; got " + file);
        final MultiblockPattern read = MultiblockPatterns.CODEC.parse(JsonOps.INSTANCE, file).getOrThrow();
        helper.assertTrue(read.places().get('R').equals(pattern.places().get('R')),
                "its place reads back with both entries, each with its port; got " + read.places().get('R'));
        helper.assertTrue(read.together().equals(pattern.together()), "and the pair that goes together");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void declare_refusesATerminalThatIsNoId(final GameTestHelper helper) {
        final MultiblockPattern pattern = MultiblockPattern.builder("bad_terminal").layer("#")
                .terminal("Not An Id").build();
        try {
            MultiblockPatterns.declare(ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "bad_terminal"),
                    pattern);
            helper.fail("a terminal named by something that is no id is refused");
        } catch (final IllegalArgumentException refused) {
            helper.assertTrue(MultiblockPatterns.get(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                    "bad_terminal")).isEmpty(), "and the pattern is not declared");
            helper.succeed();
        }
    }
}
