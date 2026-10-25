/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.series;

import dev.jstech.core.jstechseries.energy.ClampMeter;
import dev.jstech.core.jstechseries.fire.FireSafety;
import dev.jstech.core.jstechseries.hazard.HazardDevices;
import dev.jstech.core.jstechseries.probe.NetworkProbe;
import dev.jstech.core.jstechseries.probe.SignalProbe;
import dev.jstech.core.jstechseries.tools.SharedTools;
import dev.jstech.industrial.tool.HandTools;
import dev.jstech.testkit.client.ClientTestHelper;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * The tools and instruments drawn in three dimensions in the hand: each of the fourteen held, read off the screen
 * against the empty hand, and seen from the player's eye and from in front of them, for the eye to judge its grip;
 * and all of them laid on the ground.
 */
public final class HeldModelsClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, 0);
    private static final int WAIT = 200;
    /*
     * Ticks for a held item to be raised into view once it changes: a slow tool's swing is kept lowered until it has
     * recovered, a second and a quarter for the Forge Hammer.
     */
    private static final int RAISED = 30;
    /* The items laid on the ground ahead of the player: so many in a row, the rows so far apart, in blocks. */
    private static final int A_ROW = 7;
    private static final int ROWS_APART = 2;
    /*
     * Where the held item is drawn in first person, as shares of the screen, read a pixel in so many; how different a
     * pixel must be from the empty hand's, in the sum of its channels, to count; and how many must, at least.
     */
    private static final double FROM_X = 0.5;
    private static final double FROM_Y = 0.4;
    private static final int STEP = 4;
    private static final int DIFFERENT = 60;
    private static final int DRAWN = 40;
    private static final int CHANNEL = 0xFF;
    private static final List<Supplier<? extends Item>> HELD = List.of(HandTools.FORGE_HAMMER, HandTools.DRAW_PLATE,
            HandTools.MORTAR, SharedTools.SAW, SharedTools.WRENCH, FireSafety.WATER_EXTINGUISHER,
            FireSafety.FOAM_EXTINGUISHER, FireSafety.CARBON_DIOXIDE_EXTINGUISHER, FireSafety.DRY_POWDER_EXTINGUISHER,
            NetworkProbe.ITEM, SignalProbe.ITEM, ClampMeter.ITEM, HazardDevices.GEIGER_COUNTER,
            HazardDevices.POCKET_DOSIMETER);

    private HeldModelsClientTests() {
    }

    @ClientTest(timeoutTicks = 3000)
    public static void held_eachToolAndInstrumentIsDrawnInTheHand(final ClientTestContext ctx) {
        final int[][] empty = new int[1][];
        final List<ItemEntity> dropped = new ArrayList<>();
        ctx.afterTest(() -> {
            ctx.mc().options.setCameraType(CameraType.FIRST_PERSON);
            ctx.server().execute(() -> dropped.forEach(Entity::discard));
        });
        ctx.thenTeleport(0, STAND, Direction.NORTH)
                .thenServer(0, level -> ctx.hold(ItemStack.EMPTY))
                .thenAwaitLandDrawn(WAIT)
                .then(0, () -> ctx.lookAt(ctx.player().getEyePosition().add(0, 0, -1)))
                .then(RAISED, () -> empty[0] = hand(ctx));
        for (final Supplier<? extends Item> held : HELD) {
            final String name = BuiltInRegistries.ITEM.getKey(held.get()).getPath();
            ctx.thenServer(0, level -> ctx.hold(new ItemStack(held.get())))
                    .thenWaitUntil(() -> ctx.player().getMainHandItem().is(held.get()), WAIT, name + " in the hand")
                    .then(RAISED, () -> {
                        final int drawn = differing(empty[0], hand(ctx));
                        ctx.assertTrue(drawn >= DRAWN, name + " is drawn in the hand: " + drawn + " pixels");
                    })
                    .thenScreenshot(0, "held-" + name)
                    .then(0, () -> ctx.mc().options.setCameraType(CameraType.THIRD_PERSON_FRONT))
                    .thenScreenshot(3, "held-" + name + "-front")
                    .then(0, () -> ctx.mc().options.setCameraType(CameraType.FIRST_PERSON));
        }
        // On the ground they keep their models too, laid in two rows ahead of the player.
        ctx.thenServer(0, level -> {
            ctx.hold(ItemStack.EMPTY);
            for (int i = 0; i < HELD.size(); i++) {
                final BlockPos at = ctx.abs(new BlockPos(i % A_ROW - A_ROW / 2, STAND.getY(),
                        -3 - i / A_ROW * ROWS_APART));
                final ItemEntity item = new ItemEntity(level, at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5,
                        new ItemStack(HELD.get(i).get()), 0, 0, 0);
                item.setNeverPickUp();
                level.addFreshEntity(item);
                dropped.add(item);
            }
        })
                .then(20, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(new BlockPos(0, STAND.getY(), -4)))))
                .thenScreenshot(10, "held-models-on-the-ground")
                .thenServer(0, level -> dropped.forEach(Entity::discard));
    }

    /* The brightness of the screen where the main hand's item is drawn, a pixel in so many. */
    private static int[] hand(final ClientTestContext ctx) {
        final int width = ctx.mc().getWindow().getGuiScaledWidth();
        final int height = ctx.mc().getWindow().getGuiScaledHeight();
        final int fromX = (int) (width * FROM_X);
        final int fromY = (int) (height * FROM_Y);
        final int[] out = new int[((width - fromX) / STEP) * ((height - fromY) / STEP)];
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            int i = 0;
            for (int y = fromY; y + STEP <= height; y += STEP) {
                for (int x = fromX; x + STEP <= width; x += STEP) {
                    if (i < out.length) {
                        final int argb = frame.pixel(x, y);
                        out[i++] = (argb >> 16 & CHANNEL) + (argb >> 8 & CHANNEL) + (argb & CHANNEL);
                    }
                }
            }
        }
        return out;
    }

    /* How many pixels differ clearly between two readings. */
    private static int differing(final int[] before, final int[] after) {
        int count = 0;
        for (int i = 0; i < Math.min(before.length, after.length); i++) {
            if (Math.abs(before[i] - after[i]) > DIFFERENT) {
                count++;
            }
        }
        return count;
    }
}
