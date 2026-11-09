/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.monitor;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.computers.block.MonitorKind;
import dev.jstech.computers.gui.MonitorGlass;
import dev.jstech.computers.hardware.CaseGlass;
import dev.jstech.computers.hardware.FrontArea;
import dev.jstech.computers.monitor.IMonitorPicture;
import dev.jstech.core.client.live.LiveScreen;
import dev.jstech.core.client.live.LiveScreens;
import dev.jstech.core.live.LiveRate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Shows the picture on the glass of a computer that is its own screen, as an all-in-one is
 * (computers/docs/design/quince.md): what the machine shows, live, as a monitor shows it on its own glass
 * ({@link MonitorScreenRenderer}), as close as {@link LiveRate} lets a viewer read it.
 */
public final class GlassFaces {

    /* How far in front of the glass the picture is drawn, in blocks, so it never fights the glass for its depth. */
    private static final float LIFT = 0.003F;
    private static final float FRONT = MonitorKind.FRONT;
    /* The picture is drawn this many texture pixels to each unit it is laid out in, as on a monitor alone. */
    private static final int SCALE = 2;

    private GlassFaces() {
    }

    /**
     * Draws what the machine at {@code pos} shows on {@code glass}, in a pose standing on the middle of the machine's
     * floor and turned as it faces, its front the model's north face.
     */
    public static void draw(final BlockPos pos, final CaseGlass glass, final PoseStack pose,
                            final MultiBufferSource buffers) {
        final Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        final double distance = eye.distanceToSqr(Vec3.atCenterOf(pos));
        if (!LiveRate.shows(distance)) {
            return;
        }
        // Dark glass when the machine shows nothing: switched off, or not yet described.
        final IMonitorPicture picture = MonitorPictureCache.of(pos);
        if (picture instanceof IMonitorPicture.Dark) {
            return;
        }
        pose.pushPose();
        // Units run across the front as it is seen, from its top left (the model's east side) down and to the right.
        pose.translate(0.5F, 1.0F, -0.5F + glass.depth() / FRONT - LIFT);
        pose.scale(-1.0F / FRONT, -1.0F / FRONT, 1.0F);
        final FrontArea area = glass.area();
        final float[] at = MonitorScreenRenderer.fitted(area.left(), area.top(), area.right(), area.bottom());
        // A lit screen shines by itself, whatever light falls on it.
        final LiveScreen live = LiveScreens.ask(pos, (g, w, h, partial) -> MonitorPainter.paint(g, w, h, picture,
                pos, partial), MonitorGlass.WIDTH, MonitorGlass.HEIGHT, SCALE, glass.tube(), distance);
        LiveScreens.draw(pose, buffers, live, at[0], at[1], at[2], at[3], LightTexture.FULL_BRIGHT);
        pose.popPose();
    }
}
