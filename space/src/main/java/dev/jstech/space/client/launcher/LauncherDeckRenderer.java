/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.launcher;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.space.client.skin.Picture;
import dev.jstech.space.client.skin.SkinAtlas;
import dev.jstech.space.client.skin.SkinDrawing;
import dev.jstech.space.launcher.LauncherBlocks;
import dev.jstech.space.launcher.LauncherDeckBlockEntity;
import dev.jstech.space.launcher.LauncherLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Vector3f;

/**
 * Draws a formed mobile launcher's deck from the hold-down arm that measured it, as the approved launcher draws it:
 * the deck round its exhaust opening, its top and its sides each one picture over the whole of it, the opening's
 * scorched lining, the trim lip round its top edge, the grating across the opening's foot; at each corner of the
 * opening a hold-down arm's housing, its jaw reaching in to the engine's foot and its chrome cylinder; and the tail
 * service masts round it, each a column with its hood leaning in over the rocket's tail and its blue, orange and white
 * lines. It is drawn in the deck's own space: along it toward the tower, up, and across it to the tower's right.
 */
public final class LauncherDeckRenderer implements BlockEntityRenderer<LauncherDeckBlockEntity> {

    private static final float HALF = 0.5F;
    /* The deck's skins, by name. */
    private static final String TOP = "top";
    private static final String FRONT = "front";
    private static final String BACK = "back";
    private static final String RIGHT = "right";
    private static final String LEFT = "left";
    private static final String LINING = "lining";
    private static final String GRATING = "grating";
    private static final String HOUSING_TOP = "housing_top";
    private static final String HOUSING_SIDE = "housing_side";
    private static final String MAST_TOP = "mast_top";
    private static final String MAST_SIDE = "mast_side";
    /* The seeds the approved launcher lays each side's panels by: toward the tower, away, its right and its left. */
    private static final int SEED_FRONT = 3;
    private static final int SEED_BACK = 4;
    private static final int SEED_RIGHT = 2;
    private static final int SEED_LEFT = 5;
    /* The lip round the deck's top edge: how deep, how far out and in, and a hair over the deck so it shows. */
    private static final float LIP_DEEP = 0.25F;
    private static final float LIP_OUT = 0.05F;
    private static final float LIP_IN = 0.1F;
    private static final float LIP_PROUD = 0.004F;
    /* The grating across the opening's foot, from the deck's underside. */
    private static final float GRATING_FROM = 0.2F;
    private static final float GRATING_TO = 0.35F;
    /* A hold-down arm: its housing, out from the opening's edge, and its jaw, in from it. */
    private static final float HOUSING_OUT = 0.55F;
    private static final float HOUSING_HALF = 0.45F;
    private static final float HOUSING_HIGH = 0.7F;
    private static final float JAW_IN = 0.8F;
    private static final float JAW_FROM = 0.45F;
    private static final float JAW_AT = 0.35F;
    private static final float JAW_WIDE = 0.26F;
    private static final float CYLINDER_FROM = 0.75F;
    private static final float CYLINDER_OVER = 0.25F;
    private static final float CYLINDER = 0.07F;
    /* A mast: its column, its hood and the lines down it and in from the hood. */
    private static final float MAST_HALF = 0.4F;
    private static final float MAST_HIGH = (float) LauncherLayout.MAST_HIGH;
    private static final float HOOD_AT = 2.4F;
    private static final float HOOD_IN = 1.0F;
    private static final float HOOD_WIDE = 0.6F;
    private static final float HOOD_HIGH = 0.45F;
    private static final float LINE = 0.06F;
    private static final float LINE_APART = 0.16F;
    private static final float LINE_OUT = 0.42F;
    private static final float LINE_FROM = 0.1F;
    private static final float LINE_TO = 2.3F;
    private static final float LINE_IN = 0.45F;
    private static final float LINE_RISE = 0.2F;
    private static final int ROUND = 8;
    /* How far it is drawn from, in blocks: as far as the tower over it is seen. */
    private static final int SEEN_FROM = 192;

    public LauncherDeckRenderer(final BlockEntityRendererProvider.Context context) {
        // Nothing to keep: the skins are kept by the deck's size.
    }

    /** Hands the mod's bus the renderer, once, from the mod's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event ->
                event.registerBlockEntityRenderer(LauncherBlocks.HOLD_DOWN_ENTITY.get(), LauncherDeckRenderer::new));
    }

    @Override
    public void render(final LauncherDeckBlockEntity arm, final float partialTick, final PoseStack pose,
                       final MultiBufferSource buffers, final int light, final int overlay) {
        final Optional<LauncherDeckBlockEntity.Deck> found = arm.deck();
        final Level level = arm.getLevel();
        if (found.isEmpty() || level == null) {
            return;
        }
        final LauncherDeckBlockEntity.Deck deck = found.get();
        final SkinAtlas atlas = SkinAtlas.of(new Size(deck.open(), deck.back(), deck.front(), deck.across()),
                () -> skins(deck));
        final SkinDrawing drawing = SkinDrawing.in(pose, buffers, atlas, level, arm.getBlockPos(),
                arm.getBlockPos().offset(deck.axis()), deck.side());
        final LauncherSkins.Colours c = LauncherSkins.PALETTE.get();
        deck(drawing, deck, c);
        holdDowns(drawing, deck.open() + HALF, c);
        masts(drawing, deck.open(), c);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(final LauncherDeckBlockEntity arm) {
        final Optional<LauncherDeckBlockEntity.Deck> found = arm.deck();
        if (found.isEmpty()) {
            return new AABB(arm.getBlockPos());
        }
        final LauncherDeckBlockEntity.Deck deck = found.get();
        final BlockPos axis = arm.getBlockPos().offset(deck.axis());
        final Direction side = deck.side();
        final Direction across = side.getClockWise();
        AABB box = null;
        for (final int u : new int[] {-deck.back() - 1, deck.front() + 1}) {
            for (final int v : new int[] {-deck.across() - 1, deck.across() + 1}) {
                final BlockPos corner = axis.relative(side, u).relative(across, v);
                final AABB one = new AABB(corner).expandTowards(0, MAST_HIGH + 1, 0)
                        .expandTowards(0, -LauncherLayout.DECK, 0);
                box = box == null ? one : box.minmax(one);
            }
        }
        return box;
    }

    @Override
    public int getViewDistance() {
        return SEEN_FROM;
    }

    /* The deck round its opening, its lining, its lip and the grating across the opening's foot. */
    private static void deck(final SkinDrawing drawing, final LauncherDeckBlockEntity.Deck deck,
                             final LauncherSkins.Colours c) {
        final float h = deck.open() + HALF;
        final float x0 = -deck.back() - HALF;
        final float x1 = deck.front() + HALF;
        final float z0 = -deck.across() - HALF;
        final float z1 = deck.across() + HALF;
        final float bottom = -LauncherLayout.DECK;
        final float[] min = {x0, bottom, z0};
        final float[] max = {x1, 0, z1};
        final SkinDrawing.Paint under = SkinDrawing.Paint.tile(c.underside());
        // Its four pieces round the opening, their outer faces laid as the whole deck's, the faces between them left.
        final float[][][] pieces = {{{x0, bottom, z0}, {-h, 0, z1}}, {{h, bottom, z0}, {x1, 0, z1}},
                {{-h, bottom, z0}, {h, 0, -h}}, {{-h, bottom, h}, {h, 0, z1}}};
        for (final float[][] piece : pieces) {
            drawing.box(piece[0], piece[1], face -> {
                final boolean outer = switch (face) {
                    case PLUS_X -> piece[1][0] == x1;
                    case MINUS_X -> piece[0][0] == x0;
                    case PLUS_Z -> piece[1][2] == z1;
                    case MINUS_Z -> piece[0][2] == z0;
                    default -> true;
                };
                if (!outer) {
                    return null;
                }
                return switch (face) {
                    case PLUS_Y -> SkinDrawing.Paint.framed(TOP, min, max);
                    case MINUS_Y -> under;
                    case PLUS_X -> SkinDrawing.Paint.framed(FRONT, min, max);
                    case MINUS_X -> SkinDrawing.Paint.framed(BACK, min, max);
                    case PLUS_Z -> SkinDrawing.Paint.framed(RIGHT, min, max);
                    case MINUS_Z -> SkinDrawing.Paint.framed(LEFT, min, max);
                };
            });
        }
        // The opening's four walls, each its scorched lining laid once across it.
        final SkinDrawing.Paint lining = SkinDrawing.Paint.skin(LINING);
        only(drawing, new float[] {x0, bottom, -h}, new float[] {-h, 0, h}, SkinDrawing.Face.PLUS_X, lining);
        only(drawing, new float[] {h, bottom, -h}, new float[] {x1, 0, h}, SkinDrawing.Face.MINUS_X, lining);
        only(drawing, new float[] {-h, bottom, z0}, new float[] {h, 0, -h}, SkinDrawing.Face.PLUS_Z, lining);
        only(drawing, new float[] {-h, bottom, h}, new float[] {h, 0, z1}, SkinDrawing.Face.MINUS_Z, lining);
        final SkinDrawing.Paint lip = SkinDrawing.Paint.tile(c.lip());
        final float lipTop = LIP_PROUD;
        final float lipFoot = -LIP_DEEP;
        for (final float[][] edge : new float[][][] {
                {{x0 - LIP_OUT, lipFoot, z0 - LIP_OUT}, {x1 + LIP_OUT, lipTop, z0 + LIP_IN}},
                {{x0 - LIP_OUT, lipFoot, z1 - LIP_IN}, {x1 + LIP_OUT, lipTop, z1 + LIP_OUT}},
                {{x0 - LIP_OUT, lipFoot, z0}, {x0 + LIP_IN, lipTop, z1}},
                {{x1 - LIP_IN, lipFoot, z0}, {x1 + LIP_OUT, lipTop, z1}}}) {
            drawing.box(edge[0], edge[1], face -> lip);
        }
        final SkinDrawing.Paint frame = SkinDrawing.Paint.tile(c.gratingFrame());
        final SkinDrawing.Paint grating = SkinDrawing.Paint.skin(GRATING);
        drawing.box(new float[] {-h, bottom + GRATING_FROM, -h}, new float[] {h, bottom + GRATING_TO, h},
                face -> face == SkinDrawing.Face.PLUS_Y ? grating : frame);
    }

    /* A hold-down arm at each corner of the opening, whose half-width is {@code h}. */
    private static void holdDowns(final SkinDrawing drawing, final float h, final LauncherSkins.Colours c) {
        final SkinDrawing.Paint top = SkinDrawing.Paint.skin(HOUSING_TOP);
        final SkinDrawing.Paint side = SkinDrawing.Paint.skin(HOUSING_SIDE);
        for (final int sx : new int[] {-1, 1}) {
            for (final int sz : new int[] {-1, 1}) {
                final float cx = sx * (h + HOUSING_OUT);
                final float cz = sz * (h + HOUSING_OUT);
                drawing.box(new float[] {cx - HOUSING_HALF, 0, cz - HOUSING_HALF},
                        new float[] {cx + HOUSING_HALF, HOUSING_HIGH, cz + HOUSING_HALF},
                        face -> face == SkinDrawing.Face.PLUS_Y ? top : side);
                final Vector3f jaw = new Vector3f(sx * (h - JAW_IN), JAW_AT, sz * (h - JAW_IN));
                drawing.member(new Vector3f(cx, JAW_FROM, cz), jaw, JAW_WIDE, JAW_WIDE, c.jaw());
                drawing.rod(new Vector3f(cx, CYLINDER_FROM, cz), new Vector3f(jaw).add(0, CYLINDER_OVER, 0), CYLINDER,
                        ROUND, c.chrome());
            }
        }
    }

    /* The tail service masts round the opening of {@code open}, where the deck's own mast blocks stand. */
    private static void masts(final SkinDrawing drawing, final int open, final LauncherSkins.Colours c) {
        final SkinDrawing.Paint top = SkinDrawing.Paint.skin(MAST_TOP);
        final SkinDrawing.Paint side = SkinDrawing.Paint.skin(MAST_SIDE);
        final int[] lines = {c.lineBlue(), c.lineOrange(), c.lineWhite()};
        for (final double degrees : LauncherLayout.MAST_ANGLES) {
            final int[] at = LauncherLayout.mast(open, degrees);
            final Vector3f foot = new Vector3f(at[0], 0, at[1]);
            final Vector3f out = new Vector3f((float) Math.cos(Math.toRadians(degrees)), 0,
                    (float) Math.sin(Math.toRadians(degrees)));
            drawing.box(new float[] {foot.x() - MAST_HALF, 0, foot.z() - MAST_HALF},
                    new float[] {foot.x() + MAST_HALF, MAST_HIGH, foot.z() + MAST_HALF},
                    face -> face == SkinDrawing.Face.PLUS_Y ? top : side);
            final Vector3f hood = new Vector3f(foot).add(0, HOOD_AT, 0);
            final Vector3f hoodTo = new Vector3f(hood).sub(new Vector3f(out).mul(HOOD_IN));
            drawing.member(hood, hoodTo, HOOD_WIDE, HOOD_HIGH, c.hood());
            final Vector3f beside = new Vector3f(-out.z(), 0, out.x());
            for (int k = 0; k < lines.length; k++) {
                final Vector3f off = new Vector3f(beside).mul((k - 1) * LINE_APART);
                final Vector3f down = new Vector3f(foot).add(off).add(new Vector3f(out).mul(LINE_OUT));
                drawing.rod(new Vector3f(down).add(0, LINE_FROM, 0), new Vector3f(down).add(0, LINE_TO, 0), LINE,
                        ROUND, lines[k]);
                final Vector3f in = new Vector3f(hoodTo).add(off);
                drawing.rod(in, new Vector3f(in).sub(new Vector3f(out).mul(LINE_IN)).add(0, LINE_RISE, 0), LINE,
                        ROUND, lines[k]);
            }
        }
    }

    /* Every skin of {@code deck}, painted to the size of its faces. */
    private static Map<String, Picture> skins(final LauncherDeckBlockEntity.Deck deck) {
        final double length = deck.back() + deck.front() + 1;
        final double wide = 2 * deck.across() + 1;
        final double opening = 2 * deck.open() + 1;
        final double[] hole = {deck.back() - deck.open(), deck.across() - deck.open(),
                deck.back() + deck.open() + 1, deck.across() + deck.open() + 1};
        final Map<String, Picture> skins = new HashMap<>();
        skins.put(TOP, LauncherSkins.deckTop(length, wide, hole));
        skins.put(FRONT, LauncherSkins.deckSide(wide, LauncherLayout.DECK, SEED_FRONT));
        skins.put(BACK, LauncherSkins.deckSide(wide, LauncherLayout.DECK, SEED_BACK));
        skins.put(RIGHT, LauncherSkins.deckSide(length, LauncherLayout.DECK, SEED_RIGHT));
        skins.put(LEFT, LauncherSkins.deckSide(length, LauncherLayout.DECK, SEED_LEFT));
        skins.put(LINING, LauncherSkins.lining(opening, LauncherLayout.DECK));
        skins.put(GRATING, LauncherSkins.grating(opening, opening));
        skins.put(HOUSING_TOP, LauncherSkins.plate(2 * HOUSING_HALF, 2 * HOUSING_HALF));
        skins.put(HOUSING_SIDE, LauncherSkins.panel(2 * HOUSING_HALF, HOUSING_HIGH));
        skins.put(MAST_TOP, LauncherSkins.plate(2 * MAST_HALF, 2 * MAST_HALF));
        skins.put(MAST_SIDE, LauncherSkins.panel(2 * MAST_HALF, MAST_HIGH));
        return skins;
    }

    /* The face {@code face} of the box from {@code min} to {@code max}, painted {@code paint}, and nothing else. */
    private static void only(final SkinDrawing drawing, final float[] min, final float[] max,
                             final SkinDrawing.Face face, final SkinDrawing.Paint paint) {
        drawing.box(min, max, each -> each == face ? paint : null);
    }

    /* A deck's size, which its skins are painted to. */
    private record Size(int open, int back, int front, int across) {
    }
}
