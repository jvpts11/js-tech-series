/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.monitor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.block.MonitorKind;
import dev.jstech.computers.block.MonitorPanel;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.gui.MonitorGlass;
import dev.jstech.computers.monitor.IMonitorPicture;
import dev.jstech.core.client.live.LiveScreen;
import dev.jstech.core.client.live.LiveScreens;
import dev.jstech.core.gui.Tube;
import dev.jstech.core.live.LiveRate;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Shows a monitor's face in the world: what its machine shows, live, on its glass, as close as {@link LiveRate} lets a
 * viewer read it, and only its light from further off.
 *
 * <p>A monitor alone shows the picture on its own glass. A big screen is drawn whole by its bottom-left monitor: one
 * bezel round all of it, one chin under it, and the picture across its whole glass; the corner monitor's own power
 * button stands out of the chin, a part of its model.
 */
@PaletteHolder
public final class MonitorScreenRenderer implements BlockEntityRenderer<MonitorBlockEntity> {

    /** The bezel and the chin of a big screen of each kind of flat panel, as their single panels wear them. */
    private static final Palette<Bezels> BEZELS = Palettes.declare(JsComputers.MODID, "monitor/bezels", new Bezels(
            0xFF1C1E22, 0xFFA3A9B1, 0xFF1A1C20, 0xFF1A1C20, 0xFF141518, 0xFF181A1D, 0xFF07080A, 0xFF9699A0));
    /* How far in front of the face the picture is drawn, in blocks, so it never fights the face for the same depth. */
    private static final float LIFT = 0.003F;
    private static final float FRONT = MonitorKind.FRONT;
    /* The picture is drawn this many texture pixels to each unit it is laid out in, more on a big screen. */
    private static final int SCALE = 2;
    private static final int MOST_SCALE = 4;

    public MonitorScreenRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(final MonitorBlockEntity monitor, final float partialTick, final PoseStack pose,
                       final MultiBufferSource buffers, final int light, final int overlay) {
        final BlockState state = monitor.getBlockState();
        if (!(state.getBlock() instanceof MonitorBlock block)) {
            return;
        }
        final BlockPos pos = monitor.getBlockPos();
        final Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        final double distance = eye.distanceToSqr(Vec3.atCenterOf(pos));
        if (!LiveRate.shows(distance)) {
            return;
        }
        final MonitorKind kind = block.kind();
        final MonitorPanel panel = monitor.panel();
        if (panel != null && !panel.origin().equals(pos)) {
            return;
        }
        final int wide = panel == null ? 1 : panel.width();
        final int tall = panel == null ? 1 : panel.height();
        pose.pushPose();
        faceFrame(pose, state.getValue(MonitorBlock.FACING), tall);
        if (panel != null) {
            final LiveScreen frame = LiveScreens.ask(new FrameKey(pos), (g, w, h, partial) -> frame(g, w, h, kind),
                    wide * (int) FRONT, tall * (int) FRONT, 1, Tube.COLOUR, distance);
            LiveScreens.draw(pose, buffers, frame, 0, 0, wide * FRONT, tall * FRONT, light);
            pose.translate(0.0F, 0.0F, -LIFT / 2);
        }
        if (state.getValue(MonitorBlock.LIT)) {
            // A lit screen shines by itself, whatever light falls on it.
            final BlockPos shownFor = monitor.screen().getBlockPos();
            final IMonitorPicture picture = MonitorPictureCache.of(shownFor);
            final float[] glass = glass(kind, wide, tall);
            final int scale = Math.min(MOST_SCALE, Math.max(SCALE, wide));
            final LiveScreen live = LiveScreens.ask(shownFor, (g, w, h, partial) -> MonitorPainter.paint(g, w, h,
                            picture, shownFor, partial), MonitorGlass.WIDTH, MonitorGlass.HEIGHT, scale, kind.tube(),
                    distance);
            LiveScreens.draw(pose, buffers, live, glass[0], glass[1], glass[2], glass[3], LightTexture.FULL_BRIGHT);
        }
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(final MonitorBlockEntity monitor) {
        // A big screen is drawn by one corner, and must be drawn while any of it is in view.
        return monitor.panel() != null;
    }

    @Override
    public AABB getRenderBoundingBox(final MonitorBlockEntity monitor) {
        final MonitorPanel panel = monitor.panel();
        if (panel == null || !panel.origin().equals(monitor.getBlockPos())) {
            return new AABB(monitor.getBlockPos());
        }
        return new AABB(Vec3.atLowerCornerOf(panel.origin()), Vec3.atLowerCornerOf(panel.at(panel.width() - 1,
                panel.height() - 1)).add(1.0, 1.0, 1.0));
    }

    /**
     * Turns the pose so units run across the front as it is seen, {@link MonitorKind#FRONT} to a block, from its top
     * left down and to the right, just in front of the face. The front looks against the block's facing; the model's
     * own front is its north face, turned as the block state turns it.
     */
    private static void faceFrame(final PoseStack pose, final Direction facing, final int tall) {
        pose.translate(0.5F, 0.0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        pose.translate(-0.5F, 0.0F, -0.5F);
        // The north face as seen: its left edge is the model's east side, its top the block's top.
        pose.translate(1.0F, tall, -LIFT);
        pose.scale(-1.0F / FRONT, -1.0F / FRONT, 1.0F);
    }

    /*
     * Where the picture goes, in units across the front: on a monitor alone its own glass; on a big screen the glass
     * inside one bezel and above one chin. The picture keeps its shape, centred where the glass is a shade off it.
     */
    private static float[] glass(final MonitorKind kind, final int wide, final int tall) {
        final float x0 = kind.glass(0);
        final float y0 = kind.glass(1);
        final float x1 = wide * FRONT - (FRONT - kind.glass(2));
        final float y1 = tall * FRONT - (FRONT - kind.glass(3));
        final float gw = x1 - x0;
        final float gh = y1 - y0;
        final float aspect = (float) MonitorGlass.WIDTH / MonitorGlass.HEIGHT;
        float w = gw;
        float h = gw / aspect;
        if (h > gh) {
            h = gh;
            w = gh * aspect;
        }
        final float left = x0 + (gw - w) / 2;
        final float top = y0 + (gh - h) / 2;
        return new float[] {left, top, left + w, top + h};
    }

    /* A big screen's front: its bezel round all of it, one chin with the maker's mark, and the dark glass. */
    private static void frame(final GuiGraphics g, final int width, final int height, final MonitorKind kind) {
        final Bezels bezels = BEZELS.get();
        final int bezel = switch (kind) {
            case TRANSITION -> bezels.transition();
            case COLOR -> bezels.color();
            default -> bezels.standard();
        };
        final int chin = switch (kind) {
            case TRANSITION -> bezels.transitionStrip();
            case COLOR -> bezels.colorChin();
            default -> bezels.standardChin();
        };
        final int glassBottom = height - ((int) FRONT - kind.glass(3));
        g.fill(0, 0, width, height, bezel);
        g.fill(0, glassBottom + 1, width, height, chin);
        g.fill(kind.glass(0), kind.glass(1), width - ((int) FRONT - kind.glass(2)), glassBottom, bezels.glass());
        final int mark = width / 2;
        g.fill(mark - 8, glassBottom + 6, mark + 8, glassBottom + 8, bezels.mark());
    }

    /** The key a big screen's front is kept under, apart from the picture of the monitor at the same place. */
    private record FrameKey(BlockPos origin) {
    }

    private record Bezels(int transition, int transitionStrip, int standard, int standardChin, int color,
                          int colorChin, int glass, int mark) {
    }
}
