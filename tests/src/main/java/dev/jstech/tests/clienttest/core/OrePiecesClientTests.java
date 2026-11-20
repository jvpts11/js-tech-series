/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.JsCore;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.Materials;
import dev.jstech.core.material.OrePiecesBlock;
import dev.jstech.testkit.client.ClientTestFailure;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The pieces of ores that lie over their veins, close by, as the approved picture of them shows them: six ores' pieces,
 * each cut from its ore's own picture, on grass, sand, gravel and a block of stone, lying their three ways turned
 * different ways; each drawn with its art, none with the missing texture's magenta.
 */
public final class OrePiecesClientTests {

    /* The six ores, where each lies (block x and z), the way its pieces lie and how they are turned. */
    private static final Spot[] SPOTS = {
        new Spot("chalcopyrite", 0, 0, 0, 3), new Spot("cassiterite", 3, 0, 1, 1), new Spot("hematite", 5, 0, 2, 3),
        new Spot("malachite", 0, 2, 1, 1), new Spot("pyrite", 3, 3, 2, 1), new Spot("magnetite", 6, 2, 0, 0)
    };
    private static final Direction[] TURNS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    /* The ground laid, from block x and z to x and z, and the player's eye, where it looks, and a frame's wait. */
    private static final int WEST = -3;
    private static final int EAST = 10;
    private static final int NORTH = -4;
    private static final int SOUTH = 8;
    private static final Vec3 EYE = new Vec3(4.2, 4.4, 5.8);
    private static final Vec3 TARGET = new Vec3(3.4, 1.3, 1.6);
    private static final double EYE_HEIGHT = 1.62;
    private static final long NOON = 6_000L;
    private static final int ARRIVE = 200;
    private static final int SHOOT = 5;
    private static final int UPDATE = 2;
    /* How much of the view's middle may be the missing texture's magenta at most, and the channels that tell it. */
    private static final double MISSING_ART = 0.002;
    private static final int MAGENTA_HIGH = 180;
    private static final int MAGENTA_LOW = 70;

    private OrePiecesClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void pieces_lookAsTheirApprovedPicture(final ClientTestContext ctx) {
        final Vec3[] stand = new Vec3[1];
        ctx.thenServer(0, level -> {
            level.getServer().overworld().setDayTime(NOON);
            final BlockPos origin = ctx.origin();
            for (int x = WEST; x <= EAST; x++) {
                for (int z = NORTH; z <= SOUTH; z++) {
                    level.setBlock(origin.offset(x, 0, z), ground(x, z).defaultBlockState(), UPDATE);
                    for (int y = 1; y <= 6; y++) {
                        level.setBlock(origin.offset(x, y, z), (y == 1 && raised(x, z) ? Blocks.STONE : Blocks.AIR)
                                .defaultBlockState(), UPDATE);
                    }
                }
            }
            for (final Spot spot : SPOTS) {
                final Block pieces = Materials.byId(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, spot.ore()))
                        .flatMap(MaterialUses::piecesOf).orElseThrow();
                level.setBlock(origin.offset(spot.x(), raised(spot.x(), spot.z()) ? 2 : 1, spot.z()),
                        pieces.defaultBlockState().setValue(OrePiecesBlock.LAYOUT, spot.layout())
                                .setValue(OrePiecesBlock.FACING, TURNS[spot.turn()]), UPDATE);
            }
            stand[0] = Vec3.atLowerCornerOf(origin).add(EYE).subtract(0, EYE_HEIGHT, 0);
            go(ctx, level, stand[0]);
        })
                .thenWaitUntil(() -> ctx.player() != null && ctx.player().position().distanceTo(stand[0]) < 1,
                        ARRIVE, "the player's game to reach the pieces")
                .then(1, () -> ctx.lookAt(Vec3.atLowerCornerOf(ctx.origin()).add(TARGET)))
                .thenAwaitLandDrawn(ARRIVE)
                .thenScreenshot(SHOOT, "ore_pieces_close_by")
                .then(1, () -> expectArtDrawn(ctx, "the six ores' pieces"));
    }

    /* The ground of the picture: sand and a raised block of stone in the middle, gravel near, grass round them. */
    private static Block ground(final int x, final int z) {
        if (x >= 2 && x <= 3 && z >= -1 && z <= 1) {
            return Blocks.SAND;
        }
        if (raised(x, z)) {
            return Blocks.STONE;
        }
        if (x >= -1 && x <= 0 && z >= 2 && z <= 3) {
            return Blocks.GRAVEL;
        }
        return Blocks.GRASS_BLOCK;
    }

    private static boolean raised(final int x, final int z) {
        return x >= 5 && x <= 6 && z >= -1 && z <= 1;
    }

    private static void go(final ClientTestContext ctx, final ServerLevel level, final Vec3 at) {
        final ServerPlayer player = ctx.serverPlayer();
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.teleportTo(level, at.x, at.y, at.z, 180F, 35F);
    }

    /*
     * Fails the test when the missing texture's magenta shows in the middle of the view, more than a few pixels of it:
     * something there drawn without its art.
     */
    static void expectArtDrawn(final ClientTestContext ctx, final String what) {
        final double missing = missingArt(ctx);
        if (missing > MISSING_ART) {
            throw new ClientTestFailure(what + " are drawn with their own art, not the missing texture's magenta: "
                    + missing + " of the view's middle is magenta");
        }
    }

    /* How much of the middle of the view is the missing texture's magenta, from 0 to 1. */
    private static double missingArt(final ClientTestContext ctx) {
        final int width = ctx.mc().getWindow().getGuiScaledWidth();
        final int height = ctx.mc().getWindow().getGuiScaledHeight();
        int magenta = 0;
        int all = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int y = height / 4; y < height * 3 / 4; y++) {
                for (int x = width / 4; x < width * 3 / 4; x++) {
                    final int rgb = frame.pixel(x, y);
                    final int r = rgb >> 16 & 0xFF;
                    final int g = rgb >> 8 & 0xFF;
                    final int b = rgb & 0xFF;
                    magenta += r > MAGENTA_HIGH && b > MAGENTA_HIGH && g < MAGENTA_LOW ? 1 : 0;
                    all++;
                }
            }
        }
        return (double) magenta / all;
    }

    private record Spot(String ore, int x, int z, int layout, int turn) {
    }
}
