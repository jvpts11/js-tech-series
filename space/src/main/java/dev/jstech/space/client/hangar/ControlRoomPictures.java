/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.hangar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.space.JsSpace;
import dev.jstech.space.hangar.HangarStructure;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * The pictures a formed Rocket Hangar's control room lights, as its approved control room has them: the status board
 * on its end wall, the rocket in its stages, how far each is built and the bay's lamps; and the Rocket Design table's
 * blueprint, a rocket drawn on its grid. Both glow, lit from within, over the blocks they lie on.
 */
final class ControlRoomPictures {

    private static final ResourceLocation BOARD = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
            "textures/models/hangar/status_board.png");
    private static final ResourceLocation TABLE = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
            "textures/models/hangar/design_table.png");
    private static final int WHITE = 255;

    private ControlRoomPictures() {
    }

    /** Draws the pictures of the hangar whose terminal is at {@code terminal}, standing as {@code turn} says. */
    static void draw(final BlockPos terminal, final Orientation turn, final PoseStack pose,
                     final MultiBufferSource buffers) {
        // The board faces south, into the room: its west edge on the left as it is looked at, its top up.
        final double[] board = HangarStructure.board();
        final Vec3 boardNormal = turned(turn, 0, 0, 1);
        quad(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(BOARD)), boardNormal,
                at(terminal, turn, board[0], board[3], board[4]), at(terminal, turn, board[0], board[1], board[4]),
                at(terminal, turn, board[2], board[1], board[4]), at(terminal, turn, board[2], board[3], board[4]));
        // The blueprint lies on the table's top, its length along the table, the rocket's nose to the south.
        final double[] table = HangarStructure.table();
        final Vec3 tableNormal = turned(turn, 0, 1, 0);
        quad(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TABLE)), tableNormal,
                at(terminal, turn, table[0], table[4], table[1]), at(terminal, turn, table[2], table[4], table[1]),
                at(terminal, turn, table[2], table[4], table[3]), at(terminal, turn, table[0], table[4], table[3]));
    }

    /* The pattern's point ({@code x}, {@code y}, {@code z}), from the terminal's block. */
    private static Vec3 at(final BlockPos terminal, final Orientation turn, final double x, final double y,
                           final double z) {
        return HangarStructure.point(terminal, turn, x, y, z).subtract(terminal.getX(), terminal.getY(),
                terminal.getZ());
    }

    /* The pattern's way ({@code x}, {@code y}, {@code z}) turned as the hangar stands. */
    private static Vec3 turned(final Orientation turn, final int x, final int y, final int z) {
        final int[] way = turn.apply(x, y, z);
        return new Vec3(way[0], way[1], way[2]);
    }

    /*
     * A picture over the four corners {@code a} (its top left), {@code b} (bottom left), {@code c} (bottom right) and
     * {@code d} (top right), lit from within.
     */
    private static void quad(final PoseStack pose, final VertexConsumer to, final Vec3 normal, final Vec3 a,
                             final Vec3 b, final Vec3 c, final Vec3 d) {
        vertex(pose, to, normal, a, 0, 0);
        vertex(pose, to, normal, b, 0, 1);
        vertex(pose, to, normal, c, 1, 1);
        vertex(pose, to, normal, d, 1, 0);
    }

    private static void vertex(final PoseStack pose, final VertexConsumer to, final Vec3 normal, final Vec3 at,
                               final float u, final float v) {
        to.addVertex(pose.last(), (float) at.x, (float) at.y, (float) at.z).setColor(WHITE, WHITE, WHITE, WHITE)
                .setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose.last(), (float) normal.x, (float) normal.y, (float) normal.z);
    }
}
