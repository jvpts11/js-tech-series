/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import com.mojang.math.Axis;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.guide.GuideLayout;
import dev.jstech.core.guide.GuideManual;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.guide.IGuideText;
import dev.jstech.core.guide.TextSize;
import dev.jstech.core.gui.ManualButtonLook;
import dev.jstech.core.gui.layout.ManualScreenLayout;
import java.util.List;
import java.util.Random;
import java.util.function.ToIntFunction;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * A bound book's cover, drawn from the parts its style lists over the closed book: its board in the cover's colour,
 * then each part in turn, so a book is dressed as the manuals of its time were with nothing but data. A part's words
 * are the manual's cover lines, in the reader's language.
 */
final class ManualCovers {

    /** A cloth's weave: how far apart its darker rows are, and how much darker. */
    private static final int WEAVE = 2;
    private static final double WEAVE_SHADE = 0.1;
    /** Kraft paper's flecks: how many to a square of a hundred pixels, how dark, and what they are scattered by. */
    private static final int FLECKS_PER_HUNDRED = 3;
    private static final double[] FLECK_SHADES = {0.1, 0.18, 0.25};
    private static final long FLECK_SEED = 3L;
    /** A wire coil down the spine: how far apart its loops are, from the spine's ends, and how long each is. */
    private static final int COIL_PITCH = 6;
    private static final int COIL_FROM = 6;

    private ManualCovers() {
    }

    /** The closed book: its board, then the parts its style's cover lists, each over the ones before it. */
    static void draw(final GuiGraphics g, final Font font, final GuideStyle style, final GuideManual manual,
                     final ManualScreenLayout.Rect cover, final ToIntFunction<String> colour, final IGuideText text) {
        final int board = colour.applyAsInt(GuideStyle.COVER);
        ManualControls.bevel(g, cover.x(), 0, cover.width(), cover.height(), board,
                colour.applyAsInt(GuideStyle.COVER_EDGE), ManualButtonLook.shaded(board));
        for (final GuideStyle.CoverPart part : style.cover().parts()) {
            part(g, font, style, manual, cover, part, colour, text);
        }
    }

    private static void part(final GuiGraphics g, final Font font, final GuideStyle style, final GuideManual manual,
                             final ManualScreenLayout.Rect cover, final GuideStyle.CoverPart part,
                             final ToIntFunction<String> colour, final IGuideText text) {
        final int x = cover.x() + part.x();
        final int y = part.y();
        final int tint = colourOf(part.colour(), colour);
        switch (part.kind()) {
            case FILL -> g.fill(x, y, x + part.width(), y + part.height(), tint);
            case FRAME -> Draw.outline(g, x, y, part.width(), part.height(), tint);
            case PLATE -> ManualControls.bevel(g, x, y, part.width(), part.height(), tint,
                    ManualButtonLook.lit(tint), ManualButtonLook.shaded(tint));
            case IMAGE -> {
                final ResourceLocation image = ResourceLocation.tryParse(part.image());
                if (image != null) {
                    g.blit(image.withPrefix("textures/").withSuffix(".png"), x, y, part.width(), part.height(), 0, 0,
                            1, 1, 1, 1);
                }
            }
            case SMALL, CENTRED_SMALL -> words(g, font, style, x, y, part, TextSize.SMALL, tint, line(manual, part,
                    text), text);
            case WORDS -> words(g, font, style, x, y, part, TextSize.BODY, tint, line(manual, part, text), text);
            case BOLD, CENTRED -> words(g, font, style, x, y, part, TextSize.BOLD, tint, line(manual, part, text),
                    text);
            case CODE -> scaled(g, font, style, line(manual, part, text), TextSize.CODE, x, y, part.scale(), tint);
            case STAMP -> stamp(g, font, style, x, y, part, tint, line(manual, part, text));
            case CLOTH -> {
                final int weave = ManualButtonLook.darken(tint, WEAVE_SHADE);
                for (int row = y + 1; row < y + part.height() - 1; row += WEAVE) {
                    g.fill(x, row, x + part.width(), row + 1, weave);
                }
            }
            case KRAFT -> {
                // Scattered the same way every time the book is shown, so the cover does not shimmer.
                final Random flecks = new Random(FLECK_SEED);
                final int count = part.width() * part.height() * FLECKS_PER_HUNDRED / 100;
                for (int i = 0; i < count; i++) {
                    final int fx = x + flecks.nextInt(Math.max(1, part.width()));
                    final int fy = y + flecks.nextInt(Math.max(1, part.height()));
                    g.fill(fx, fy, fx + 1, fy + 1, ManualButtonLook.darken(tint,
                            FLECK_SHADES[flecks.nextInt(FLECK_SHADES.length)]));
                }
            }
            case WIRE -> {
                for (int loop = y + COIL_FROM; loop < y + part.height() - COIL_FROM; loop += COIL_PITCH) {
                    g.fill(x, loop, x + part.width(), loop + 2, tint);
                    g.fill(x, loop, x + part.width(), loop + 1, ManualButtonLook.lit(tint));
                    g.fill(x, loop + 2, x + part.width(), loop + 3, ManualButtonLook.shaded(tint));
                }
            }
        }
    }

    /** One of the manual's cover lines a part names, from 1, in the reader's language; empty when there is none. */
    private static String line(final GuideManual manual, final GuideStyle.CoverPart part, final IGuideText text) {
        final List<String> lines = manual.coverKeys();
        return part.words() < 1 || part.words() > lines.size() ? "" : text.text(lines.get(part.words() - 1));
    }

    /** Words wrapped in the part's width at its scale, left from its edge or centred on its middle. */
    private static void words(final GuiGraphics g, final Font font, final GuideStyle style, final int x, final int y,
                              final GuideStyle.CoverPart part, final TextSize size, final int tint, final String words,
                              final IGuideText text) {
        final boolean centred = part.kind() == GuideStyle.PartKind.CENTRED
                || part.kind() == GuideStyle.PartKind.CENTRED_SMALL;
        int lineY = y;
        for (final String line : GuideLayout.wrap(words, (int) (part.width() / part.scale()), size, text)) {
            final int width = (int) (GuideFonts.width(font, line, size, style) * part.scale());
            scaled(g, font, style, line, size, centred ? x + (part.width() - width) / 2 : x, lineY, part.scale(),
                    tint);
            lineY += Math.round(size.lineHeight() * part.scale());
        }
    }

    private static void scaled(final GuiGraphics g, final Font font, final GuideStyle style, final String words,
                               final TextSize size, final int x, final int y, final float scale, final int tint) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        GuideFonts.draw(g, font, words, size, style, 0, 0, tint);
        g.pose().popPose();
    }

    /** A rubber stamp: its frame turned a little, its words in bold inside it, as a stamp lands on a cover. */
    private static void stamp(final GuiGraphics g, final Font font, final GuideStyle style, final int x, final int y,
                              final GuideStyle.CoverPart part, final int tint, final String words) {
        g.pose().pushPose();
        g.pose().translate(x + part.width() / 2.0F, y + part.height() / 2.0F, 0.0F);
        g.pose().mulPose(Axis.ZP.rotationDegrees(part.turn()));
        Draw.outline(g, -part.width() / 2, -part.height() / 2, part.width(), part.height(), tint);
        Draw.outline(g, -part.width() / 2 + 1, -part.height() / 2 + 1, part.width() - 2, part.height() - 2, tint);
        g.pose().popPose();
        final int width = GuideFonts.width(font, words, TextSize.BOLD, style);
        GuideFonts.draw(g, font, words, TextSize.BOLD, style, x + (part.width() - width) / 2,
                y + (part.height() - TextSize.BOLD.lineHeight()) / 2 + 1, tint);
    }

    /** A part's colour: written {@code #AARRGGBB}, or a role of the style's palette; ink when it names neither. */
    private static int colourOf(final String written, final ToIntFunction<String> colour) {
        final Integer parsed = PaletteLookup.parse(written);
        if (parsed != null) {
            return parsed;
        }
        return colour.applyAsInt(written.isEmpty() ? GuideStyle.INK : written);
    }
}
