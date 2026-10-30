/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.launcher;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.space.launcher.CraneBlockEntity;
import dev.jstech.space.launcher.LauncherBlocks;
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
 * Draws the hammerhead crane on a formed umbilical tower's top from its boom's tip, as the approved launcher draws it:
 * the red house over the tower's top, a little wider than the tower, its louvres; the red girder of the boom on the
 * house, from just past the rocket's far side to a little past the tower's back, its web's members on its faces; the
 * cable hanging from the boom just past the rocket's side, the yellow hook on it; and the red aviation lamps on the
 * house's corners over the tower's. It is drawn in the boom's own space: along it toward the tower, up, and across.
 */
public final class CraneRenderer implements BlockEntityRenderer<CraneBlockEntity> {

    /* The skins, by name. */
    private static final String HOUSE_END = "house_end";
    private static final String HOUSE_SIDE = "house_side";
    private static final String HOUSE_TOP = "house_top";
    private static final String BOOM_SIDE = "boom_side";
    private static final String BOOM_TOP = "boom_top";
    private static final String BOOM_END = "boom_end";
    /* The tower's half-width to its outsides; how far past them the house reaches, and how tall it is. */
    private static final float TOWER_HALF = 2.5F;
    private static final float HOUSE_OVER = 0.2F;
    private static final float HOUSE_HIGH = 1.4F;
    /* The boom, from the bottom of its own row of frames: its foot and top, half its width, and its ends. */
    private static final float BOOM_FOOT = 0.6F;
    private static final float BOOM_TOP_AT = 1.6F;
    private static final float BOOM_HALF = 0.45F;
    private static final float BOOM_FROM = -0.2F;
    private static final float BOOM_PAST = 0.6F;
    /* The cable and the hook, out along the boom from its tip, under it. */
    private static final float CABLE_AT = 1.4F;
    private static final float CABLE_TO = -2.6F;
    private static final float CABLE = 0.035F;
    private static final int CABLE_ROUND = 6;
    private static final float HOOK_HALF = 0.35F;
    private static final float HOOK_ACROSS = 0.3F;
    private static final float HOOK_FOOT = -3.2F;
    /* The aviation lamps on the house's corners. */
    private static final float LAMP_FOOT = 0.4F;
    private static final float LAMP_TOP = 0.65F;
    private static final float LAMP = 0.09F;
    private static final int LAMP_ROUND = 8;
    private static final float HALF = 0.5F;
    /* How far it is drawn from, in blocks: as far as the tower is seen. */
    private static final int SEEN_FROM = 256;

    public CraneRenderer(final BlockEntityRendererProvider.Context context) {
        // Nothing to keep: the skins are kept by the crane's size.
    }

    /** Hands the mod's bus the renderer, once, from the mod's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event ->
                event.registerBlockEntityRenderer(LauncherBlocks.CRANE_ENTITY.get(), CraneRenderer::new));
    }

    @Override
    public void render(final CraneBlockEntity tip, final float partialTick, final PoseStack pose,
                       final MultiBufferSource buffers, final int light, final int overlay) {
        final Optional<CraneBlockEntity.Crane> found = tip.crane();
        final Level level = tip.getLevel();
        if (found.isEmpty() || level == null || found.get().near() < 0) {
            return;
        }
        final CraneBlockEntity.Crane crane = found.get();
        final float houseFrom = crane.near() - HALF - HOUSE_OVER;
        final float houseTo = crane.far() + HALF + HOUSE_OVER;
        final float houseHalf = TOWER_HALF + HOUSE_OVER;
        final float boomTo = crane.back() + BOOM_PAST;
        final LauncherAtlas atlas = LauncherAtlas.of(new Size(crane.near(), crane.far(), crane.back()),
                () -> skins(houseTo - houseFrom, 2 * houseHalf, boomTo - BOOM_FROM));
        final SkinDrawing drawing = SkinDrawing.in(pose, buffers, atlas, level, tip.getBlockPos(), tip.getBlockPos(),
                crane.tower());
        final LauncherSkins.Colours c = LauncherSkins.PALETTE.get();
        drawing.box(new float[] {houseFrom, -1, -houseHalf}, new float[] {houseTo, HOUSE_HIGH - 1, houseHalf},
                face -> SkinDrawing.Paint.skin(switch (face) {
                    case PLUS_X, MINUS_X -> HOUSE_END;
                    case PLUS_Z, MINUS_Z -> HOUSE_SIDE;
                    case PLUS_Y, MINUS_Y -> HOUSE_TOP;
                }));
        drawing.box(new float[] {BOOM_FROM, BOOM_FOOT, -BOOM_HALF}, new float[] {boomTo, BOOM_TOP_AT, BOOM_HALF},
                face -> SkinDrawing.Paint.skin(switch (face) {
                    case PLUS_X, MINUS_X -> BOOM_END;
                    case PLUS_Z, MINUS_Z -> BOOM_SIDE;
                    case PLUS_Y, MINUS_Y -> BOOM_TOP;
                }));
        drawing.rod(new Vector3f(CABLE_AT, BOOM_FOOT, 0), new Vector3f(CABLE_AT, CABLE_TO, 0), CABLE, CABLE_ROUND,
                c.cable());
        final SkinDrawing.Paint hook = SkinDrawing.Paint.tile(c.yellow());
        drawing.box(new float[] {CABLE_AT - HOOK_HALF, HOOK_FOOT, -HOOK_ACROSS},
                new float[] {CABLE_AT + HOOK_HALF, CABLE_TO, HOOK_ACROSS}, face -> hook);
        for (final float u : new float[] {crane.near() - HALF, crane.far() + HALF}) {
            for (final float v : new float[] {-TOWER_HALF, TOWER_HALF}) {
                drawing.rod(new Vector3f(u, LAMP_FOOT, v), new Vector3f(u, LAMP_TOP, v), LAMP, LAMP_ROUND,
                        c.aviation());
            }
        }
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(final CraneBlockEntity tip) {
        final Optional<CraneBlockEntity.Crane> found = tip.crane();
        if (found.isEmpty()) {
            return new AABB(tip.getBlockPos());
        }
        final Direction along = found.get().tower();
        final BlockPos end = tip.getBlockPos().relative(along, found.get().back() + 1);
        return new AABB(tip.getBlockPos().relative(along.getOpposite())).minmax(new AABB(end))
                .inflate(along.getAxis() == Direction.Axis.X ? 0 : TOWER_HALF + 1, 0,
                        along.getAxis() == Direction.Axis.Z ? 0 : TOWER_HALF + 1)
                .expandTowards(0, HOOK_FOOT - 1, 0).expandTowards(0, 2, 0);
    }

    @Override
    public int getViewDistance() {
        return SEEN_FROM;
    }

    /* The crane's skins: its house {@code long} along the boom and {@code wide} across, its boom {@code boom} long. */
    private static Map<String, LauncherSkins.Picture> skins(final double houseLong, final double wide,
                                                            final double boom) {
        final Map<String, LauncherSkins.Picture> skins = new HashMap<>();
        skins.put(HOUSE_END, LauncherSkins.craneHouse(wide, HOUSE_HIGH));
        skins.put(HOUSE_SIDE, LauncherSkins.craneHouse(houseLong, HOUSE_HIGH));
        skins.put(HOUSE_TOP, LauncherSkins.craneHouse(houseLong, wide));
        skins.put(BOOM_SIDE, LauncherSkins.boom(boom, BOOM_TOP_AT - BOOM_FOOT));
        skins.put(BOOM_TOP, LauncherSkins.boom(boom, 2 * BOOM_HALF));
        skins.put(BOOM_END, LauncherSkins.boom(2 * BOOM_HALF, BOOM_TOP_AT - BOOM_FOOT));
        return skins;
    }

    /* A crane's size, which its skins are painted to. */
    private record Size(int near, int far, int back) {
    }
}
