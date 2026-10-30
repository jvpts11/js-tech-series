/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.hangar;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.space.client.skin.Picture;
import dev.jstech.space.client.skin.SkinAtlas;
import dev.jstech.space.client.skin.SkinDrawing;
import dev.jstech.space.hangar.HangarBay;
import dev.jstech.space.hangar.HangarPieceBlock;
import dev.jstech.space.hangar.HangarTerminalBlockEntity;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/**
 * Draws a formed Rocket Hangar's crane and work platforms whole, as the approved hangar has them. The crane: a yellow
 * box girder a block square along each of its two walls, its steel rail on top and a bracket under it every eight
 * blocks; the bridge's taller girder on the rails from wall to wall, two blocks wide; the hoist's trolley of steel
 * plates hanging under the bridge, its drum inside. A work platform: its deck, a grated plate over an underframe of
 * I-beams with hazard stripes round its edge; along the edge it faces, a hazard band under it, the yellow posts and
 * the rail; and two struts down from under it to the side away from that edge.
 */
final class HangarPieces {

    /* The crane's skins, by name. */
    private static final String RAIL = "rail";
    private static final String BRIDGE = "bridge";
    private static final String HOIST_SIDE = "hoist_side";
    private static final String HOIST_TOP = "hoist_top";
    /* A platform's skins, by name. */
    private static final String DECK = "deck";
    private static final String UNDER = "under";
    private static final String EDGE = "edge";
    private static final String BAND = "band";
    private static final float HALF = 0.5F;
    /* The rails: their girders a block square, the steel rail on top, the brackets under them. */
    private static final float STRIP_HIGH = 0.15F;
    private static final float STRIP_HALF = 0.1F;
    private static final float BRACKET_FIRST = 3;
    private static final float BRACKET_EVERY = 8;
    private static final float BRACKET_HALF = 0.3F;
    private static final float BRACKET_DOWN = 1.5F;
    /* The bridge on the rails, and the hoist under it with its drum. */
    private static final float BRIDGE_FOOT = 1.15F;
    private static final float BRIDGE_TOP = 2.6F;
    private static final float BRIDGE_HALF = 1.0F;
    private static final float HOIST_FOOT = 0.2F;
    private static final float HOIST_HALF = 1.3F;
    private static final float DRUM_AT = 0.7F;
    private static final float DRUM_HALF = 1.0F;
    private static final float DRUM = 0.45F;
    private static final int DRUM_ROUND = 16;
    /* A platform: its deck at its blocks' tops, its edge band, its railing and its struts. */
    private static final float DECK_DEEP = 0.3F;
    private static final float BAND_DEEP = 0.5F;
    private static final float BAND_HALF = 0.08F;
    private static final float RAILING = 1.0F;
    private static final float POST = 0.03F;
    private static final float TOP_RAIL = 0.04F;
    private static final int RAIL_ROUND = 6;
    private static final float STRUT_IN = 0.5F;
    private static final float STRUT_OUT = 2.0F;
    private static final float STRUT_DOWN = 2.7F;
    private static final float STRUT = 0.2F;

    private HangarPieces() {
    }

    /** Draws {@code terminal}'s formed crane and platforms, the pose standing on the terminal's block. */
    static void draw(final HangarTerminalBlockEntity terminal, final Level level, final PoseStack pose,
                     final MultiBufferSource buffers) {
        terminal.bay().ifPresent(bay -> crane(terminal, bay, level, pose, buffers));
        for (final List<BlockPos> deck : decks(level, terminal.platforms())) {
            platform(terminal, deck, level, pose, buffers);
        }
    }

    /* The crane of {@code bay}, drawn along its rails from their first block. */
    private static void crane(final HangarTerminalBlockEntity terminal, final HangarBay bay, final Level level,
                              final PoseStack pose, final MultiBufferSource buffers) {
        final boolean alongX = bay.span() == Direction.Axis.Z;
        final int rails = bay.hoist().getY();
        final BlockPos low = bay.low();
        final BlockPos high = bay.high();
        // Along the rails and across to the far wall's, each from the near wall's first block.
        final Direction along = alongX ? Direction.EAST : Direction.SOUTH;
        final BlockPos origin = alongX ? new BlockPos(low.getX(), rails, low.getZ())
                : new BlockPos(high.getX(), rails, low.getZ());
        final int length = alongX ? bay.width() : bay.depth();
        final int wide = alongX ? bay.depth() : bay.width();
        final float bridge = alongX ? bay.bridge().getX() - low.getX() : bay.bridge().getZ() - low.getZ();
        final float hoist = alongX ? bay.hoist().getZ() - low.getZ() : high.getX() - bay.hoist().getX();
        final SkinAtlas atlas = SkinAtlas.of(new CraneSize(length, wide), () -> craneSkins(length, wide));
        final SkinDrawing drawing = SkinDrawing.in(pose, buffers, atlas, level, terminal.getBlockPos(), origin, along);
        final HangarSkins.Colours c = HangarSkins.PALETTE.get();
        final SkinDrawing.Paint yellow = SkinDrawing.Paint.tile(c.yellow());
        final SkinDrawing.Paint strip = SkinDrawing.Paint.tile(c.steelLit());
        final SkinDrawing.Paint bracket = SkinDrawing.Paint.tile(c.steelDark());
        for (final float row : new float[] {0, wide - 1}) {
            drawing.box(new float[] {-HALF, 0, row - HALF}, new float[] {length - HALF, 1, row + HALF},
                    face -> face == SkinDrawing.Face.PLUS_Z || face == SkinDrawing.Face.MINUS_Z
                            ? SkinDrawing.Paint.skin(RAIL) : yellow);
            drawing.box(new float[] {-HALF, 1, row - STRIP_HALF}, new float[] {length - HALF, 1 + STRIP_HIGH,
                    row + STRIP_HALF}, face -> strip);
            for (float at = -HALF + BRACKET_FIRST; at < length - HALF; at += BRACKET_EVERY) {
                drawing.box(new float[] {at - BRACKET_HALF, -BRACKET_DOWN, row - HALF},
                        new float[] {at + BRACKET_HALF, 0, row + HALF}, face -> bracket);
            }
        }
        drawing.box(new float[] {bridge - BRIDGE_HALF, BRIDGE_FOOT, -HALF},
                new float[] {bridge + BRIDGE_HALF, BRIDGE_TOP, wide - HALF},
                face -> face == SkinDrawing.Face.PLUS_X || face == SkinDrawing.Face.MINUS_X
                        ? SkinDrawing.Paint.skin(BRIDGE) : yellow);
        drawing.box(new float[] {bridge - HOIST_HALF, HOIST_FOOT, hoist - HOIST_HALF},
                new float[] {bridge + HOIST_HALF, BRIDGE_FOOT, hoist + HOIST_HALF},
                face -> SkinDrawing.Paint.skin(face == SkinDrawing.Face.PLUS_Y || face == SkinDrawing.Face.MINUS_Y
                        ? HOIST_TOP : HOIST_SIDE));
        drawing.rod(new Vector3f(bridge, DRUM_AT, hoist - DRUM_HALF), new Vector3f(bridge, DRUM_AT, hoist + DRUM_HALF),
                DRUM, DRUM_ROUND, c.steelDark());
        pose.popPose();
    }

    /*
     * A work platform's deck, its blocks {@code deck}: drawn along the edge it faces from its first block there, its
     * edge on the near side and the side it is not at on the far.
     */
    private static void platform(final HangarTerminalBlockEntity terminal, final List<BlockPos> deck,
                                 final Level level, final PoseStack pose, final MultiBufferSource buffers) {
        final Direction faces = level.getBlockState(deck.getFirst()).getValue(HorizontalDirectionalBlock.FACING);
        final Direction along = faces.getClockWise();
        final Direction away = along.getClockWise();
        int fromAlong = Integer.MAX_VALUE;
        int toAlong = Integer.MIN_VALUE;
        int fromAway = Integer.MAX_VALUE;
        int toAway = Integer.MIN_VALUE;
        for (final BlockPos at : deck) {
            final int a = at.getX() * along.getStepX() + at.getZ() * along.getStepZ();
            final int w = at.getX() * away.getStepX() + at.getZ() * away.getStepZ();
            fromAlong = Math.min(fromAlong, a);
            toAlong = Math.max(toAlong, a);
            fromAway = Math.min(fromAway, w);
            toAway = Math.max(toAway, w);
        }
        final int length = toAlong - fromAlong + 1;
        final int depth = toAway - fromAway + 1;
        // The block with the least along it and away from the edge, which the drawing stands on.
        final BlockPos any = deck.getFirst();
        final int anyAlong = any.getX() * along.getStepX() + any.getZ() * along.getStepZ();
        final int anyAway = any.getX() * away.getStepX() + any.getZ() * away.getStepZ();
        final BlockPos origin = any.relative(along, fromAlong - anyAlong).relative(away, fromAway - anyAway);
        final SkinAtlas atlas = SkinAtlas.of(new DeckSize(length, depth), () -> deckSkins(length, depth));
        final SkinDrawing drawing = SkinDrawing.in(pose, buffers, atlas, level, terminal.getBlockPos(), origin, along);
        final HangarSkins.Colours c = HangarSkins.PALETTE.get();
        final float[] min = {-HALF, 1 - DECK_DEEP, -HALF};
        final float[] max = {length - HALF, 1, depth - HALF};
        drawing.box(min, max, face -> switch (face) {
            case PLUS_Y -> SkinDrawing.Paint.skin(DECK);
            case MINUS_Y -> SkinDrawing.Paint.skin(UNDER);
            default -> SkinDrawing.Paint.framed(EDGE, min, max);
        });
        final float[] bandMin = {-HALF, 1 - BAND_DEEP, -HALF - BAND_HALF};
        final float[] bandMax = {length - HALF, 1, -HALF + BAND_HALF};
        drawing.box(bandMin, bandMax, face -> SkinDrawing.Paint.framed(BAND, bandMin, bandMax));
        for (int k = 0; k <= length; k++) {
            drawing.rod(new Vector3f(k - HALF, 1, -HALF), new Vector3f(k - HALF, 1 + RAILING, -HALF), POST,
                    RAIL_ROUND, c.yellow());
        }
        drawing.rod(new Vector3f(-HALF, 1 + RAILING, -HALF), new Vector3f(length - HALF, 1 + RAILING, -HALF),
                TOP_RAIL, RAIL_ROUND, c.yellow());
        final float middle = (depth - 1) / 2.0F;
        final float wall = depth - HALF;
        final float from = Math.min(middle + STRUT_OUT, wall - 1);
        for (final float at : new float[] {-HALF + STRUT_IN, length - HALF - STRUT_IN}) {
            drawing.member(new Vector3f(at, 1 - DECK_DEEP, from), new Vector3f(at, 1 - DECK_DEEP - STRUT_DOWN, wall),
                    STRUT, STRUT, c.steel());
        }
        pose.popPose();
    }

    /* The decks among {@code platforms}: each run of platforms side by side at one height, facing one way. */
    private static List<List<BlockPos>> decks(final Level level, final List<BlockPos> platforms) {
        final Set<BlockPos> left = new HashSet<>(platforms);
        final List<List<BlockPos>> decks = new ArrayList<>();
        for (final BlockPos start : platforms) {
            if (!left.remove(start) || !(level.getBlockState(start).getBlock() instanceof HangarPieceBlock)) {
                continue;
            }
            final BlockState first = level.getBlockState(start);
            final List<BlockPos> deck = new ArrayList<>();
            final Deque<BlockPos> next = new ArrayDeque<>();
            next.add(start);
            while (!next.isEmpty()) {
                final BlockPos at = next.poll();
                deck.add(at);
                for (final Direction way : Direction.Plane.HORIZONTAL) {
                    final BlockPos beside = at.relative(way);
                    if (left.contains(beside) && level.getBlockState(beside).getValue(
                            HorizontalDirectionalBlock.FACING) == first.getValue(HorizontalDirectionalBlock.FACING)) {
                        left.remove(beside);
                        next.add(beside);
                    }
                }
            }
            decks.add(deck);
        }
        return decks;
    }

    /* The crane's skins: its rails {@code length} long, its bridge {@code wide} across. */
    private static Map<String, Picture> craneSkins(final int length, final int wide) {
        final Map<String, Picture> skins = new HashMap<>();
        skins.put(RAIL, HangarSkins.girder(length, 1));
        skins.put(BRIDGE, HangarSkins.girder(wide, BRIDGE_TOP - BRIDGE_FOOT));
        skins.put(HOIST_SIDE, HangarSkins.plate(2 * HOIST_HALF, BRIDGE_FOOT - HOIST_FOOT));
        skins.put(HOIST_TOP, HangarSkins.plate(2 * HOIST_HALF, 2 * HOIST_HALF));
        return skins;
    }

    /* A deck's skins, {@code length} along its edge and {@code depth} away from it. */
    private static Map<String, Picture> deckSkins(final int length, final int depth) {
        final Map<String, Picture> skins = new HashMap<>();
        skins.put(DECK, HangarSkins.grating(length, depth));
        skins.put(UNDER, HangarSkins.underframe(length, depth));
        skins.put(EDGE, HangarSkins.edge(length, DECK_DEEP));
        skins.put(BAND, HangarSkins.edge(length, BAND_DEEP));
        return skins;
    }

    /* A crane's size, which its skins are painted to. */
    private record CraneSize(int length, int wide) {
    }

    /* A deck's size, which its skins are painted to. */
    private record DeckSize(int length, int depth) {
    }
}
