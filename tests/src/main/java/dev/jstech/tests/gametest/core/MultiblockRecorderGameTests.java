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
import dev.jstech.core.multiblock.IMatchResult;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.core.multiblock.MultiblockRecorder;
import dev.jstech.core.multiblock.PortKind;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestStructures;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Reading a multiblock's pattern off one built: each kind of block its letter, the first of its name, the ports their
 * kind under I and O, the controller at {@code #} and air left free; the pattern read so knows the structure it was
 * read from, and its file reads back the same.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MultiblockRecorderGameTests {

    private static final String ARENA = "empty";
    /* The furnace's corners, its controller amid its north face, and its ports on its west and east faces. */
    private static final BlockPos FROM = new BlockPos(2, 2, 2);
    private static final BlockPos TO = new BlockPos(4, 3, 4);
    private static final BlockPos CONTROLLER = new BlockPos(3, 2, 2);
    private static final BlockPos IN = new BlockPos(2, 2, 3);
    private static final BlockPos OUT = new BlockPos(4, 2, 3);

    private MultiblockRecorderGameTests() {
    }

    @GameTest(template = ARENA)
    public static void record_givesEachKindOfBlockItsLetterAndKnowsTheStructureItWasReadFrom(
            final GameTestHelper helper) {
        build(helper);
        final MultiblockRecorder.Recording recorded = MultiblockRecorder.record(helper.getLevel(),
                helper.absolutePos(FROM), helper.absolutePos(TO), helper.absolutePos(CONTROLLER),
                "jstests:recorded_furnace", false, false);
        final MultiblockPattern pattern = recorded.pattern();
        helper.assertTrue(recorded.sizeX() == 3 && recorded.sizeY() == 2 && recorded.sizeZ() == 3,
                "it spans the corners: " + recorded);
        helper.assertTrue(pattern.rows(0)[0].equals("B#B"), "the controller stands at # amid the north face: "
                + String.join("/", pattern.rows(0)));
        helper.assertTrue(pattern.rows(0)[1].equals("I O"), "the ports are I and O, and air is left free: "
                + String.join("/", pattern.rows(0)));
        final Optional<MultiblockRecorder.Letter> bricks = recorded.letters().stream()
                .filter(letter -> letter.block().equals("minecraft:bricks")).findFirst();
        helper.assertTrue(bricks.isPresent() && bricks.get().letter() == 'B' && bricks.get().count() == 14,
                "the bricks are B, fourteen of them: " + recorded.letters());
        helper.assertTrue(pattern.ports().get('I') == PortKind.ITEM_INPUT
                && pattern.ports().get('O') == PortKind.ITEM_OUTPUT, "the ports keep their kind");
        helper.assertTrue(MultiblockPatterns.match(pattern, helper.getLevel(), helper.absolutePos(CONTROLLER))
                instanceof IMatchResult.Success, "the pattern knows the structure it was read from");
        final JsonElement file = MultiblockPatterns.CODEC.encodeStart(JsonOps.INSTANCE, pattern).getOrThrow();
        final MultiblockPattern read = MultiblockPatterns.CODEC.parse(JsonOps.INSTANCE, file).getOrThrow();
        helper.assertTrue(String.join("/", read.rows(1)).equals(String.join("/", pattern.rows(1)))
                && read.controllerBlock().equals(pattern.controllerBlock()), "its file reads back the same");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void record_refusesAControllerOutsideTheCorners(final GameTestHelper helper) {
        build(helper);
        boolean refused = false;
        try {
            MultiblockRecorder.record(helper.getLevel(), helper.absolutePos(FROM), helper.absolutePos(TO),
                    helper.absolutePos(new BlockPos(7, 2, 7)), "jstests:outside", false, false);
        } catch (final IllegalArgumentException outside) {
            refused = true;
        }
        helper.assertTrue(refused, "a controller outside the corners is refused");
        helper.succeed();
    }

    /* A furnace of bricks two high: a ring below with its controller and two ports, a full floor of bricks above. */
    private static void build(final GameTestHelper helper) {
        for (final BlockPos at : BlockPos.betweenClosed(FROM, TO)) {
            helper.setBlock(at, Blocks.BRICKS);
        }
        helper.setBlock(new BlockPos(3, 2, 3), Blocks.AIR);
        helper.setBlock(CONTROLLER, Blocks.BLAST_FURNACE);
        helper.setBlock(IN, TestStructures.ITEM_INPUT.get());
        helper.setBlock(OUT, TestStructures.ITEM_OUTPUT.get());
    }
}
