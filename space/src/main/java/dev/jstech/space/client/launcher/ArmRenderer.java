/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.launcher;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.space.client.skin.Picture;
import dev.jstech.space.client.skin.SkinAtlas;
import dev.jstech.space.client.skin.SkinDrawing;
import dev.jstech.space.launcher.ArmBlockEntity;
import dev.jstech.space.launcher.LauncherBlocks;
import dev.jstech.space.rocket.RocketKitBlocks;
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
 * Draws an arm out from a formed umbilical tower from its end at the rocket, as the approved launcher draws it: a
 * swing arm's grey beam with its stiffeners and the hazard band by the tower, the hinge's column at the tower's face,
 * its blue and orange lines along its top (and the white one to an Umbilical Plate) and the round plug that meets the
 * rocket, its face yellow; the crew access arm's grated walkway with its yellow rails and posts, and the white room
 * at its end by the hatch. It is drawn in the arm's own space: back along it to the tower, up, and across it.
 */
public final class ArmRenderer implements BlockEntityRenderer<ArmBlockEntity> {

    /* The skins, by name. */
    private static final String SIDE = "side";
    private static final String PLATE = "plate";
    private static final String WALKWAY = "walkway";
    private static final String ROOM_END = "room_end";
    private static final String ROOM_SIDE = "room_side";
    private static final String ROOM_TOP = "room_top";
    /* Where the rocket's side is, from this end's middle, and the tower's face past the arm's last block. */
    private static final float TIP = -0.5F;
    private static final float FACE_PAST = 0.5F;
    private static final float MIDDLE = 0.5F;
    /* A swing arm: its beam, its hinge's column, its lines and its plug, from the rocket's side and tower's face. */
    private static final float BEAM_FROM = 0.35F;
    private static final float BEAM_PAST = 0.1F;
    private static final float BEAM_HALF = 0.3F;
    private static final float BEAM_ACROSS = 0.35F;
    private static final float PLATE_ACROSS = 0.7F;
    private static final float HINGE_HALF = 0.7F;
    private static final float HINGE_ACROSS = 0.55F;
    private static final float HINGE = 0.22F;
    private static final int HINGE_ROUND = 12;
    private static final float LINES_OVER = 0.38F;
    private static final float LINE_APART = 0.18F;
    private static final float LINE_TO = 0.45F;
    private static final float LINE = 0.07F;
    private static final int LINE_ROUND = 8;
    private static final float PLUG_FROM = 0.38F;
    private static final float PLUG_TO = 0.02F;
    private static final float PLUG = 0.24F;
    private static final int PLUG_ROUND = 16;
    /* How far the dropped lines hang below the arm. */
    private static final float HANG = 1.2F;
    /* The crew access arm: its walkway, its rails and posts, and the white room at its end. */
    private static final float WALK_FROM = 1.0F;
    private static final float WALK_DEEP = 0.25F;
    private static final float WALK_HALF = 0.6F;
    private static final float RAIL_ACROSS = 0.58F;
    private static final float RAIL_HIGH = 1.0F;
    private static final float RAIL = 0.035F;
    private static final float POST = 0.03F;
    private static final float POST_EVERY = 0.8F;
    private static final int RAIL_ROUND = 6;
    private static final float ROOM_LONG = 1.15F;
    private static final float ROOM_HALF = 0.85F;
    private static final float ROOM_HIGH = 1.7F;
    /* How far it is drawn from, in blocks. */
    private static final int SEEN_FROM = 192;

    public ArmRenderer(final BlockEntityRendererProvider.Context context) {
        // Nothing to keep: the skins are kept by the arm's size.
    }

    /** Hands the mod's bus the renderer, once, from the mod's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event ->
                event.registerBlockEntityRenderer(LauncherBlocks.ARM_ENTITY.get(), ArmRenderer::new));
    }

    @Override
    public void render(final ArmBlockEntity end, final float partialTick, final PoseStack pose,
                       final MultiBufferSource buffers, final int light, final int overlay) {
        final Optional<ArmBlockEntity.Arm> found = end.arm();
        final Level level = end.getLevel();
        if (found.isEmpty() || level == null) {
            return;
        }
        final ArmBlockEntity.Arm arm = found.get();
        final float face = arm.length() - FACE_PAST;
        final Direction back = arm.out().getOpposite();
        if (end.getBlockState().is(LauncherBlocks.CREW_ACCESS_ARM.get())) {
            final float walkFrom = TIP + WALK_FROM;
            final float walkTo = face + BEAM_PAST;
            final SkinAtlas atlas = SkinAtlas.of(new Size(true, arm.length(), 0),
                    () -> crewSkins(walkTo - walkFrom));
            final SkinDrawing drawing = SkinDrawing.in(pose, buffers, atlas, level, end.getBlockPos(),
                    end.getBlockPos(), back);
            crew(drawing, walkFrom, walkTo, face);
        } else {
            final BlockPos tip = end.getBlockPos().relative(arm.out());
            final int[] lines = lines(level, tip);
            final SkinAtlas atlas = SkinAtlas.of(new Size(false, arm.length(), lines.length),
                    () -> swingSkins(face - TIP, lines));
            final SkinDrawing drawing = SkinDrawing.in(pose, buffers, atlas, level, end.getBlockPos(),
                    end.getBlockPos(), back);
            // The rocket gone, its hoses have dropped away: they hang from the arm's end, the plug with them.
            swing(drawing, face, lines, level.getBlockState(tip).isAir());
        }
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(final ArmBlockEntity end) {
        final Optional<ArmBlockEntity.Arm> found = end.arm();
        if (found.isEmpty()) {
            return new AABB(end.getBlockPos());
        }
        final BlockPos tower = end.getBlockPos().relative(found.get().out().getOpposite(), found.get().length());
        // Two blocks down, for the lines hanging once the rocket has gone.
        return new AABB(end.getBlockPos()).minmax(new AABB(tower)).inflate(1, 2, 1);
    }

    @Override
    public int getViewDistance() {
        return SEEN_FROM;
    }

    /*
     * A swing arm from the rocket's side to the tower's {@code face}, carrying {@code lines}; {@code dropped}, the
     * rocket gone, its lines hang down from its end with the plug.
     */
    private static void swing(final SkinDrawing drawing, final float face, final int[] lines,
                              final boolean dropped) {
        final LauncherSkins.Colours c = LauncherSkins.PALETTE.get();
        final float[] min = {TIP + BEAM_FROM, MIDDLE - BEAM_HALF, -BEAM_ACROSS};
        final float[] max = {face + BEAM_PAST, MIDDLE + BEAM_HALF, BEAM_ACROSS};
        // Each face's skin laid from its own corner, sixteen texels a block, as the approved arm's are.
        drawing.box(min, max, each -> SkinDrawing.Paint.framed(each == SkinDrawing.Face.PLUS_Z
                || each == SkinDrawing.Face.MINUS_Z ? SIDE : PLATE, min, max));
        drawing.rod(new Vector3f(face + BEAM_PAST, MIDDLE - HINGE_HALF, HINGE_ACROSS),
                new Vector3f(face + BEAM_PAST, MIDDLE + HINGE_HALF, HINGE_ACROSS), HINGE, HINGE_ROUND, c.hinge());
        final float end = dropped ? TIP + PLUG_TO : TIP + LINE_TO;
        for (int k = 0; k < lines.length; k++) {
            final float across = -LINE_APART + k * LINE_APART;
            drawing.rod(new Vector3f(face, MIDDLE + LINES_OVER, across),
                    new Vector3f(end, MIDDLE + LINES_OVER, across), LINE, LINE_ROUND, lines[k]);
            if (dropped) {
                drawing.rod(new Vector3f(end, MIDDLE + LINES_OVER, across),
                        new Vector3f(end, MIDDLE - HANG, across), LINE, LINE_ROUND, lines[k]);
            }
        }
        if (dropped) {
            final float plug = MIDDLE - HANG;
            drawing.capped(new Vector3f(end, plug, 0), new Vector3f(end, plug - (PLUG_FROM - PLUG_TO), 0), PLUG,
                    PLUG_ROUND, c.armLit(), c.yellow());
        } else {
            drawing.capped(new Vector3f(TIP + PLUG_FROM, MIDDLE, 0), new Vector3f(TIP + PLUG_TO, MIDDLE, 0), PLUG,
                    PLUG_ROUND, c.armLit(), c.yellow());
        }
    }

    /* The crew access arm: its walkway from {@code from} to {@code to}, its rails to the tower's {@code face}. */
    private static void crew(final SkinDrawing drawing, final float from, final float to, final float face) {
        final LauncherSkins.Colours c = LauncherSkins.PALETTE.get();
        final SkinDrawing.Paint edge = SkinDrawing.Paint.tile(c.steelDark());
        final SkinDrawing.Paint walkway = SkinDrawing.Paint.skin(WALKWAY);
        drawing.box(new float[] {from, 0, -WALK_HALF}, new float[] {to, WALK_DEEP, WALK_HALF},
                each -> each == SkinDrawing.Face.PLUS_Y ? walkway : edge);
        for (final float across : new float[] {-RAIL_ACROSS, RAIL_ACROSS}) {
            drawing.rod(new Vector3f(from, WALK_DEEP + RAIL_HIGH, across), new Vector3f(face, WALK_DEEP + RAIL_HIGH,
                    across), RAIL, RAIL_ROUND, c.yellow());
            for (float at = from; at < face; at += POST_EVERY) {
                drawing.rod(new Vector3f(at, WALK_DEEP, across), new Vector3f(at, WALK_DEEP + RAIL_HIGH, across),
                        POST, RAIL_ROUND, c.yellow());
            }
        }
        drawing.box(new float[] {TIP, WALK_DEEP, -ROOM_HALF}, new float[] {TIP + ROOM_LONG, WALK_DEEP + ROOM_HIGH,
                ROOM_HALF}, each -> SkinDrawing.Paint.skin(switch (each) {
                    case PLUS_X, MINUS_X -> ROOM_END;
                    case PLUS_Z, MINUS_Z -> ROOM_SIDE;
                    case PLUS_Y, MINUS_Y -> ROOM_TOP;
                }));
    }

    /*
     * The lines a swing arm carries, by what it meets at {@code tip}: blue and orange to a tank, and the white one too
     * to an Umbilical Plate, whether the rocket's part there has formed or not.
     */
    private static int[] lines(final Level level, final BlockPos tip) {
        final LauncherSkins.Colours c = LauncherSkins.PALETTE.get();
        final boolean plate = FormedPartBlock.originalAt(level, tip, level.getBlockState(tip))
                .is(RocketKitBlocks.UMBILICAL_PLATE.get());
        return plate ? new int[] {c.lineBlue(), c.lineOrange(), c.lineWhite()}
                : new int[] {c.lineBlue(), c.lineOrange()};
    }

    /* A swing arm's skins, {@code length} from the rocket's side to the tower's face, carrying {@code lines}. */
    private static Map<String, Picture> swingSkins(final double length, final int[] lines) {
        final Map<String, Picture> skins = new HashMap<>();
        skins.put(SIDE, LauncherSkins.armSide(length, 2 * BEAM_HALF, lines));
        skins.put(PLATE, LauncherSkins.plate(length, PLATE_ACROSS, LauncherSkins.PALETTE.get().armGrey()));
        return skins;
    }

    /* The crew access arm's skins, its walkway {@code walk} long. */
    private static Map<String, Picture> crewSkins(final double walk) {
        final Map<String, Picture> skins = new HashMap<>();
        skins.put(WALKWAY, LauncherSkins.grating(walk, 2 * WALK_HALF));
        skins.put(ROOM_END, LauncherSkins.whiteRoom(2 * ROOM_HALF, ROOM_HIGH));
        skins.put(ROOM_SIDE, LauncherSkins.whiteRoom(ROOM_LONG, ROOM_HIGH));
        skins.put(ROOM_TOP, LauncherSkins.whiteRoom(ROOM_LONG, 2 * ROOM_HALF));
        return skins;
    }

    /* An arm's size, which its skins are painted to: the crew's or a swing arm, its length, its lines. */
    private record Size(boolean crew, int length, int lines) {
    }
}
