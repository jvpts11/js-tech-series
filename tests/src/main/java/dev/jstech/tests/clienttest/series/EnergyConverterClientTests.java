/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.series;

import dev.jstech.core.jstechseries.energy.BatteryBankBlockEntity;
import dev.jstech.core.jstechseries.energy.ConverterView;
import dev.jstech.core.jstechseries.energy.EnergyConverterBlockEntity;
import dev.jstech.core.jstechseries.energy.EnergyConverterLayout;
import dev.jstech.core.jstechseries.energy.EnergyConverters;
import dev.jstech.core.jstechseries.energy.SharedEnergy;
import dev.jstech.core.jstechseries.energy.VoltageClass;
import dev.jstech.core.jstechseries.energy.client.EnergyConverterScreen;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.TestCables;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * A converter's screen on a player's game, to set beside the approved design: a configurable converter feeding a
 * machine FE from a battery, its needles up and its bridge running; set from its face (a class key, the knob, a drum,
 * the switch), each change seen on the screen; and a fixed one with its ceiling's plate.
 */
public final class EnergyConverterClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, -1);
    private static final int ROW = 1;
    private static final int FLOOR = 2;
    private static final BlockPos CONVERTER = new BlockPos(0, FLOOR, ROW);
    private static final BlockPos CABLE = new BlockPos(-1, FLOOR, ROW);
    private static final BlockPos BATTERY = new BlockPos(-2, FLOOR, ROW);
    private static final BlockPos MACHINE = new BlockPos(1, FLOOR, ROW);
    private static final int WAIT = 120;

    private EnergyConverterClientTests() {
    }

    @ClientTest(timeoutTicks = 800)
    public static void converter_showsWhatCrossesAndIsSetFromItsFace(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> build(ctx, level, EnergyConverters.CONFIGURABLE.get()))
                .thenRightClick(5, CONVERTER)
                .thenWaitUntil(() -> shown(ctx, ConverterView::running), WAIT,
                        "the converter's screen to show energy crossing")
                .thenScreenshot(10, "converter-running")
                .then(0, () -> ctx.clickGui(EnergyConverterLayout.KEY_X + EnergyConverterLayout.KEY_PITCH + 4,
                        EnergyConverterLayout.KEY_Y + 4))
                .thenWaitUntil(() -> shown(ctx, view -> view.voltageClass() == VoltageClass.MEDIUM), WAIT,
                        "the MV key to set its class")
                .then(0, () -> ctx.clickGui(EnergyConverterLayout.KNOB_X, EnergyConverterLayout.KNOB_Y))
                .thenWaitUntil(() -> shown(ctx, ConverterView::toJe), WAIT, "the knob to turn it toward JE")
                .then(0, () -> ctx.clickGui(EnergyConverterLayout.DRUM_X + 1, EnergyConverterLayout.DRUM_Y + 2))
                .thenWaitUntil(() -> shown(ctx, view -> view.limit().digits() == 1100), WAIT,
                        "the first drum's upper half to turn it up")
                .then(0, () -> ctx.clickGui(EnergyConverterLayout.SWITCH_X + 5, EnergyConverterLayout.SWITCH_Y + 5))
                .thenWaitUntil(() -> shown(ctx, view -> !view.on()), WAIT, "the switch to turn it off")
                .thenScreenshot(5, "converter-set")
                .then(0, () -> ctx.mc().setScreen(null))
                .thenServer(0, level -> build(ctx, level, EnergyConverters.HIGH.get()))
                .thenRightClick(5, CONVERTER)
                .thenWaitUntil(() -> shown(ctx, ConverterView::fixed), WAIT, "the fixed converter's screen")
                .thenScreenshot(10, "converter-fixed")
                .then(0, () -> ctx.mc().setScreen(null))
                .thenServer(0, level -> clear(ctx, level));
    }

    /* Whether the converter's screen is open and shows what {@code test} looks for. */
    private static boolean shown(final ClientTestContext ctx, final Predicate<ConverterView> test) {
        return ctx.mc().screen instanceof EnergyConverterScreen screen && test.test(screen.getMenu().view());
    }

    /* A full battery, a run of the basic cable, the converter toward FE, and a machine taking the FE. */
    private static void build(final ClientTestContext ctx, final ServerLevel level, final Block converter) {
        clear(ctx, level);
        level.setBlockAndUpdate(ctx.abs(BATTERY), SharedEnergy.BATTERY.get().defaultBlockState());
        if (level.getBlockEntity(ctx.abs(BATTERY)) instanceof BatteryBankBlockEntity battery) {
            battery.store().setStored(SharedEnergy.BATTERY_CAPACITY);
        }
        TestCables.lay(level, ctx.abs(CABLE), SharedEnergy.INSULATED_COPPER_CABLE.get());
        level.setBlockAndUpdate(ctx.abs(CONVERTER), converter.defaultBlockState());
        if (level.getBlockEntity(ctx.abs(CONVERTER)) instanceof EnergyConverterBlockEntity placed) {
            placed.setToJe(false);
        }
        level.setBlockAndUpdate(ctx.abs(MACHINE), IndustrialModule.COMPRESSOR.get().defaultBlockState());
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (final BlockPos at : List.of(BATTERY, CABLE, CONVERTER, MACHINE)) {
            level.setBlockAndUpdate(ctx.abs(at), Blocks.AIR.defaultBlockState());
        }
    }
}
