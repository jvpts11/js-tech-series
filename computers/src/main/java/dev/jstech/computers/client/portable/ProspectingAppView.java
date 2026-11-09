/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.client.portable.IAppKit.Tone;
import dev.jstech.computers.client.portable.MobileCanvas.Align;
import dev.jstech.computers.operation.payload.portable.PortableActionPayload;
import dev.jstech.computers.operation.payload.portable.PortableActionPayload.Action;
import dev.jstech.computers.operation.payload.portable.PortableProspectPayload;
import dev.jstech.computers.portable.MobileApp;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.TextKey;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Prospecting (computers/docs/design/wireless.md), as the K0.11 apps page drew it on the Nebula S10: a compass, north
 * up, with a needle for each vein near, as long as it shows strong and in the colour of what it is made of; and under
 * it a card for each, its strength in words and bars and the way it lies. J's Core's world kit tells which way and how
 * strongly, never where.
 */
@PaletteHolder
final class ProspectingAppView implements IMobileAppView {

    private static final int ASK_EVERY = 60;
    private static final float RADIUS = 40.0F;
    private static final int BARS = 5;
    private static final float STRONG = 0.7F;
    private static final float FAIR = 0.4F;
    private static final TextKey[] POINTS = {AppTexts.NORTH, AppTexts.NORTH_EAST, AppTexts.EAST, AppTexts.SOUTH_EAST,
        AppTexts.SOUTH, AppTexts.SOUTH_WEST, AppTexts.WEST, AppTexts.NORTH_WEST};
    /** Its colours, as the K0.11 apps page drew them: the dial, its rings and letters, the bars. */
    private static final Palette<Colours> COLOURS = Palettes.declare(JsComputers.MODID, "mobile/prospecting",
            new Colours(0xFFFFFFFF, 0xFFD8DCE6, 0xFFECEEF4, 0xFF9AA0AA, 0xFF20242A, 0xFF1A73E8, 0xFFDFE3EA));

    @Override
    public int askEvery() {
        return ASK_EVERY;
    }

    @Override
    @Nullable
    public PortableActionPayload ask(final UUID device, final MobileAppState state) {
        return PortableActionPayload.of(device, Action.PROSPECT);
    }

    @Override
    public void paint(final AppPage page) {
        final Colours k = COLOURS.get();
        final MobileCanvas c = page.c();
        final IAppKit kit = page.kit();
        final float top = kit.bar(c, MobileApp.PROSPECTING, page.top(), MobileParts.name(MobileApp.PROSPECTING),
                null, false);
        final PortableProspectPayload view = PortableApps.prospect(page.device());
        final float cx = c.width() / 2.0F;
        final float cy = top + RADIUS + 4;
        c.disc(cx, cy, RADIUS, k.dial());
        c.ring(cx, cy, RADIUS, 1, k.ring());
        c.ring(cx, cy, RADIUS * 0.62F, 1, k.innerRing());
        final TextKey[] cardinal = {AppTexts.NORTH, AppTexts.EAST, AppTexts.SOUTH, AppTexts.WEST};
        for (int n = 0; n < cardinal.length; n++) {
            final double angle = Math.toRadians(n * 90.0);
            c.text(MobileTexts.say(cardinal[n]), cx + (float) Math.sin(angle) * (RADIUS - 6),
                    cy - (float) Math.cos(angle) * (RADIUS - 6) - 3, k.letters(), MobileCanvas.SMALL, Align.CENTRE,
                    false);
        }
        float y = cy + RADIUS + 6;
        if (view != null) {
            for (final PortableProspectPayload.Vein vein : view.veins()) {
                final double angle = Math.toRadians(vein.bearing());
                final float length = RADIUS * (0.35F + 0.6F * vein.strength());
                final float x1 = cx + (float) Math.sin(angle) * length;
                final float y1 = cy - (float) Math.cos(angle) * length;
                c.line(cx, cy, x1, y1, 1.5F, 0xFF << 24 | vein.colour());
                c.disc(x1, y1, 3, 0xFF << 24 | vein.colour());
            }
            for (final PortableProspectPayload.Vein vein : view.veins()) {
                if (y + 28 > page.bottom() - 10) {
                    break;
                }
                y = card(page, k, vein, y);
            }
            if (view.veins().isEmpty()) {
                kit.note(c, y + 4, MobileTexts.say(AppTexts.NO_VEINS), Tone.DIM);
                y += 14;
            }
        }
        c.disc(cx, cy, 3, k.centre());
        kit.note(c, y + 4, MobileTexts.say(AppTexts.NEVER_PLACES), Tone.DIM);
    }

    /** The point of the compass {@code bearing} is nearest, in degrees clockwise from north. */
    static TextKey point(final float bearing) {
        final int eighth = Math.round(((bearing % 360 + 360) % 360) / 45.0F) % POINTS.length;
        return POINTS[eighth];
    }

    /* A vein's card: its colour, what it is, how strongly it shows and which way, and its strength in bars. */
    private static float card(final AppPage page, final Colours k, final PortableProspectPayload.Vein vein,
                              final float y) {
        final MobileCanvas c = page.c();
        final IAppKit kit = page.kit();
        final float w = c.width();
        kit.card(c, 5, y, w - 10, 24);
        c.disc(14, y + 12, 4, 0xFF << 24 | vein.colour());
        c.text(c.fit(MobileTexts.say(vein.name()), w - 70, MobileCanvas.SMALL), 22, y + 4, kit.ink(Tone.TEXT),
                MobileCanvas.SMALL, Align.LEFT, true);
        final TextKey word = vein.strength() > STRONG ? AppTexts.STRONG : vein.strength() > FAIR ? AppTexts.FAIR
                : AppTexts.FAINT;
        c.text(MobileTexts.say(AppTexts.HOW_AND_WHERE.with(word.text(), point(vein.bearing()).text())), 22, y + 13,
                kit.ink(Tone.DIM), MobileCanvas.SMALL);
        final int lit = Math.round(vein.strength() * BARS);
        for (int n = 0; n < BARS; n++) {
            c.rect(w - 34 + n * 5, y + 15 - n * 2, 3, 3 + n * 2, n < lit ? k.barOn() : k.barOff());
        }
        return y + 28;
    }

    /** The prospecting's colours, as the palette above names them. */
    private record Colours(int dial, int ring, int innerRing, int letters, int centre, int barOn, int barOff) {
    }
}
