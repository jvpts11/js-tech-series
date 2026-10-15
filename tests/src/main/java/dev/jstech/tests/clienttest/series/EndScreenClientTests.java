/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.series;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableEnd;
import dev.jstech.core.cable.CableEndLayout;
import dev.jstech.core.cable.Lane;
import dev.jstech.core.client.cable.CableEndScreen;
import dev.jstech.core.grid.EndMode;
import dev.jstech.core.inventory.ItemFilter;
import dev.jstech.core.inventory.NamedSubject;
import dev.jstech.core.jstechseries.lines.SharedPipes;
import dev.jstech.core.jstechseries.tools.SharedTools;
import dev.jstech.tests.TestFluidEnds;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.TestCables;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * The screen of a line's end on a player's game, to set beside the approved design: opened by sneaking with the Basic
 * Wrench on the east end of a copper pipe, set from its keys, its rate typed, a bucket of water clicked onto a filter
 * slot and three scrolls keeping 300 mB; and, with the wrench in hand, what flows along the pipe read beside the
 * crosshair.
 */
public final class EndScreenClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, -2);
    private static final int FLOOR = 2;
    private static final BlockPos OUTPUT = new BlockPos(-3, FLOOR, 1);
    private static final BlockPos INPUT = new BlockPos(2, FLOOR, 1);
    private static final List<BlockPos> RUN = List.of(new BlockPos(-2, FLOOR, 1), new BlockPos(-1, FLOOR, 1),
            new BlockPos(0, FLOOR, 1), new BlockPos(1, FLOOR, 1));
    private static final BlockPos LAST = new BlockPos(1, FLOOR, 1);
    private static final int WAIT = 200;
    private static final int BUCKET_SLOT = 9;
    private static final int KEEP = 300;
    private static final int PRIORITY = 5;
    private static final int DOUBLE_CLICK = 8;

    private EndScreenClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void endScreen_setsTheEndAndItsFilter(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> build(ctx, level))
                .thenGive(0, new ItemStack(SharedTools.WRENCH.get()))
                .thenServer(0, level -> ctx.give(BUCKET_SLOT, new ItemStack(Items.WATER_BUCKET)))
                .then(2, () -> {
                    ctx.selectHotbar(0);
                    ctx.lookAt(Vec3.atCenterOf(ctx.abs(LAST)));
                })
                .thenSneakClick(2, LAST, Direction.EAST)
                .thenWaitUntil(() -> shown(ctx, end -> true), WAIT, "the east end's screen to open")
                .thenScreenshot(5, "end-open")
                .then(0, () -> ctx.clickGui(CableEndLayout.modeX(1) + 4, CableEndLayout.MODE_Y + 4))
                .thenWaitUntil(() -> shown(ctx, end -> end.mode() == EndMode.PUSH), WAIT, "Push to set push")
                .then(0, () -> {
                    for (int k = 0; k < PRIORITY; k++) {
                        ctx.clickGui(CableEndLayout.plusX(CableEndLayout.PRIORITY_FIELD_WIDTH) + 4,
                                CableEndLayout.PRIORITY_Y + 4);
                    }
                })
                .thenWaitUntil(() -> shown(ctx, end -> end.priority() == PRIORITY), WAIT, "plus to raise the priority")
                .then(0, () -> ctx.clickGui(CableEndLayout.FIELD_X + 4, CableEndLayout.RATE_Y + 4))
                .then(1, () -> {
                    final CableEndScreen screen = ctx.screen(CableEndScreen.class);
                    for (final char typed : "250".toCharArray()) {
                        screen.charTyped(typed, 0);
                    }
                    screen.keyPressed(InputConstants.KEY_RETURN, 0, 0);
                })
                .thenWaitUntil(() -> shown(ctx, end -> end.rate() == 250L), WAIT, "the rate typed to be set")
                .then(0, () -> ctx.clickGui(CableEndLayout.INVENTORY_X + 8, CableEndLayout.INVENTORY_Y + 8))
                .then(2, () -> ctx.clickGui(CableEndLayout.ghostX(0) + 8, CableEndLayout.GHOST_Y + 8))
                .thenWaitUntil(() -> shown(ctx, end -> end.filter().rules().equals(List.of(
                        new ItemFilter.Fuzzy("minecraft:water", 0L)))), WAIT, "the bucket to name water")
                // Put back once a double click is past: the game reads two quick clicks on a slot as one.
                .then(DOUBLE_CLICK, () -> ctx.clickGui(CableEndLayout.INVENTORY_X + 8,
                        CableEndLayout.INVENTORY_Y + 8))
                .then(1, () -> {
                    final CableEndScreen screen = ctx.screen(CableEndScreen.class);
                    for (int k = 0; k < KEEP / 100; k++) {
                        screen.mouseScrolled(screen.getGuiLeft() + CableEndLayout.ghostX(0) + 8,
                                screen.getGuiTop() + CableEndLayout.GHOST_Y + 8, 0, 1);
                    }
                })
                .thenWaitUntil(() -> shown(ctx, end -> end.filter().amountFor(new NamedSubject("minecraft:water"))
                        == KEEP), WAIT, "three scrolls to keep 300 mB")
                .thenScreenshot(5, "end-pipe")
                .then(0, () -> ctx.player().closeContainer())
                .thenAwaitNoScreen(40)
                // The end keeps 300 mB in the input and so the water stops: set back, it runs again.
                .thenServer(0, level -> {
                    if (level.getBlockEntity(ctx.abs(LAST)) instanceof CableBlockEntity cable) {
                        cable.setEnd(SharedPipes.COPPER_PIPE.get(), Direction.EAST, CableEnd.DEFAULT);
                    }
                })
                .then(0, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(RUN.get(1)))))
                .thenWaitUntil(() -> ctx.mc().level != null && ctx.mc().level.getBlockEntity(ctx.abs(RUN.get(1)))
                        instanceof CableBlockEntity cable && !cable.flow(Lane.MIDDLE).isEmpty(), WAIT,
                        "the water to run again")
                .thenScreenshot(10, "flow-sight")
                .thenServer(0, level -> clear(ctx, level));
    }

    /* Whether the end's screen is open and shows what {@code test} looks for. */
    private static boolean shown(final ClientTestContext ctx, final Predicate<CableEnd> test) {
        return ctx.mc().screen instanceof CableEndScreen screen && test.test(screen.getMenu().end());
    }

    /* An output full of water, a run of copper pipe east, and an input past its east end. */
    private static void build(final ClientTestContext ctx, final ServerLevel level) {
        clear(ctx, level);
        TestFluidEnds.clear(level, ctx.abs(OUTPUT));
        TestFluidEnds.clear(level, ctx.abs(INPUT));
        TestFluidEnds.tankAt(level, ctx.abs(OUTPUT)).fill(new FluidStack(Fluids.WATER, 60_000),
                IFluidHandler.FluidAction.EXECUTE);
        level.setBlockAndUpdate(ctx.abs(OUTPUT), Blocks.CHISELED_TUFF.defaultBlockState());
        level.setBlockAndUpdate(ctx.abs(INPUT), Blocks.CHISELED_TUFF_BRICKS.defaultBlockState());
        for (final BlockPos at : RUN) {
            TestCables.lay(level, ctx.abs(at), SharedPipes.COPPER_PIPE.get());
        }
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (final BlockPos at : List.of(OUTPUT, INPUT, RUN.get(0), RUN.get(1), RUN.get(2), RUN.get(3))) {
            level.setBlockAndUpdate(ctx.abs(at), Blocks.AIR.defaultBlockState());
        }
    }
}
