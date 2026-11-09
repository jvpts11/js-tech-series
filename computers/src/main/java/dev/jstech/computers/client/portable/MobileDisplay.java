/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.computers.client.portable.MobileCanvas.Align;
import dev.jstech.computers.operation.payload.portable.PortableViewPayload;
import dev.jstech.computers.portable.MobileApp;
import dev.jstech.computers.portable.PortableBattery;
import dev.jstech.computers.portable.PortableBoot;
import dev.jstech.computers.portable.PortableClock;
import dev.jstech.computers.portable.PortableModel;
import dev.jstech.computers.portable.Portables;
import dev.jstech.computers.registry.ComputingComponents;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * What a portable's screen shows, wherever it is seen: held up, glanced at in the hand, standing in its dock. Off, it
 * is dark; switched on, it plays its start from the same moment everywhere; then it shows its system, at the page its
 * session is on when it is held up, at its lock when it is only glanced at.
 */
public final class MobileDisplay {

    /* The bolt drawn over a battery while it charges, mark by mark. */
    private static final String[] BOLT = {"...XX", "..XX.", ".XXXX", "..XX.", ".XX..", "XX..."};

    private MobileDisplay() {
    }

    /**
     * Paints what the device in {@code stack} shows on its screen, at the origin of {@code graphics}, and returns the
     * places that can be pressed. {@code session} is null for a device only glanced at; the pointer is in the screen's
     * own pixels, negative when it is off it.
     */
    public static List<MobileCanvas.Press> paint(final GuiGraphics graphics, final ItemStack stack,
                                                 final PortableModel model, @Nullable final MobileSession session,
                                                 final double mouseX, final double mouseY) {
        final Minecraft minecraft = Minecraft.getInstance();
        final MobileCanvas canvas = new MobileCanvas(graphics, minecraft.font, model.screenWidth(),
                model.screenHeight());
        final long now = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        final long onSince = Portables.onSince(stack);
        final PortableBoot.Stage stage = PortableBoot.stage(onSince, now);
        final IMobileLook look = IMobileLook.of(model.system());
        switch (stage) {
            case OFF -> canvas.rect(0, 0, canvas.width(), canvas.height(), MobileParts.colours().black());
            case MARK -> canvas.picture(MobileArt.start(model), model.screenWidth() * 2, model.screenHeight() * 2,
                    0, 0, canvas.width(), canvas.height());
            case SYSTEM -> look.boot(canvas, frame(stack, model, session, mouseX, mouseY, now), PortableBoot.progress(
                    onSince, now));
            case READY -> look.paint(canvas, frame(stack, model, session, mouseX, mouseY, now));
        }
        return canvas.presses();
    }

    /**
     * Paints a device standing in its dock: switched off on its charger, it shows its battery filling, the bolt over
     * it while it charges and the share under it.
     */
    public static void charging(final GuiGraphics graphics, final PortableModel model, final int percent,
                                final boolean filling) {
        final MobileCanvas c = new MobileCanvas(graphics, Minecraft.getInstance().font, model.screenWidth(),
                model.screenHeight());
        final MobileParts.Colours k = MobileParts.colours();
        final float w = c.width();
        final float h = c.height();
        c.rect(0, 0, w, h, k.black());
        final float bw = Math.min(w, h * 0.66F) * 0.5F;
        final float bh = bw * 0.5F;
        final float x0 = (w - bw) / 2;
        final float y0 = h * 0.42F - bh / 2;
        final float t = Math.max(1.0F, bw / 28);
        c.rect(x0, y0, bw, t, k.batteryLine());
        c.rect(x0, y0 + bh - t, bw, t, k.batteryLine());
        c.rect(x0, y0, t, bh, k.batteryLine());
        c.rect(x0 + bw - t, y0, t, bh, k.batteryLine());
        c.rect(x0 + bw, y0 + bh / 3, t * 2, bh / 3, k.batteryLine());
        c.rect(x0 + 2 * t, y0 + 2 * t, (bw - 4 * t) * percent / 100.0F, bh - 4 * t, k.batteryGreen());
        if (filling) {
            final float mark = Math.max(1.0F, bh / 10);
            c.glyph(BOLT, x0 + bw / 2 - 2.5F * mark, y0 + bh / 2 - 3 * mark, k.white(), mark);
        }
        c.text(MobileParts.say(MobileTexts.PERCENT.with(percent)), w / 2, y0 + bh + 2 * t + 4, k.batteryLine(),
                MobileCanvas.BIG, Align.CENTRE, false);
    }

    /* Everything the screen of the device in {@code stack} shows this frame. */
    private static MobileFrame frame(final ItemStack stack, final PortableModel model,
                                     @Nullable final MobileSession session, final double mouseX,
                                     final double mouseY, final long now) {
        final Minecraft minecraft = Minecraft.getInstance();
        final UUID id = Portables.id(stack);
        final PortableViewPayload view = id == null ? null : PortableViews.of(id);
        final long dayTime = minecraft.level == null ? 0L : minecraft.level.getDayTime();
        final int percent = PortableBattery.percent(Portables.battery(stack, model), PortableBattery.capacity(model));
        final String owner = minecraft.player == null ? "" : minecraft.player.getName().getString();
        return new MobileFrame(model, session == null ? MobilePage.LOCK : session.page(),
                session == null ? null : session.app(),
                view == null ? MobileApp.factory(model) : view.installed(),
                view == null ? List.of() : view.alerts(), view != null && view.mirror(), percent,
                stack.get(ComputingComponents.PORTABLE_LINK.get()), PortableClock.time(dayTime),
                PortableClock.day(dayTime), owner, mouseX, mouseY, session == null ? null : session.balloon(),
                session == null ? 0.0F : session.slide(), session == null ? MobileSession.ALL_TAB : session.tab(),
                session == null ? 0 : session.scroll(), now, session);
    }
}
