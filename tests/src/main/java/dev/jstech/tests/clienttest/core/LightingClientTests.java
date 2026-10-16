/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.light.DynamicLightsClient;
import dev.jstech.core.light.LampBlockEntity;
import dev.jstech.core.light.LightBlock;
import dev.jstech.tests.TestLights;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * How light looks to a player: a green lamp tints the floor it lights green, in the corners of the floor's faces as
 * the world's mesh is built; a torch held lights the way on the player's own game, and the way goes dark once it is
 * put away; a floodlight's shaft through the night air, lighting a wall twenty blocks off; and the halos of bright
 * lamps in the dark.
 */
public final class LightingClientTests {

    private static final BlockPos LAMP = new BlockPos(0, 2, 3);
    private static final BlockPos FLOOR = new BlockPos(1, 1, 3);
    private static final BlockPos STAND = new BlockPos(0, 3, -2);
    private static final BlockPos FLOOD = new BlockPos(-8, 2, 6);
    private static final int WALL_X = 12;
    private static final BlockPos FLOOD_STAND = new BlockPos(2, 3, -4);
    private static final long NIGHT = 18_000L;
    private static final long DAY = 6_000L;
    private static final long FULL_DAY = 24_000L;
    private static final long DUSK = 13_000L;
    private static final int WAIT = 300;
    private static final int CHANNEL = 0xFF;
    private static final int GREEN_SHIFT = 8;
    private static final int BLUE_SHIFT = 16;
    private static final long SEED = 42L;

    private LightingClientTests() {
    }

    @ClientTest(timeoutTicks = 900)
    public static void colouredLight_tintsTheFloorAGreenLampLights(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.serverPlayer().serverLevel().getServer().execute(() -> floor(ctx,
                ctx.serverPlayer().serverLevel(), Blocks.GRASS_BLOCK.defaultBlockState())));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    level.setDayTime(NIGHT);
                    // A white floor shows the colours; a red alarm and a blue soul lantern beside the green lamp.
                    floor(ctx, level, Blocks.WHITE_CONCRETE.defaultBlockState());
                    level.setBlockAndUpdate(ctx.abs(LAMP), TestLights.FRAGILE_LAMP.get().defaultBlockState());
                    level.setBlockAndUpdate(ctx.abs(LAMP.west(5)), TestLights.ALARM_LAMP.get().defaultBlockState());
                    level.setBlockAndUpdate(ctx.abs(LAMP.west(5).above()), Blocks.REDSTONE_BLOCK
                            .defaultBlockState());
                    level.setBlockAndUpdate(ctx.abs(LAMP.east(5)), Blocks.SOUL_LANTERN.defaultBlockState());
                })
                .thenWaitUntil(() -> lit(ctx, LAMP), WAIT, "the lamp to be on on the player's game")
                .thenWaitUntil(() -> greener(ctx), WAIT, "the floor beside it to be tinted green")
                .thenWaitUntil(() -> night(ctx), WAIT, "night on the player's game")
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(FLOOR))))
                .thenScreenshot(20, "coloured-lamps-tint-a-white-floor")
                .thenServer(0, level -> {
                    level.setDayTime(DAY);
                    clear(ctx, level);
                    floor(ctx, level, Blocks.GRASS_BLOCK.defaultBlockState());
                });
    }

    @ClientTest(timeoutTicks = 900)
    public static void dynamicLight_aTorchHeldLightsTheWay(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.serverPlayer().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    level.setDayTime(NIGHT);
                    ctx.serverPlayer().setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TORCH));
                })
                .thenWaitUntil(() -> !DynamicLightsClient.laid().isEmpty() && aroundPlayer(ctx) >= 12, WAIT,
                        "the torch's light laid about the player on their own game")
                .thenWaitUntil(() -> night(ctx), WAIT, "night on the player's game")
                .thenWaitUntilServer(level -> !(level.getBlockState(eye(ctx)).getBlock() instanceof LightBlock), 2,
                        "none laid in the server's world", level -> "the server holds "
                                + level.getBlockState(eye(ctx)) + " at the player's eyes")
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(FLOOR))))
                .thenScreenshot(10, "torch-held-lights-the-way")
                .thenServer(0, level -> ctx.serverPlayer().setItemInHand(InteractionHand.MAIN_HAND,
                        ItemStack.EMPTY))
                .thenWaitUntil(() -> DynamicLightsClient.laid().isEmpty() && aroundPlayer(ctx) == 0, WAIT,
                        "the way to go dark once the torch is put away")
                .thenServer(0, level -> {
                    level.setDayTime(DAY);
                    clear(ctx, level);
                });
    }

    @ClientTest(timeoutTicks = 900)
    public static void floodlight_shaftLightsAWallFarOff(final ClientTestContext ctx) {
        final BlockPos lit = new BlockPos(WALL_X - 1, FLOOD.getY(), FLOOD.getZ());
        ctx.thenTeleport(0, FLOOD_STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    level.setDayTime(NIGHT);
                    for (int y = 1; y <= 6; y++) {
                        for (int z = FLOOD.getZ() - 4; z <= FLOOD.getZ() + 4; z++) {
                            level.setBlockAndUpdate(ctx.abs(new BlockPos(WALL_X, y, z)),
                                    Blocks.STONE.defaultBlockState());
                        }
                    }
                    level.setBlockAndUpdate(ctx.abs(FLOOD), TestLights.FLOODLIGHT.get().defaultBlockState()
                            .setValue(DirectionalBlock.FACING, Direction.EAST));
                })
                .thenWaitUntil(() -> clientState(ctx, lit).getBlock() instanceof LightBlock, WAIT,
                        "the wall's light, twenty blocks off, laid by the server and seen on the player's game")
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(new BlockPos(2, 3, FLOOD.getZ())))))
                .thenScreenshot(20, "floodlight-shaft-at-night")
                .thenServer(0, level -> {
                    level.setDayTime(DAY);
                    clear(ctx, level);
                    for (int y = 1; y <= 6; y++) {
                        for (int z = FLOOD.getZ() - 4; z <= FLOOD.getZ() + 4; z++) {
                            level.setBlockAndUpdate(ctx.abs(new BlockPos(WALL_X, y, z)),
                                    Blocks.AIR.defaultBlockState());
                        }
                    }
                });
    }

    @ClientTest(timeoutTicks = 900)
    public static void halos_glowAboutBrightLampsInTheDark(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    level.setDayTime(NIGHT);
                    level.setBlockAndUpdate(ctx.abs(LAMP), TestLights.FRAGILE_LAMP.get().defaultBlockState());
                    level.setBlockAndUpdate(ctx.abs(LAMP.west(3)), Blocks.SOUL_LANTERN.defaultBlockState());
                    level.setBlockAndUpdate(ctx.abs(LAMP.east(3)), Blocks.SEA_LANTERN.defaultBlockState());
                })
                .thenWaitUntil(() -> lit(ctx, LAMP), WAIT, "the lamp to be on on the player's game")
                .then(30, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(LAMP))))
                .thenScreenshot(10, "halos-at-night")
                .thenServer(0, level -> {
                    level.setDayTime(DAY);
                    clear(ctx, level);
                });
    }

    /* Whether it is night on the player's game, which hears the time a second at a time. */
    private static boolean night(final ClientTestContext ctx) {
        final ClientLevel level = ctx.mc().level;
        return level != null && level.getDayTime() % FULL_DAY > DUSK;
    }

    /* The floor under the tests' blocks, laid of {@code state}. */
    private static void floor(final ClientTestContext ctx, final ServerLevel level, final BlockState state) {
        for (int x = -9; x <= 4; x++) {
            for (int z = 0; z <= 7; z++) {
                level.setBlockAndUpdate(ctx.abs(new BlockPos(x, 1, z)), state);
            }
        }
    }

    /* Whether the lamp at {@code at} is on on the player's game. */
    private static boolean lit(final ClientTestContext ctx, final BlockPos at) {
        final ClientLevel level = ctx.mc().level;
        return level != null && level.getBlockEntity(ctx.abs(at)) instanceof LampBlockEntity lamp && lamp.lit();
    }

    /* Whether the top of the floor beside the lamp is drawn greener than it is red and blue. */
    private static boolean greener(final ClientTestContext ctx) {
        final ClientLevel level = ctx.mc().level;
        if (level == null) {
            return false;
        }
        final BlockPos at = ctx.abs(FLOOR);
        final BlockState state = level.getBlockState(at);
        final BakedModel model = ctx.mc().getBlockRenderer().getBlockModel(state);
        final ModelData data = model.getModelData(level, at, state, ModelData.EMPTY);
        final List<BakedQuad> top = model.getQuads(state, Direction.UP, RandomSource.create(SEED), data, null);
        if (top.isEmpty()) {
            return false;
        }
        // A corner keeps its colour as red, green, blue and alpha from the lowest byte up.
        final int colour = top.getFirst().getVertices()[3];
        final int red = colour & CHANNEL;
        final int green = colour >> GREEN_SHIFT & CHANNEL;
        final int blue = colour >> BLUE_SHIFT & CHANNEL;
        return green == CHANNEL && red < CHANNEL && blue < CHANNEL;
    }

    /* The light laid where the player's eyes are, on their own game. */
    private static int aroundPlayer(final ClientTestContext ctx) {
        final ClientLevel level = ctx.mc().level;
        return level == null || ctx.mc().player == null ? 0 : level.getBrightness(LightLayer.BLOCK,
                BlockPos.containing(ctx.mc().player.getEyePosition()));
    }

    private static BlockPos eye(final ClientTestContext ctx) {
        return BlockPos.containing(ctx.serverPlayer().getEyePosition());
    }

    private static BlockState clientState(final ClientTestContext ctx, final BlockPos at) {
        final ClientLevel level = ctx.mc().level;
        return level == null ? Blocks.AIR.defaultBlockState() : level.getBlockState(ctx.abs(at));
    }

    /* Everything the tests place, back to air. */
    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = -9; x <= 4; x++) {
            for (int y = 2; y <= 5; y++) {
                for (int z = 0; z <= 7; z++) {
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, z)), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }
}
