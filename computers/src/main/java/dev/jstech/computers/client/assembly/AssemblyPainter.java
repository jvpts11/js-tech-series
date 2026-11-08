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
import dev.jstech.computers.hardware.PsuCondition;
import dev.jstech.computers.hardware.PsuSpec;
import dev.jstech.computers.hardware.PsuWear;
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
    /** The power supply's wear bar, as the game draws an item's durability: its track, then what is left of it. */
    private static final Palette<WearBar> WEAR_BAR = Palettes.declare(JsComputers.MODID, "screen/psu_wear",
            new WearBar(0xFF000000, 0xFF3EE04A, 0xFFE0D22A, 0xFFFF8A1A, 0xFFFF3A2A, 0x78280000));
    /** A water cooler's radiator on the cooling face. */
    private static final Palette<Radiator> RADIATOR = Palettes.declare(JsComputers.MODID, "screen/assembly_radiator",
            new Radiator(0xFF2A2C31, 0xFF5F6164, 0xFF111214, 0xFF4A4D55, 0xFFD6D9DD, 0xFFA1A3A6, 0xFF9AA0A8,
                    0xFFF3F3F3, 0xFF111214));
    private static final int RADIATOR_SEATS = 2;
    private static final int RADIATOR_RING = 6;
    private static final int RADIATOR_RING_DOTS = 20;
    private static final int RADIATOR_BLADE = 5;
    private static final int RADIATOR_BLADES = 7;
    private static final double BLADE_PHASE = 0.3;
    private static final double BLADE_SWEEP = 0.12;
    private static final int FAN_RING_DOTS = 14;
    private static final int COOLING_GHOST_ALPHA = 120;
    private static final int WEAR_BAR_X = 2;
    private static final int WEAR_BAR_Y = 13;
    private static final int WEAR_BAR_W = 13;
    private static final float HALF_WORN = 0.5f;

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
        if (empty && seat.kind() == SeatKind.COOLER) {
            drawCoolerGhost(g, x, y, withAlpha(JsTechTheme.dim(), COOLING_GHOST_ALPHA));
        } else if (empty && seat.kind() == SeatKind.FAN) {
            drawFanGhost(g, x, y, withAlpha(JsTechTheme.dim(), COOLING_GHOST_ALPHA));
        } else if (empty) {
            drawGhost(g, x, y, seat.kind(), JsTechTheme.dim(), active ? GHOST_ALPHA : DIMMED_GHOST_ALPHA);
        }
    }

    /**
     * A water cooler's radiator over a pair of fan seats, the first at ({@code x}, {@code y}): its fins, its end tanks,
     * its two fans' rings over the fins, along the top when {@code horizontal} and down the front otherwise; then its
     * two hoses to the pump over the processor at ({@code pumpX}, {@code pumpY}). The Standard's are black, the
     * Advanced's white, as their coolers are.
     */
    public static void drawRadiator(final GuiGraphics g, final int x, final int y, final boolean horizontal,
                                    final Era era, final int pumpX, final int pumpY) {
        final Radiator colours = RADIATOR.get();
        final boolean dark = HardwareEra.partsIndex(era) <= HardwareEra.partsIndex(HardwareEra.STANDARD);
        final int fin = dark ? colours.finDark() : colours.finLight();
        final int line = dark ? colours.lineDark() : colours.lineLight();
        final int tank = dark ? colours.tankDark() : colours.tankLight();
        final int blade = dark ? colours.bladeDark() : colours.bladeLight();
        final int length = AssemblyGeometry.PITCH * RADIATOR_SEATS - 2;
        final int w = horizontal ? length : GHOST;
        final int h = horizontal ? GHOST : length;
        frame(g, x - 1, y - 1, x + w + 1, y + h + 1, JsTechTheme.slotEdge());
        g.fill(x, y, x + w, y + h, fin);
        if (horizontal) {
            for (int xx = x + 3; xx < x + w - 3; xx += 2) {
                g.fill(xx, y + 1, xx + 1, y + h - 1, line);
            }
            g.fill(x, y, x + 3, y + h, tank);
            g.fill(x + w - 3, y, x + w, y + h, tank);
        } else {
            for (int yy = y + 3; yy < y + h - 3; yy += 2) {
                g.fill(x + 1, yy, x + w - 1, yy + 1, line);
            }
            g.fill(x, y, x + w, y + 3, tank);
            g.fill(x, y + h - 3, x + w, y + h, tank);
        }
        for (int i = 0; i < RADIATOR_SEATS; i++) {
            final int cx = horizontal ? x + 3 + (w - 6) * (2 * i + 1) / (2 * RADIATOR_SEATS) : x + w / 2;
            final int cy = horizontal ? y + h / 2 : y + 3 + (h - 6) * (2 * i + 1) / (2 * RADIATOR_SEATS);
            ring(g, cx, cy, RADIATOR_RING, mix(blade, colours.hose(), 0.2f), RADIATOR_RING_DOTS);
            blades(g, cx, cy, RADIATOR_BLADE, blade);
            g.fill(cx - 1, cy - 1, cx + 2, cy + 2, mix(blade, colours.hose(), 0.4f));
        }
        // The two hoses from the pump, along the top of the case to the radiator's near tank.
        final int px = pumpX + 6;
        final int py = pumpY - 1;
        for (int k = 0; k < 2; k++) {
            final int hy = py - 2 - k * 2;
            g.fill(Math.min(px + k * 5, x + 1), hy, Math.max(px + k * 5, x + 1) + 1, hy + 1, colours.hose());
            g.fill(px + k * 5, hy, px + k * 5 + 1, py + 1, colours.hose());
        }
    }

    /** The edge round the power supply's seat: amber once it is degraded, red once it has failed. */
    public static void drawPsuEdge(final GuiGraphics g, final int left, final int top, final Seat seat,
                                   final PsuSpec spec, final PsuCondition condition) {
        if (condition.failed(spec) || condition.degraded(spec)) {
            final int x = left + seat.x();
            final int y = top + seat.y();
            frame(g, x - 1, y - 1, x + 17, y + 17, condition.failed(spec) ? JsTechTheme.red() : JsTechTheme.amber());
        }
    }

    /**
     * The power supply's wear under its icon at ({@code x}, {@code y}), drawn over the item as the game draws its
     * durability: a failed supply darkened, and a redundant one's bar split into its units.
     */
    public static void drawPsuWear(final GuiGraphics g, final int x, final int y, final PsuSpec spec,
                                   final PsuCondition condition) {
        final WearBar colours = WEAR_BAR.get();
        if (condition.failed(spec)) {
            g.fill(x, y, x + 16, y + 16, colours.failedShade());
        }
        g.fill(x + WEAR_BAR_X, y + WEAR_BAR_Y, x + WEAR_BAR_X + WEAR_BAR_W, y + WEAR_BAR_Y + 2, colours.track());
        final int units = spec.units();
        final int unitW = (WEAR_BAR_W - (units - 1)) / units;
        for (int unit = 0; unit < units; unit++) {
            final float wear = condition.wearOf(unit);
            final int ux = x + WEAR_BAR_X + unit * (unitW + 1);
            // A failed unit of a redundant supply shows red; a supply of one unit that failed shows only its track.
            final int left = wear >= 1f ? (units > 1 ? unitW : 0) : Math.round(unitW * (1f - wear));
            g.fill(ux, y + WEAR_BAR_Y, ux + left, y + WEAR_BAR_Y + 1, wearColour(colours, wear));
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

    /**
     * A small gauge beside a readout's label, in what is left of its top row: its track, filled to {@code fraction} in
     * {@code colour}, and a mark at each of {@code marks}.
     */
    public static void drawGauge(final GuiGraphics g, final Font font, final int left, final int top, final Box box,
                                 final String label, final double fraction, final int colour, final double... marks) {
        final int x0 = left + box.x() + 3 + JsTechTheme.widthS(font, label) + 4;
        final int x1 = left + box.x() + box.w() - 3;
        final int y = top + box.y();
        if (x1 - x0 < 4) {
            return;
        }
        g.fill(x0, y + 3, x1, y + 6, JsTechTheme.track());
        frame(g, x0, y + 3, x1, y + 6, JsTechTheme.line());
        final int filled = x0 + 1 + (int) Math.round((x1 - x0 - 2) * Math.clamp(fraction, 0.0, 1.0));
        if (filled > x0 + 1) {
            g.fill(x0 + 1, y + 4, filled, y + 5, colour);
        }
        for (final double mark : marks) {
            final int mx = x0 + 1 + (int) Math.round((x1 - x0 - 2) * mark);
            g.fill(mx, y + 2, mx + 1, y + 7, JsTechTheme.dim());
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

    /* An empty fan seat's shape: the fan's square frame, its ring, its hub. */
    private static void drawFanGhost(final GuiGraphics g, final int x, final int y, final int colour) {
        frame(g, x + 1, y + 1, x + 15, y + 15, colour);
        ring(g, x + 7.5, y + 7.5, 4.6, colour, FAN_RING_DOTS);
        g.fill(x + 8, y + 8, x + 9, y + 9, colour);
    }

    /* An empty cooler seat's shape: a heatsink seen from above, its frame and its fins. */
    private static void drawCoolerGhost(final GuiGraphics g, final int x, final int y, final int colour) {
        frame(g, x + 2, y + 3, x + 14, y + 13, colour);
        for (int xx = 4; xx < 13; xx += 2) {
            g.fill(x + xx, y + 5, x + xx + 1, y + 11, colour);
        }
    }

    /* A ring of {@code dots} pixels round ({@code cx}, {@code cy}). */
    private static void ring(final GuiGraphics g, final double cx, final double cy, final double radius,
                             final int colour, final int dots) {
        for (int i = 0; i < dots; i++) {
            final double t = 2 * Math.PI * i / dots;
            final int px = (int) Math.round(cx + radius * Math.cos(t));
            final int py = (int) Math.round(cy + radius * Math.sin(t));
            g.fill(px, py, px + 1, py + 1, colour);
        }
    }

    /* A fan's seven blades: short swept strokes from the hub out. */
    private static void blades(final GuiGraphics g, final int cx, final int cy, final int radius, final int colour) {
        for (int i = 0; i < RADIATOR_BLADES; i++) {
            final double t = 2 * Math.PI * i / RADIATOR_BLADES + BLADE_PHASE;
            for (int k = 2; k <= radius; k++) {
                final double sweep = t + BLADE_SWEEP * k;
                final int px = (int) Math.round(cx + k * Math.cos(sweep));
                final int py = (int) Math.round(cy + k * Math.sin(sweep));
                g.fill(px, py, px + 1, py + 1, colour);
            }
        }
    }

    /* What is left of a unit: green past half, yellow to the warning, then orange; red once it has failed. */
    private static int wearColour(final WearBar colours, final float wear) {
        if (wear >= 1f) {
            return colours.failed();
        }
        if (wear >= PsuWear.DEGRADED) {
            return colours.degraded();
        }
        return wear >= HALF_WORN ? colours.half() : colours.good();
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
            // The cooling seats' shapes are drawn rather than read off the sheet: the sheet's blank one stands in.
            case COOLER, FAN -> 7;
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

    private record WearBar(int track, int good, int half, int degraded, int failed, int failedShade) {
    }

    /** A water cooler's radiator: the Standard's black and the Advanced's white fins, tanks and fans, and its hoses. */
    private record Radiator(int finDark, int lineDark, int tankDark, int bladeDark, int finLight, int lineLight,
                            int tankLight, int bladeLight, int hose) {
    }
}
