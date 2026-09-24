/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio;

import com.mojang.blaze3d.audio.ListenerTransform;
import dev.jstech.core.JsCore;
import dev.jstech.core.audio.AlertSignBoard;
import dev.jstech.core.client.gui.component.Draw;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Alerts on screen, for a player who asked for them: when an alert plays, a sign comes up at the top of the screen
 * with what the alert is and an arrow to where it comes from, the way the game's subtitles point. It blinks three
 * times as it comes up and goes after four seconds.
 */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class AlertSigns {

    private static final AlertSignBoard BOARD = new AlertSignBoard();
    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "alert_signs");
    private static final String MARK = "!";
    private static final String LEFT = "<";
    private static final String RIGHT = ">";
    /** From the top of the screen to the first sign, and from one sign to the next. */
    private static final int TOP = 14;
    private static final int PITCH = 19;
    private static final int HEIGHT = 15;

    private AlertSigns() {
    }

    @SubscribeEvent
    public static void onRegisterLayers(final RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, LAYER, AlertSigns::render);
    }

    @SubscribeEvent
    public static void onLoggingOut(final ClientPlayerNetworkEvent.LoggingOut event) {
        BOARD.clear();
    }

    /** Puts up the sign for an alert that has just started, when the player asked for alerts on screen. */
    public static void raise(final SoundInstance alert) {
        if (!AudioPrefsStore.prefs().visualCues()) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final WeighedSoundEvents events = minecraft.getSoundManager().getSoundEvent(alert.getLocation());
        final Component subtitle = events == null ? null : events.getSubtitle();
        BOARD.raise(subtitle != null ? subtitle.getString() : alert.getLocation().toString(), alert.getX(),
                alert.getY(), alert.getZ(), !alert.isRelative(), minecraft.gui.getGuiTicks());
    }

    /** The signs up now, newest first, for a test to look at. */
    public static List<AlertSignBoard.Sign> showing() {
        return BOARD.showing(Minecraft.getInstance().gui.getGuiTicks());
    }

    private static void render(final GuiGraphics g, final DeltaTracker delta) {
        final Minecraft minecraft = Minecraft.getInstance();
        final long now = minecraft.gui.getGuiTicks();
        final List<AlertSignBoard.Sign> signs = BOARD.showing(now);
        if (signs.isEmpty()) {
            return;
        }
        final Font font = minecraft.font;
        final AlertSignPalette.Colours colours = AlertSignPalette.get();
        final ListenerTransform listener = minecraft.getSoundManager().getListenerTransform();
        for (int i = 0; i < signs.size(); i++) {
            final AlertSignBoard.Sign sign = signs.get(i);
            if (AlertSignBoard.lit(sign, now)) {
                draw(g, font, colours, sign, direction(listener, sign), TOP + i * PITCH);
            }
        }
    }

    /* The box, its bordered mark, what it says and the arrow, centred at the top. */
    private static void draw(final GuiGraphics g, final Font font, final AlertSignPalette.Colours colours,
                             final AlertSignBoard.Sign sign, final int direction, final int y) {
        final int arrow = font.width(RIGHT);
        final int width = 4 + 9 + 4 + font.width(sign.label()) + 6 + arrow + 3;
        final int x = g.guiWidth() / 2 - width / 2;
        g.fill(x - 1, y - 1, x + width + 1, y + HEIGHT + 1, colours.border());
        g.fill(x, y, x + width, y + HEIGHT, colours.ground());
        g.fill(x + 4, y + 3, x + 13, y + 12, colours.border());
        g.drawString(font, MARK, x + 8, y + 4, colours.mark(), false);
        Draw.text(g, font, sign.label(), x + 17, y + 4, colours.text(), colours.ground());
        if (direction > 0) {
            Draw.text(g, font, RIGHT, x + width - 3 - arrow, y + 4, colours.arrow(), colours.ground());
        } else if (direction < 0) {
            Draw.text(g, font, LEFT, x + width - 3 - arrow, y + 4, colours.arrow(), colours.ground());
        }
    }

    /* As the game's subtitles judge it: ahead points nowhere, otherwise to the side the sound is on. */
    private static int direction(final ListenerTransform listener, final AlertSignBoard.Sign sign) {
        if (!sign.placed()) {
            return 0;
        }
        final Vec3 towards = new Vec3(sign.x(), sign.y(), sign.z()).subtract(listener.position()).normalize();
        return AlertSignBoard.direction(listener.forward().dot(towards), listener.right().dot(towards));
    }
}
