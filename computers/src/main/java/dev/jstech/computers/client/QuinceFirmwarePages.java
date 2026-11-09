/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.layout.QuinceStartLayout;
import dev.jstech.computers.os.QuinceStart;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * What the firmware of Quince's machines puts on the glass while they start (computers/docs/design/quince.md), as the
 * approved boot page drew it: the Russet 128K's Happy face on its grey and Open Firmware's, the fruit on the EFI's grey
 * with its spinner or on the black with its bar; the Sad Quince when the parts make no computer; the start-up choosers
 * a sneaking power-on opens, and Open Firmware's page of text. The pictures are the page's own pixels, drawn once by
 * its generator, and the words are the game's font, at the glass's size.
 */
@PaletteHolder
public final class QuinceFirmwarePages {

    private static final int W = QuinceStartLayout.WIDTH;
    private static final int H = QuinceStartLayout.HEIGHT;

    /** Every colour the pages are drawn in, the page's own ({@code jsc:firmware/quince}). */
    private static final Palette<Colours> COLOURS = Palettes.declare(JsComputers.MODID, "firmware/quince",
            new Colours(0xFF9C9CA4, 0xFFBFBFBF, 0xFF303030, 0xFF000000, 0xFF3A3A3C, 0xFFF2F2F2,
                    0xFF000000, 0xFFFFFFFF, 0xFFBFBFBF, 0xFF3A3A3C, 0xFF1E1E20, 0xFFF2F2F2, 0xFF000000, 0xFFF2F2F2,
                    0xFF3E4A5C, 0xFF1C222C, 0xFF52565E, 0xFF2A2C30, 0xFF000000,
                    0xFFF2F2F2, 0x2EFFFFFF, 0xFFE8E8EC, 0xFFE8E8EC, 0xFF2A2A2E, 0xFF8A8A8F,
                    0xFFFFFFFF, 0xFF000000, 0xFFFFFFFF, 0xFF000000, 0xFF1E1E20, 0xFFF2F2F2));
    private static final ResourceLocation HAPPY = picture("happy");
    private static final ResourceLocation SAD = picture("sad");
    private static final ResourceLocation FRUIT_AQUA = picture("fruit_aqua");
    private static final ResourceLocation FRUIT_WHITE = picture("fruit_white");
    private static final ResourceLocation SAD_FRUIT = picture("sad_fruit");
    private static final ResourceLocation DITHER = picture("dither");
    private static final ResourceLocation DISK = picture("disk");
    private static final ResourceLocation DISK_FLAT = picture("disk_flat");
    private static final ResourceLocation GEAR = picture("gear");
    /* The Transition's spinner: twelve spokes, the lit one going round once a second. */
    private static final int SPOKES = 12;
    private static final int TICKS_A_TURN = 20;

    private QuinceFirmwarePages() {
    }

    /**
     * What a machine starting shows: the Happy face on the Russet 128K's dither or on Open Firmware's grey, the fruit
     * with the spinner on the Transition's grey, or the fruit with the bar on the black of the later ones.
     */
    public static void start(final GuiGraphics g, final int x, final int y, final QuinceStart start, final Era era,
                             final int ticks, final int postTicks) {
        final Colours c = COLOURS.get();
        final int cx = x + W / 2;
        final int cy = y + H / 2;
        switch (start) {
            case ROM, OPEN_FIRMWARE -> {
                if (start == QuinceStart.ROM) {
                    Draw.blended(() -> g.blit(DITHER, x, y, 0.0F, 0.0F, W, H, W, H));
                } else {
                    g.fill(x, y, x + W, y + H, c.legacyGround());
                }
                face(g, HAPPY, cx, cy + QuinceStartLayout.FACE_DY);
            }
            case EFI, CHIP, NONE -> {
                if (start == QuinceStart.EFI && HardwareEra.partsIndex(era) <= HardwareEra.partsIndex(
                        HardwareEra.TRANSITION)) {
                    g.fill(x, y, x + W, y + H, c.efiGrey());
                    fruit(g, FRUIT_AQUA, cx, cy + QuinceStartLayout.FRUIT_DY);
                    spinner(g, cx, cy + QuinceStartLayout.SPINNER_DY, c.efiGrey(), c.spinnerInk(), ticks);
                } else {
                    g.fill(x, y, x + W, y + H, c.black());
                    fruit(g, FRUIT_WHITE, cx, cy + QuinceStartLayout.FRUIT_DY);
                    final int bx = cx - QuinceStartLayout.BAR_W / 2;
                    final int by = cy + QuinceStartLayout.BAR_DY;
                    g.fill(bx, by, bx + QuinceStartLayout.BAR_W, by + QuinceStartLayout.BAR_H, c.barTrack());
                    final int done = postTicks <= 0 ? 0 : Math.min(QuinceStartLayout.BAR_W,
                            QuinceStartLayout.BAR_W * ticks / postTicks);
                    g.fill(bx, by, bx + done, by + QuinceStartLayout.BAR_H, c.barFill());
                }
            }
        }
    }

    /**
     * A machine that started and found nothing to boot: its start picture, still, and under it what the firmware says,
     * in a box of the picture's own colours.
     */
    public static void noBoot(final GuiGraphics g, final Font font, final int x, final int y, final QuinceStart start,
                              final Era era, final List<String> lines) {
        start(g, x, y, start, era, 0, 1);
        final Colours c = COLOURS.get();
        final boolean dark = start == QuinceStart.CHIP || start == QuinceStart.EFI
                && HardwareEra.partsIndex(era) > HardwareEra.partsIndex(HardwareEra.TRANSITION);
        final int paper = dark ? c.noBootPaperDark() : c.noBootPaperLight();
        final int ink = dark ? c.noBootInkDark() : c.noBootInkLight();
        final int bx = x + (W - QuinceStartLayout.NO_BOOT_W) / 2;
        final int by = y + QuinceStartLayout.noBootTop();
        g.fill(bx, by, bx + QuinceStartLayout.NO_BOOT_W, by + QuinceStartLayout.noBootH(), paper);
        frame(g, bx, by, bx + QuinceStartLayout.NO_BOOT_W, by + QuinceStartLayout.noBootH(), ink);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            TextWall.centered(g, font, TextWall.clip(font, lines.get(i), QuinceStartLayout.NO_BOOT_W - 8), x + W / 2,
                    by + QuinceStartLayout.NO_BOOT_PAD + i * QuinceStartLayout.PAGE_ROW, ink);
        }
    }

    /**
     * The Sad Quince: the Russet 128K's face turned sad, white on black, with the fault under it (Open Firmware's
     * word along the foot); or the later firmwares' fruit with its crossed eyes, their words and the fault.
     */
    public static void sad(final GuiGraphics g, final Font font, final int x, final int y, final QuinceStart start,
                           final Era era, final String fault) {
        final Colours c = COLOURS.get();
        final int cx = x + W / 2;
        final int cy = y + H / 2;
        if (start == QuinceStart.ROM || start == QuinceStart.OPEN_FIRMWARE) {
            g.fill(x, y, x + W, y + H, c.sadOldGround());
            face(g, SAD, cx, cy + QuinceStartLayout.SAD_FACE_DY);
            TextWall.centered(g, font, TextWall.clip(font, fault, W - 16), cx, cy + QuinceStartLayout.SAD_FAULT_DY,
                    c.sadOldInk());
            if (start == QuinceStart.OPEN_FIRMWARE) {
                TextWall.centered(g, font, QuinceFirmwareTexts.of(QuinceFirmwareTexts.OF_FAULT), cx,
                        y + H - QuinceStartLayout.SAD_FOOT_UP, c.sadOldInk());
            }
            return;
        }
        final boolean grey = start == QuinceStart.EFI
                && HardwareEra.partsIndex(era) <= HardwareEra.partsIndex(HardwareEra.TRANSITION);
        final int ground = grey ? c.sadGreyGround() : start == QuinceStart.EFI ? c.sadDarkGround() : c.sadChipGround();
        final int ink = grey ? c.sadGreyInk() : start == QuinceStart.EFI ? c.sadDarkInk() : c.sadChipInk();
        g.fill(x, y, x + W, y + H, ground);
        tinted(g, SAD_FRUIT, cx, cy + QuinceStartLayout.SAD_FRUIT_DY, ink);
        TextWall.centered(g, font, QuinceFirmwareTexts.of(start == QuinceStart.CHIP ? QuinceFirmwareTexts.SAD_CHIP
                : QuinceFirmwareTexts.SAD_EFI), cx, cy + QuinceStartLayout.SAD_WORDS_DY, ink);
        TextWall.centered(g, font, TextWall.clip(font, fault, W - 16), cx, cy + QuinceStartLayout.SAD_CODE_DY, ink);
    }

    /**
     * A start-up chooser: the Startup Manager of EFI on the Transition's blue slate or the Standard's grey, or the
     * Quince chip's Startup Options on black, with its gear last; the volumes in a row, the one picked lit, and under
     * it its button.
     *
     * @param names   the volumes' names, as many as the row holds
     * @param options whether the last of them is the Startup Options' gear rather than a volume
     */
    public static void chooser(final GuiGraphics g, final Font font, final int x, final int y,
                               final QuinceStart start, final Era era, final List<String> names,
                               final boolean options, final int picked) {
        final Colours c = COLOURS.get();
        final boolean chip = start == QuinceStart.CHIP;
        final boolean early = !chip && HardwareEra.partsIndex(era) <= HardwareEra.partsIndex(HardwareEra.TRANSITION);
        if (chip) {
            g.fill(x, y, x + W, y + H, c.chooserChip());
            TextWall.centered(g, font, QuinceFirmwareTexts.of(QuinceFirmwareTexts.LOADING_OPTIONS), x + W / 2,
                    y + QuinceStartLayout.OPTIONS_LINE_Y, c.optionsLine());
        } else {
            g.fillGradient(x, y, x + W, y + H, early ? c.chooserEarlyTop() : c.chooserLaterTop(),
                    early ? c.chooserEarlyBottom() : c.chooserLaterBottom());
        }
        final int count = Math.min(names.size(), QuinceStartLayout.MOST_VOLUMES);
        for (int i = 0; i < count; i++) {
            final int cx = x + QuinceStartLayout.volumeCenterX(i, count);
            final int top = y + QuinceStartLayout.VOLUME_TOP;
            if (i == picked) {
                final int left = cx - QuinceStartLayout.VOLUME_W / 2;
                g.fill(left, top, left + QuinceStartLayout.VOLUME_W, top + QuinceStartLayout.VOLUME_H, c.pickFill());
                frame(g, left, top, left + QuinceStartLayout.VOLUME_W, top + QuinceStartLayout.VOLUME_H,
                        c.pickFrame());
            }
            final ResourceLocation icon = options && i == count - 1 ? GEAR : early ? DISK : DISK_FLAT;
            final int half = QuinceStartLayout.ICON / 2;
            Draw.blended(() -> g.blit(icon, cx - half, y + QuinceStartLayout.ICON_CENTER_Y - half, 0.0F, 0.0F,
                    QuinceStartLayout.ICON, QuinceStartLayout.ICON, QuinceStartLayout.ICON, QuinceStartLayout.ICON));
            TextWall.centered(g, font, TextWall.clip(font, names.get(i), QuinceStartLayout.VOLUME_PITCH - 6), cx,
                    y + QuinceStartLayout.NAME_Y, c.chooserText());
        }
        if (count == 0) {
            return;
        }
        final int cx = x + QuinceStartLayout.volumeCenterX(Math.min(picked, count - 1), count);
        final int by = y + QuinceStartLayout.BUTTON_TOP;
        final String word = chip ? QuinceFirmwareTexts.of(QuinceFirmwareTexts.CONTINUE) : ">";
        final int bw = chip ? TextWall.width(font, word) + QuinceStartLayout.BUTTON_PAD : QuinceStartLayout.ARROW_W;
        g.fill(cx - bw / 2, by, cx + bw / 2, by + QuinceStartLayout.BUTTON_H, c.buttonFill());
        TextWall.centered(g, font, word, cx, by + 3, c.buttonText());
    }

    /** Open Firmware's page: black text on white, the oldest lines scrolling off the top, the prompt at the foot. */
    public static void openFirmware(final GuiGraphics g, final Font font, final int x, final int y,
                                    final List<String> lines) {
        final Colours c = COLOURS.get();
        g.fill(x, y, x + W, y + H, c.ofGround());
        final int first = Math.max(0, lines.size() - QuinceStartLayout.PAGE_LINES);
        for (int i = first; i < lines.size(); i++) {
            TextWall.draw(g, font, TextWall.clip(font, lines.get(i), W - 2 * QuinceStartLayout.PAGE_X),
                    x + QuinceStartLayout.PAGE_X, y + QuinceStartLayout.PAGE_TOP + (i - first)
                            * QuinceStartLayout.PAGE_ROW, c.ofInk());
        }
    }

    private static ResourceLocation picture(final String name) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "textures/gui/quince/" + name + ".png");
    }

    /* The Russet 128K's face, twice its size, centred on (cx, cy). */
    private static void face(final GuiGraphics g, final ResourceLocation face, final int cx, final int cy) {
        final int size = QuinceStartLayout.FACE * QuinceStartLayout.FACE_SCALE;
        Draw.blended(() -> g.blit(face, cx - size / 2, cy - size / 2, size, size, 0.0F, 0.0F,
                QuinceStartLayout.FACE, QuinceStartLayout.FACE, QuinceStartLayout.FACE, QuinceStartLayout.FACE));
    }

    /* The fruit, three times its size, centred on (cx, cy), in its own colours. */
    private static void fruit(final GuiGraphics g, final ResourceLocation fruit, final int cx, final int cy) {
        final int size = QuinceStartLayout.FRUIT * QuinceStartLayout.FRUIT_SCALE;
        Draw.blended(() -> g.blit(fruit, cx - size / 2, cy - size / 2, size, size, 0.0F, 0.0F,
                QuinceStartLayout.FRUIT, QuinceStartLayout.FRUIT, QuinceStartLayout.FRUIT, QuinceStartLayout.FRUIT));
    }

    /* The same fruit drawn white and tinted with {@code ink}: the Sad Quince in its firmware's ink. */
    private static void tinted(final GuiGraphics g, final ResourceLocation fruit, final int cx, final int cy,
                               final int ink) {
        g.setColor((ink >> 16 & 0xFF) / 255.0F, (ink >> 8 & 0xFF) / 255.0F, (ink & 0xFF) / 255.0F, 1.0F);
        fruit(g, fruit, cx, cy);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /* The spinner: twelve spokes from its ground's colour to its ink, the darkest going round with the ticks. */
    private static void spinner(final GuiGraphics g, final int cx, final int cy, final int ground, final int ink,
                                final int ticks) {
        final int r = QuinceStartLayout.SPINNER_RADIUS;
        final int lead = ticks * SPOKES / TICKS_A_TURN;
        for (int i = 0; i < SPOKES; i++) {
            final double angle = Math.toRadians(i * 30.0 - 90.0);
            final float t = 0.25F + 0.75F * Math.floorMod(i - lead, SPOKES) / (SPOKES - 1.0F);
            final int colour = mix(ground, ink, t);
            for (int s = r / 2; s < r; s++) {
                final int px = (int) Math.round(cx + s * Math.cos(angle));
                final int py = (int) Math.round(cy + s * Math.sin(angle));
                g.fill(px, py, px + 2, py + 1, colour);
            }
        }
    }

    private static int mix(final int from, final int to, final float t) {
        // The ground's own alpha, the three channels between the two.
        int out = from >>> 24 << 24;
        for (int shift = 0; shift <= 16; shift += 8) {
            final int a = from >> shift & 0xFF;
            final int b = to >> shift & 0xFF;
            out |= Math.round(a + (b - a) * t) << shift;
        }
        return out;
    }

    private static void frame(final GuiGraphics g, final int x0, final int y0, final int x1, final int y1,
                              final int colour) {
        g.fill(x0, y0, x1, y0 + 1, colour);
        g.fill(x0, y1 - 1, x1, y1, colour);
        g.fill(x0, y0, x0 + 1, y1, colour);
        g.fill(x1 - 1, y0, x1, y1, colour);
    }

    /**
     * The pages' colours: Open Firmware's start grey; the EFI's grey and its spinner; the black start and its bar;
     * the Sad Quince's ground and ink for the old firmwares, the Transition's grey, the Standard's dark and the chip's
     * black; the choosers' slates, top and bottom, and the chip's black; the volumes' names, the pick and its frame,
     * the button and its word, the Startup Options' line; Open Firmware's page; the no-boot box, light and dark.
     */
    private record Colours(int legacyGround, int efiGrey, int spinnerInk, int black, int barTrack, int barFill,
                           int sadOldGround, int sadOldInk, int sadGreyGround, int sadGreyInk, int sadDarkGround,
                           int sadDarkInk, int sadChipGround, int sadChipInk, int chooserEarlyTop,
                           int chooserEarlyBottom, int chooserLaterTop, int chooserLaterBottom, int chooserChip,
                           int chooserText, int pickFill, int pickFrame, int buttonFill, int buttonText,
                           int optionsLine, int ofGround, int ofInk, int noBootPaperLight, int noBootInkLight,
                           int noBootPaperDark, int noBootInkDark) {
    }
}
