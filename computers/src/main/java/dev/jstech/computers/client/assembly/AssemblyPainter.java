/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.assembly;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.layout.AssemblyGeometry;
import dev.jstech.computers.gui.layout.AssemblyGeometry.Box;
import dev.jstech.computers.gui.layout.AssemblyGeometry.Seat;
import dev.jstech.computers.gui.layout.AssemblyGeometry.SeatKind;
import dev.jstech.computers.hardware.ClusterInterfaceCardSpec;
import dev.jstech.computers.hardware.CraftingCardSpec;
import dev.jstech.computers.hardware.GpuSpec;
import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.hardware.SoundCardSpec;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.core.jstechseries.theme.client.JsTechTheme;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Draws an assembly screen as the machine: the open case from its own pictures (the case, and the board over its tray
 * once one is in), each card lying along its slot as its 3D model shows it with its bracket on the back panel, every
 * seat with the shape of the part it takes when it is empty, and the front panel's and the service panel's pieces (the
 * power button, the readouts, the switches, the lamps). The pictures are drawn by the art scripts from the same code
 * the approved screens were drawn with, one set per era.
 */
@PaletteHolder
public final class AssemblyPainter {

    /* Whether each card has a picture, asked of the resources once per card. */
    private static final Map<ResourceLocation, Boolean> HAS_PICTURE = new HashMap<>();
    /** The light a lit lamp's glass catches in its corner. */
    private static final Palette<LampLight> LAMP_LIGHT = Palettes.declare(JsComputers.MODID, "screen/assembly_lamp",
            new LampLight(0xFFFFFFFF));

    private static final ResourceLocation GHOSTS = tex("ghosts");
    private static final int GHOST = 16;
    private static final int GHOST_ALPHA = 110;
    private static final int DIMMED_GHOST_ALPHA = 55;
    private static final int BRACKET_GPU = 0;
    private static final int BRACKET_AUDIO = 1;
    private static final int BRACKET_CRAFTING = 2;
    private static final int BRACKET_CLUSTER = 3;
    private static final int BRACKET_PLAIN = 4;
    private static final int BRACKETS = 5;
    private static final int LAMP = 4;

    private AssemblyPainter() {
    }

    /** The case, from its picture: bare, with the tray's standoffs showing, or with the board over its tray. */
    public static void drawCase(final GuiGraphics g, final int left, final int top, final AssemblyGeometry geometry,
                                final Era era, final boolean boardIn) {
        final Box box = geometry.caseBox();
        final String kind = geometry.kind().name().toLowerCase(Locale.ROOT);
        // The pictures are the five eras with parts'; a later era is drawn as the one it wears.
        final String age = HardwareEra.withParts(era).path();
        final String name = "case_" + caseName(kind) + "_" + age + (boardIn ? "" : "_bare");
        blit(g, tex(name), left + box.x(), top + box.y(), box.w(), box.h());
        if (boardIn) {
            blit(g, tex("board_" + caseName(kind) + "_" + age), left + box.x(), top + box.y(), box.w(), box.h());
        }
    }

    /** A card lying along its slot, as its 3D model shows it, when the card has a picture; nothing otherwise. */
    public static void drawCard(final GuiGraphics g, final int left, final int top, final Seat seat,
                                final ItemStack card) {
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(card.getItem());
        final ResourceLocation picture = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                "textures/gui/assembly/card/" + id.getPath() + ".png");
        if (!JsComputers.MODID.equals(id.getNamespace()) || !hasPicture(picture)) {
            return;
        }
        blit(g, picture, left + seat.x() + AssemblyGeometry.CARD_DX, top + seat.y() + AssemblyGeometry.CARD_DY,
                AssemblyGeometry.CARD_W, AssemblyGeometry.CARD_H);
    }

    /** The bracket a card shows on the back panel, by what the card is: its connectors in the era's case metal. */
    public static void drawBracket(final GuiGraphics g, final int left, final int top, final Seat seat,
                                   final ItemStack card, final Era era) {
        final int index = bracketOf(card);
        g.blit(tex("brackets_" + HardwareEra.withParts(era).path()), left + AssemblyGeometry.BRACKET_X,
                top + seat.y() + 2, index * AssemblyGeometry.BRACKET_W, 0, AssemblyGeometry.BRACKET_W,
                AssemblyGeometry.BRACKET_H, AssemblyGeometry.BRACKET_W * BRACKETS, AssemblyGeometry.BRACKET_H);
    }

    /**
     * A seat's frame, dimmed when the board offers no part for it, and the shape of the part it takes when it holds
     * none.
     */
    public static void drawSeat(final GuiGraphics g, final int left, final int top, final Seat seat,
                                final boolean active, final boolean empty) {
        final int x = left + seat.x();
        final int y = top + seat.y();
        JsTechTheme.slot(g, x, y);
        if (!active) {
            g.fill(x - 1, y - 1, x + 17, y + 17, withAlpha(JsTechTheme.screen(), 128));
        }
        if (empty) {
            drawGhost(g, x, y, seat.kind(), JsTechTheme.dim(), active ? GHOST_ALPHA : DIMMED_GHOST_ALPHA);
        }
    }

    /** The shape of the part a seat of that kind takes, in that colour at that alpha, for a seat holding none. */
    public static void drawGhost(final GuiGraphics g, final int x, final int y, final SeatKind kind, final int colour,
                                 final int alpha) {
        RenderSystem.enableBlend();
        g.setColor(((colour >> 16) & 255) / 255f, ((colour >> 8) & 255) / 255f, (colour & 255) / 255f, alpha / 255f);
        g.blit(GHOSTS, x, y, ghostOf(kind) * GHOST, 0, GHOST, GHOST, GHOST * 8, GHOST);
        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    /** The power button: its face, its lamp, and its word beside the lamp. */
    public static void drawPower(final GuiGraphics g, final Font font, final int left, final int top, final Box box,
                                 final String word, final boolean hovered, final boolean active, final int lamp) {
        final int x = left + box.x();
        final int y = top + box.y();
        JsTechTheme.button(g, x, y, box.w(), box.h(), hovered && active);
        drawLamp(g, x + 5, y + 6, lamp, true);
        JsTechTheme.text(g, font, word, x + 14, y + 4, active ? JsTechTheme.text() : JsTechTheme.dim());
    }

    /** One instrument of the front panel: its label small over its value, set right; a long value in small type. */
    public static void drawReadout(final GuiGraphics g, final Font font, final int left, final int top,
                                   final Box box, final String label, final String value, final int colour) {
        final int x = left + box.x();
        final int y = top + box.y();
        JsTechTheme.panel(g, x, y, box.w(), box.h());
        JsTechTheme.textS(g, font, label, x + 3, y + 2, JsTechTheme.dim());
        if (font.width(value) <= box.w() - 6) {
            JsTechTheme.textRight(g, font, value, x + box.w() - 3, y + 9, colour);
        } else {
            JsTechTheme.textSRight(g, font, value, x + box.w() - 3, y + 10, colour);
        }
    }

    /** A rocker of the service panel: its caption left, its knob thrown right when it is on. */
    public static void drawSwitch(final GuiGraphics g, final Font font, final int left, final int top, final Box box,
                                  final String caption, final boolean on) {
        final int x = left + box.x();
        final int y = top + box.y();
        JsTechTheme.panel(g, x, y, box.w(), box.h());
        JsTechTheme.textS(g, font, caption, x + 3, y + 3, JsTechTheme.dim());
        final int kx = x + box.w() - 15;
        g.fill(kx, y + 2, kx + 12, y + 11, JsTechTheme.track());
        frame(g, kx, y + 2, kx + 12, y + 11, JsTechTheme.line());
        final int knob = on ? kx + 6 : kx + 1;
        g.fill(knob, y + 3, knob + 5, y + 10, on ? JsTechTheme.accent() : JsTechTheme.dim());
    }

    /** The lamps' panel: a lamp over each caption, lit or dark. */
    public static void drawLamps(final GuiGraphics g, final Font font, final int left, final int top, final Box box,
                                 final String[] captions, final boolean[] lit, final int[] colours) {
        final int x = left + box.x();
        final int y = top + box.y();
        JsTechTheme.panel(g, x, y, box.w(), box.h());
        final int third = box.w() / captions.length;
        for (int i = 0; i < captions.length; i++) {
            final int cx = x + i * third + third / 2;
            drawLamp(g, cx - 2, y + 4, colours[i], lit[i]);
            JsTechTheme.textSCenter(g, font, captions[i], cx, y + 11, JsTechTheme.dim());
        }
    }

    /** A lamp, lit or dark, its highlight in its top left corner when lit. */
    public static void drawLamp(final GuiGraphics g, final int x, final int y, final int colour, final boolean lit) {
        g.fill(x, y, x + LAMP, y + LAMP, lit ? colour : mix(colour, JsTechTheme.screen(), 0.75f));
        if (lit) {
            g.fill(x + 1, y + 1, x + 2, y + 2, mix(colour, LAMP_LIGHT.get().glint(), 0.5f));
        }
    }

    /** A one-pixel frame round [x0, x1) by [y0, y1). */
    public static void frame(final GuiGraphics g, final int x0, final int y0, final int x1, final int y1,
                             final int colour) {
        g.fill(x0, y0, x1, y0 + 1, colour);
        g.fill(x0, y1 - 1, x1, y1, colour);
        g.fill(x0, y0, x0 + 1, y1, colour);
        g.fill(x1 - 1, y0, x1, y1, colour);
    }

    /** Two colours mixed: {@code t} of the way from {@code a} to {@code b}, opaque. */
    public static int mix(final int a, final int b, final float t) {
        final int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        final int gr = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        final int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
        return (255 << 24) | (r << 16) | (gr << 8) | bl;
    }

    /** A colour with another alpha. */
    public static int withAlpha(final int colour, final int alpha) {
        return (colour & 0xFFFFFF) | (alpha << 24);
    }

    /* The cases' pictures are named by the case the towers share, the Mainframe's and the server's. */
    private static String caseName(final String kind) {
        return "tower".equals(kind) ? "small" : kind;
    }

    private static int bracketOf(final ItemStack card) {
        final IExpansionCardSpec spec = HardwareRegistry.card(card);
        if (spec instanceof GpuSpec) {
            return BRACKET_GPU;
        }
        if (spec instanceof SoundCardSpec) {
            return BRACKET_AUDIO;
        }
        if (spec instanceof CraftingCardSpec) {
            return BRACKET_CRAFTING;
        }
        if (spec instanceof ClusterInterfaceCardSpec) {
            return BRACKET_CLUSTER;
        }
        return BRACKET_PLAIN;
    }

    private static int ghostOf(final SeatKind kind) {
        return switch (kind) {
            case BOARD -> 0;
            case CPU -> 1;
            case RAM -> 2;
            case CARD -> 3;
            case PSU -> 4;
            case DISK -> 5;
            case BAY -> 6;
        };
    }

    private static void blit(final GuiGraphics g, final ResourceLocation texture, final int x, final int y,
                             final int w, final int h) {
        RenderSystem.enableBlend();
        g.blit(texture, x, y, 0, 0, w, h, w, h);
        RenderSystem.disableBlend();
    }

    private static boolean hasPicture(final ResourceLocation picture) {
        return HAS_PICTURE.computeIfAbsent(picture,
                p -> Minecraft.getInstance().getResourceManager().getResource(p).isPresent());
    }

    private static ResourceLocation tex(final String name) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "textures/gui/assembly/" + name + ".png");
    }

    /** The light in a lit lamp's corner. */
    private record LampLight(int glint) {
    }
}
