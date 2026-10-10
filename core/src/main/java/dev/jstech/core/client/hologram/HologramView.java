/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.hologram;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.core.JsCore;
import dev.jstech.core.hologram.HologramPayload;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * The holograms this player's game was told of, drawn in the world each frame: every line turned to face the camera,
 * on a dim ground, the way a name over a player's head is drawn. A hologram goes when the server takes it down, when
 * its time runs out, when the player goes further than {@link #RANGE}, or when the player leaves the world.
 */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class HologramView {

    /** Further than this, in blocks, a hologram is not drawn: past it the words are too small to read. */
    public static final double RANGE = 48.0;
    /* A block holds forty of the font's pixels, as a name over a head is drawn. */
    private static final float SCALE = 0.025F;
    private static final int LINE_HEIGHT = 10;

    private static final Map<ResourceLocation, Shown> SHOWN = new LinkedHashMap<>();

    private HologramView() {
    }

    /** Takes what the server said: a hologram up or rewritten, or, with no lines, taken down. */
    public static void accept(final HologramPayload payload) {
        if (payload.lines().isEmpty()) {
            SHOWN.remove(payload.id());
            return;
        }
        final ClientLevel level = Minecraft.getInstance().level;
        final long until = payload.ticks() <= 0 || level == null ? Long.MAX_VALUE
                : level.getGameTime() + payload.ticks();
        SHOWN.put(payload.id(), new Shown(new Vec3(payload.x(), payload.y(), payload.z()), payload.lines(), until));
    }

    /** The lines of the hologram {@code id} as this game holds them, or none; for a test to look at. */
    public static List<Component> linesOf(final ResourceLocation id) {
        final Shown shown = SHOWN.get(id);
        return shown == null ? List.of() : shown.lines();
    }

    @SubscribeEvent
    public static void onRender(final RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || SHOWN.isEmpty()) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        final long now = minecraft.level.getGameTime();
        SHOWN.values().removeIf(shown -> shown.until() < now);
        final Camera camera = event.getCamera();
        final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        final HologramPalette.Colours colours = HologramPalette.get();
        for (final Shown shown : SHOWN.values()) {
            if (shown.at().distanceToSqr(camera.getPosition()) <= RANGE * RANGE) {
                draw(event.getPoseStack(), camera, minecraft.font, buffers, colours, shown);
            }
        }
        buffers.endBatch();
    }

    @SubscribeEvent
    public static void onLoggingOut(final ClientPlayerNetworkEvent.LoggingOut event) {
        SHOWN.clear();
    }

    /* Fires on respawn and on a change of dimension, where the game does not log out: the words of the old level
       must not be drawn at their old coordinates in the new one. */
    @SubscribeEvent
    public static void onPlayerReplaced(final ClientPlayerNetworkEvent.Clone event) {
        SHOWN.clear();
    }

    /* Each line centred on the hologram's middle, the last one at its foot, all of them facing the camera. */
    private static void draw(final PoseStack pose, final Camera camera, final Font font,
                             final MultiBufferSource buffers, final HologramPalette.Colours colours,
                             final Shown shown) {
        final Vec3 from = shown.at().subtract(camera.getPosition());
        pose.pushPose();
        pose.translate(from.x, from.y, from.z);
        pose.mulPose(camera.rotation());
        pose.scale(SCALE, -SCALE, SCALE);
        final List<Component> lines = shown.lines();
        for (int i = 0; i < lines.size(); i++) {
            final Component line = lines.get(i);
            final float x = -font.width(line) / 2.0F;
            final float y = (i - lines.size()) * LINE_HEIGHT;
            font.drawInBatch(line, x, y, colours.text(), false, pose.last().pose(), buffers,
                    Font.DisplayMode.NORMAL, colours.ground(), LightTexture.FULL_BRIGHT);
        }
        pose.popPose();
    }

    /** One hologram as this game holds it. */
    private record Shown(Vec3 at, List<Component> lines, long until) {
    }
}
