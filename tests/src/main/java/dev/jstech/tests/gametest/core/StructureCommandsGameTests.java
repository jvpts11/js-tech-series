/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.moving.physics.PhysicsMotion;
import dev.jstech.tests.JsTests;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The commands that run moving structures: {@code freeze} holds one in the air until {@code unfreeze} lets it fall,
 * {@code remove} takes one away, and {@code export} saves one's blocks as a structure file the game places again.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class StructureCommandsGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jscore_structure_commands";
    /* How close, in blocks, one held still is to where it was held. */
    private static final double HELD = 0.01;

    private StructureCommandsGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void freeze_holdsItInTheAirUntilUnfreezeLetsItFall(final GameTestHelper helper) {
        final MovingStructure block = made(helper, new BlockPos(4, 5, 4));
        block.move(PhysicsMotion.AT_REST);
        final double[] held = new double[1];
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    run(helper, "jstech structure freeze " + block.entity().getStringUUID());
                    held[0] = block.worldBox().minY;
                })
                .thenExecuteAfter(20, () -> helper.assertTrue(Math.abs(block.worldBox().minY - held[0]) < HELD,
                        "frozen, it hangs where it was: " + block.worldBox()))
                .thenExecute(() -> run(helper, "jstech structure unfreeze " + block.entity().getStringUUID()))
                .thenExecuteAfter(10, () -> helper.assertTrue(block.worldBox().minY < held[0] - 0.5,
                        "let go, it falls: " + block.worldBox()))
                .thenExecute(() -> MovingStructures.takeAway(block))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void remove_takesTheStructureAway(final GameTestHelper helper) {
        final MovingStructure block = made(helper, new BlockPos(4, 3, 4));
        run(helper, "jstech structure remove " + block.entity().getStringUUID());
        helper.assertTrue(block.entity().isRemoved(), "it is gone");
        helper.assertTrue(MovingStructures.byId(helper.getLevel(), block.id()) == null, "and known no more");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void export_savesItsBlocksAsAStructureFileTheGamePlacesAgain(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(3, 3, 3), Blocks.GOLD_BLOCK);
        helper.setBlock(new BlockPos(4, 3, 3), Blocks.STONE);
        warm(level);
        final StructureResult made = MovingStructures.assemble(level, helper.absolutePos(new BlockPos(3, 3, 3)),
                helper.absolutePos(new BlockPos(4, 3, 3)));
        helper.assertTrue(made.ok(), "a structure is made");
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "exported_"
                + made.structure().id().asString());
        run(helper, "jstech structure export " + made.structure().entity().getStringUUID() + " " + id);
        final Optional<StructureTemplate> saved = level.getStructureManager().get(id);
        helper.assertTrue(saved.isPresent() && saved.get().getSize().getX() == 2, "the file is written, two"
                + " blocks long");
        final BlockPos at = helper.absolutePos(new BlockPos(2, 4, 6));
        saved.get().placeInWorld(level, at, at, new StructurePlaceSettings(), level.getRandom(), 2);
        helper.assertTrue(level.getBlockState(at).is(Blocks.GOLD_BLOCK) && level.getBlockState(at.east())
                .is(Blocks.STONE), "and placed, its blocks are there again");
        MovingStructures.takeAway(made.structure());
        helper.succeed();
    }

    /* Runs {@code command} as the server does, quietly. */
    private static void run(final GameTestHelper helper, final String command) {
        final MinecraftServer server = helper.getLevel().getServer();
        final CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
        server.getCommands().performPrefixedCommand(source, command);
    }

    /* A structure of one stone at {@code at}. */
    private static MovingStructure made(final GameTestHelper helper, final BlockPos at) {
        helper.setBlock(at, Blocks.STONE);
        warm(helper.getLevel());
        final StructureResult made = MovingStructures.assemble(helper.getLevel(), helper.absolutePos(at),
                helper.absolutePos(at));
        helper.assertTrue(made.ok() && made.structure() != null, "a structure is made");
        return made.structure();
    }

    /* Loads the chunks round the yard's next plot, so a structure made comes into it at once. */
    private static void warm(final ServerLevel level) {
        final int plot = StructureYard.nextPlot(level);
        final int cx = YardLayout.centreX(plot) >> 4;
        final int cz = YardLayout.centreZ(plot) >> 4;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.getChunk(cx + dx, cz + dz);
            }
        }
    }
}
