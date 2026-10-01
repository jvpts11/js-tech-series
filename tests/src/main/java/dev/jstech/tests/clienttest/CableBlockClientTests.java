/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableEntry;
import dev.jstech.core.cable.Cables;
import dev.jstech.core.client.model.CableBakedModel;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The shared cable block as a player sees it, to set beside the approved page of the cables: the four data lines in
 * one run, each in its lane, the access line turning off through a junction box, plugs where the run meets a router,
 * and a red and a blue backbone side by side with an uncoloured one joining both, each ring on its block.
 */
public final class CableBlockClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, -1);
    private static final int ROW = 2;
    private static final int FLOOR = 2;
    /** Where the access line leaves the run, and the block that becomes a junction box. */
    private static final BlockPos TURN = new BlockPos(0, FLOOR, ROW);
    private static final List<CableEntry> FOUR = List.of(ComputingModule.ETHERNET_CABLE, ComputingModule.HBW_CABLE,
            ComputingModule.HPC_CABLE, ComputingModule.CRAFTING_CABLE);
    private static final float LOOK_DOWN = 35.0F;

    private CableBlockClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void sharedBlock_drawsTheLinesInTheirLanesAndTheJunctionBox(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    for (int x = -2; x <= 2; x++) {
                        final BlockPos at = ctx.abs(new BlockPos(x, FLOOR, ROW));
                        FOUR.forEach(cable -> Cables.lay(level, at, cable.get()));
                    }
                    // The access line turns off north at the middle block, and the run starts at a router.
                    Cables.lay(level, ctx.abs(TURN.north()), ComputingModule.ETHERNET_CABLE.get());
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(-3, FLOOR, ROW)),
                            ComputingModule.PERSONAL_ROUTER.get().defaultBlockState());
                })
                .thenWaitUntil(() -> {
                    final CableBlockEntity turn = clientCable(ctx, TURN);
                    return turn != null && turn.wires().size() == FOUR.size() && turn.shape().junction();
                }, 80, "the player's game to know the run and its junction box")
                .thenAssert(2, () -> {
                    final CableBlockEntity turn = clientCable(ctx, TURN);
                    final BlockState state = turn.getBlockState();
                    final BakedModel model = ctx.mc().getBlockRenderer().getBlockModel(state);
                    return model instanceof CableBakedModel && !model.getQuads(state, null, RandomSource.create(0L),
                            turn.getModelData(), RenderType.cutout()).isEmpty();
                }, "the wires drawn by the cable block's own model")
                .thenAssert(0, () -> {
                    final CableBlockEntity end = clientCable(ctx, new BlockPos(-2, FLOOR, ROW));
                    return end != null && end.crosses(ComputingModule.ETHERNET_CABLE.get(), Direction.WEST)
                            && !end.crosses(ComputingModule.HPC_CABLE.get(), Direction.WEST);
                }, "the router takes the access and backbone lines, and only those")
                .then(0, () -> ctx.player().setXRot(LOOK_DOWN))
                .thenScreenshot(10, "four-lines")
                .thenServer(0, level -> clear(ctx, level));
    }

    @ClientTest(timeoutTicks = 400)
    public static void sharedBlock_ringsEachDyedWireOnItsBlock(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    for (int x = -2; x <= 2; x++) {
                        final BlockPos red = ctx.abs(new BlockPos(x, FLOOR, ROW));
                        final BlockPos blue = ctx.abs(new BlockPos(x, FLOOR, ROW + 1));
                        Cables.lay(level, red, ComputingModule.HBW_CABLE.get());
                        Cables.lay(level, blue, ComputingModule.HBW_CABLE.get());
                        Cables.at(level, red).dye(ComputingModule.HBW_CABLE.get(), DyeColor.RED);
                        Cables.at(level, blue).dye(ComputingModule.HBW_CABLE.get(), DyeColor.BLUE);
                    }
                    // An uncoloured backbone across both ends, joining each colour.
                    Cables.lay(level, ctx.abs(new BlockPos(3, FLOOR, ROW)), ComputingModule.HBW_CABLE.get());
                    Cables.lay(level, ctx.abs(new BlockPos(3, FLOOR, ROW + 1)), ComputingModule.HBW_CABLE.get());
                })
                .thenWaitUntil(() -> {
                    final CableBlockEntity uncoloured = clientCable(ctx, new BlockPos(3, FLOOR, ROW));
                    return uncoloured != null
                            && uncoloured.crosses(ComputingModule.HBW_CABLE.get(), Direction.WEST)
                            && uncoloured.crosses(ComputingModule.HBW_CABLE.get(), Direction.SOUTH);
                }, 80, "the uncoloured backbone joined to the red and the blue")
                .thenAssert(0, () -> {
                    final CableBlockEntity red = clientCable(ctx, new BlockPos(0, FLOOR, ROW));
                    return red != null && !red.crosses(ComputingModule.HBW_CABLE.get(), Direction.SOUTH);
                }, "red and blue side by side do not join")
                .then(0, () -> ctx.player().setXRot(LOOK_DOWN))
                .thenScreenshot(10, "colours")
                .thenServer(0, level -> clear(ctx, level));
    }

    private static @Nullable CableBlockEntity clientCable(final ClientTestContext ctx, final BlockPos relative) {
        return ctx.mc().level != null && ctx.mc().level.getBlockEntity(ctx.abs(relative)) instanceof CableBlockEntity
                cable ? cable : null;
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = -3; x <= 3; x++) {
            for (int z = ROW - 1; z <= ROW + 1; z++) {
                level.removeBlock(ctx.abs(new BlockPos(x, FLOOR, z)), false);
            }
        }
    }
}
