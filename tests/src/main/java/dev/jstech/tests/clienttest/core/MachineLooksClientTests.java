/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.model.kit.KitAnimations;
import dev.jstech.core.heat.Blackbody;
import dev.jstech.core.heat.HeatBody;
import dev.jstech.core.heat.IHeatHolder;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.mechanical.MechanicalPartBlockEntity;
import dev.jstech.tests.TestHeat;
import dev.jstech.tests.TestMechanical;
import dev.jstech.tests.TestModels;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * How machines look to a player: a turbine whose iron merges into its model once it forms, its rotor turning; a crank
 * turning a shaft, two meshed gears and a pulley that drives a larger one by a belt, all drawn turning as fast as they
 * turn; a hot plate glowing red in the dark; and the same plate, a pig and the room through thermal goggles, in false
 * colour, with how hot the plate is read beside the crosshair.
 */
public final class MachineLooksClientTests {

    /* The turbine's controller, the middle of its south side; it runs north of it. */
    private static final BlockPos TURBINE = new BlockPos(0, 2, 4);
    private static final BlockPos TURBINE_STAND = new BlockPos(0, 3, 8);
    /* The parts' line runs along X; they are looked at from the east, face on. */
    private static final BlockPos CRANK = new BlockPos(-3, 2, 2);
    private static final BlockPos SMALL_GEAR = new BlockPos(-1, 2, 2);
    private static final BlockPos LARGE_GEAR = new BlockPos(-1, 3, 2);
    private static final BlockPos SMALL_PULLEY = new BlockPos(0, 2, 2);
    private static final BlockPos LARGE_PULLEY = new BlockPos(0, 4, 4);
    private static final BlockPos PARTS_STAND = new BlockPos(4, 3, 3);
    private static final BlockPos PLATE = new BlockPos(0, 2, 1);
    private static final BlockPos PIG = new BlockPos(-2, 2, 1);
    private static final BlockPos ROOM_IRON = new BlockPos(2, 2, 1);
    private static final BlockPos STAND = new BlockPos(0, 3, -3);
    private static final long NIGHT = 18_000L;
    private static final long DAY = 6_000L;
    private static final int WAIT = 300;
    /* A heat scan is every half second; two of them, so the view has caught up. */
    private static final int SCANNED = 24;

    private MachineLooksClientTests() {
    }

    @ClientTest(timeoutTicks = 900)
    public static void turbine_isDrawnAsItsModelOnceItForms(final ClientTestContext ctx) {
        ctx.thenTeleport(0, TURBINE_STAND, Direction.NORTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    for (int y = 0; y < 2; y++) {
                        for (int x = -1; x <= 1; x++) {
                            for (int z = 0; z <= 2; z++) {
                                final BlockPos at = TURBINE.offset(x, y, -z);
                                if (!at.equals(TURBINE)) {
                                    level.setBlockAndUpdate(ctx.abs(at), Blocks.IRON_BLOCK.defaultBlockState());
                                }
                            }
                        }
                    }
                    level.setBlockAndUpdate(ctx.abs(TURBINE), TestModels.TURBINE_BLOCK.get().defaultBlockState());
                })
                .thenWaitUntil(() -> client(ctx, TURBINE) instanceof MachineBlockEntity turbine
                        && turbine.structure() != null && turbine.structure().merged(), WAIT,
                        "the turbine to form and merge on the player's game")
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(TURBINE.offset(0, 0, -1)))))
                .thenScreenshot(20, "turbine-formed-model")
                .thenServer(0, level -> clear(ctx, level));
    }

    @ClientTest(timeoutTicks = 900)
    public static void turningParts_turnAsFastAsTheyTurnTheirBeltRunning(final ClientTestContext ctx) {
        ctx.thenTeleport(0, PARTS_STAND, Direction.WEST)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    part(ctx, level, CRANK, TestMechanical.CRANK.get());
                    part(ctx, level, CRANK.east(), TestMechanical.SHAFT.get());
                    part(ctx, level, SMALL_GEAR, TestMechanical.SMALL_GEAR.get());
                    part(ctx, level, LARGE_GEAR, TestMechanical.LARGE_GEAR.get());
                    part(ctx, level, SMALL_PULLEY, TestMechanical.SMALL_PULLEY.get());
                    part(ctx, level, LARGE_PULLEY, TestMechanical.LARGE_PULLEY.get());
                    final MechanicalPartBlockEntity small = (MechanicalPartBlockEntity) level.getBlockEntity(
                            ctx.abs(SMALL_PULLEY));
                    final MechanicalPartBlockEntity large = (MechanicalPartBlockEntity) level.getBlockEntity(
                            ctx.abs(LARGE_PULLEY));
                    small.link(ctx.abs(LARGE_PULLEY), false, BuiltInRegistries.ITEM.getKey(
                            TestMechanical.BELT.get()));
                    large.link(ctx.abs(SMALL_PULLEY), false, BuiltInRegistries.ITEM.getKey(
                            TestMechanical.BELT.get()));
                })
                .thenWaitUntil(() -> client(ctx, LARGE_PULLEY) instanceof MechanicalPartBlockEntity pulley
                        && Math.abs(pulley.rpm()) > 1.0, WAIT, "the belt to turn the large pulley")
                .thenWaitUntil(() -> KitAnimations.drawn(client(ctx, SMALL_GEAR)).isPresent(), WAIT,
                        "the gear to be drawn by the Core's models")
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(new BlockPos(-1, 3, 3)))))
                .thenScreenshot(10, "turning-parts-and-belt")
                .thenScreenshot(7, "turning-parts-and-belt-later")
                .thenServer(0, level -> clear(ctx, level));
    }

    @ClientTest(timeoutTicks = 900)
    public static void hotPlate_glowsRedInTheDark(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    level.setDayTime(NIGHT);
                    level.setBlockAndUpdate(ctx.abs(PLATE), TestHeat.HOT_PLATE.get().defaultBlockState());
                })
                .thenWaitUntil(() -> clientKelvin(ctx, PLATE) > Blackbody.DRAPER_POINT + 300.0, WAIT,
                        "the plate to reach a red heat on the player's game")
                .then(SCANNED, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(PLATE))))
                .thenScreenshot(6, "hot-plate-glowing-at-night")
                .thenAssert(0, () -> {
                    final int colour = plateOnScreen(ctx);
                    return (colour >> 16 & 0xFF) > (colour >> 8 & 0xFF) + 30;
                }, "the plate glows red")
                .thenServer(0, level -> {
                    level.setDayTime(DAY);
                    clear(ctx, level);
                });
    }

    @ClientTest(timeoutTicks = 900)
    public static void thermalView_showsHeatInFalseColour(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.serverPlayer().setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    level.setBlockAndUpdate(ctx.abs(PLATE), TestHeat.HOT_PLATE.get().defaultBlockState());
                    level.setBlockAndUpdate(ctx.abs(ROOM_IRON), TestHeat.IRON.get().defaultBlockState());
                    final Pig pig = EntityType.PIG.create(level);
                    pig.setNoAi(true);
                    pig.moveTo(Vec3.atBottomCenterOf(ctx.abs(PIG)));
                    level.addFreshEntity(pig);
                    ctx.serverPlayer().setItemSlot(EquipmentSlot.HEAD, new ItemStack(TestHeat.GOGGLES.get()));
                })
                .thenWaitUntil(() -> clientKelvin(ctx, PLATE) > Blackbody.DRAPER_POINT, WAIT,
                        "the plate to heat up on the player's game")
                .then(SCANNED, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(PLATE))))
                .thenScreenshot(6, "thermal-view")
                .thenAssert(0, () -> {
                    final int colour = plateOnScreen(ctx);
                    return (colour >> 16 & 0xFF) > 200;
                }, "the plate shows hot, in the bright end of the false colours")
                .thenAssert(0, () -> {
                    final int corner = ctx.pixel(2, 2);
                    return (corner >> 16 & 0xFF) < 70 && (corner >> 8 & 0xFF) < 70 && (corner & 0xFF) < 90;
                }, "the sky, as cold as the air, lies under the dark veil")
                .thenServer(0, level -> {
                    ctx.serverPlayer().setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                    clear(ctx, level);
                })
                .thenScreenshot(6, "thermal-view-goggles-off");
    }

    /* The colour on screen a little up and left of the crosshair, clear of it and of the reading beside it. */
    private static int plateOnScreen(final ClientTestContext ctx) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2 - 8;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2 - 8;
        return ctx.pixel(x, y);
    }

    private static void part(final ClientTestContext ctx, final ServerLevel level, final BlockPos at,
                             final Block block) {
        level.setBlockAndUpdate(ctx.abs(at), block.defaultBlockState().setValue(RotatedPillarBlock.AXIS,
                Direction.Axis.X));
    }

    private static BlockEntity client(final ClientTestContext ctx, final BlockPos at) {
        return ctx.mc().level == null ? null : ctx.mc().level.getBlockEntity(ctx.abs(at));
    }

    /* How hot the block at {@code at} is on the player's game; none while it is not known there. */
    private static double clientKelvin(final ClientTestContext ctx, final BlockPos at) {
        if (client(ctx, at) instanceof IHeatHolder holder) {
            final HeatBody body = holder.heatBody();
            return body != null && body.known() ? body.temperature() : 0.0;
        }
        return 0.0;
    }

    /* Everything the tests place, back to air, and the pig gone. */
    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = -4; x <= 2; x++) {
            for (int y = 2; y <= 5; y++) {
                for (int z = 0; z <= 6; z++) {
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, z)), Blocks.AIR.defaultBlockState());
                }
            }
        }
        level.getEntitiesOfClass(Pig.class, ctx.serverPlayer().getBoundingBox().inflate(16))
                .forEach(pig -> pig.discard());
    }
}
