/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.DataLinkNames;
import dev.jstech.computers.gui.layout.NetworkLinksLayout;
import dev.jstech.computers.operation.payload.NetworkNodeInfo;
import dev.jstech.computers.operation.payload.NodeLink;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.network.DataLink;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * How the Network Manager draws its nodes' links: a link's words in the LINK column, the line it is drawn with on the
 * Map (the fibre thick and aqua, a copper backbone, the access line thin, an earlier era's cable in dashes, the
 * crafting cable amber, a link down red and dashed), the legend that explains them, the optical routers, the hover
 * card's NETWORK lines and the line under the Devices list that says why a node lost its link.
 */
@PaletteHolder
final class NetworkLinkDrawing {

    /** How a link is drawn: its colour, how thick, and whether in dashes. */
    record Style(int colour, int thickness, boolean dashed) {
    }

    /** What the Map's legend explains, each line by its words, and how big it is drawn; none at all when empty. */
    record Legend(Map<String, Style> lines, boolean card, boolean router, int width, int height) {
    }

    /** The lines' colours on the Map and in the LINK column, a link down's, and the words of a link that is neither. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/network_links",
            new Colours(0xFF1FB8C8, 0xFF6B7FB0, 0xFF9FB4E6, 0xFFC94FB0, 0xFF8A6F3A, 0xFFD98A3A, 0xFFD1495B,
                    0xFF8A8F9A));
    private static final int DASH = 4;
    private static final int DASH_GAP = 3;
    private static final int ROUTER = 9;

    private NetworkLinkDrawing() {
    }

    /** A link in the LINK column: its cable and speed, or that it is down; nothing for a link the screen cannot tell. */
    static String words(final NodeLink link) {
        final DataLink cable = link.dataLink();
        if (cable == null) {
            return "";
        }
        final Text name = DataLinkNames.of(cable).text();
        return GameText.resolve(link.up() ? NetworkLinkTexts.SPEED.with(name, JsTechTheme.fmt(cable.throughput()))
                : NetworkLinkTexts.DOWN.with(name));
    }

    /**
     * A link on the Hardware tab, by its cable's serialized name: the cable and its speed, with the node it is on when
     * {@code node} names one; the word for none when there is no link.
     */
    static String hardwareValue(final String link, final String node) {
        final DataLink cable = link.isEmpty() ? null : DataLink.byName(link);
        if (cable == null) {
            return GameText.resolve(NetworkLinkTexts.NONE);
        }
        final Text name = DataLinkNames.of(cable).text();
        final String speed = JsTechTheme.fmt(cable.throughput());
        return GameText.resolve(node.isEmpty() ? NetworkLinkTexts.LINK_VALUE.with(name, speed)
                : NetworkLinkTexts.SLOWEST_VALUE.with(name, speed, node));
    }

    /** The colour of a link's words: aqua for the fibre, red for a link down, grey for the rest. */
    static int wordsColour(final NodeLink link) {
        final DataLink cable = link.dataLink();
        if (!link.up()) {
            return colours().down();
        }
        return cable != null && cable.straight() ? colours().fibre() : colours().other();
    }

    /** How a link is drawn on a network whose Mainframe is of {@code era}: an earlier era's cable in dashes. */
    static Style style(final NodeLink link, @Nullable final HardwareEra era) {
        final DataLink cable = link.dataLink();
        if (!link.up()) {
            return new Style(colours().down(), 1, true);
        }
        if (cable == null) {
            return new Style(colours().access(), 1, false);
        }
        final boolean older = era != null && cable.era().level() < era.level();
        return switch (cable.line()) {
            case BACKBONE -> cable.straight() ? new Style(colours().fibre(), 2, false)
                    : new Style(colours().copper(), 2, older);
            case ACCESS -> new Style(colours().access(), 1, older);
            case HPC -> new Style(colours().compute(), 2, false);
            case LONG_DISTANCE -> new Style(colours().longDistance(), 2, older);
            case CRAFTING -> new Style(colours().crafting(), 1, false);
        };
    }

    /** The colour of the fibre, which marks an Optical Network Card and an optical router as well. */
    static int fibre() {
        return colours().fibre();
    }

    /** The colour of a link down, and of the words that say so. */
    static int down() {
        return colours().down();
    }

    /** A link as the Map draws them: a horizontal leg along {@code y1}, then a vertical one down to {@code y2}. */
    static void draw(final GuiGraphics g, final int x1, final int y1, final int x2, final int y2, final Style style) {
        horizontal(g, Math.min(x1, x2), Math.max(x1, x2), y1, style);
        vertical(g, x2, Math.min(y1, y2), Math.max(y1, y2), style);
    }

    /** The square that marks a node with an Optical Network Card, its corner at {@code x}, {@code y}. */
    static void cardSquare(final GuiGraphics g, final int x, final int y) {
        g.fill(x, y, x + NetworkLinksLayout.CARD_SQUARE, y + NetworkLinksLayout.CARD_SQUARE, colours().fibre());
    }

    /** An optical router on the Map: a small box of the fibre's colour, centred on {@code cx}, {@code cy}. */
    static void router(final GuiGraphics g, final int cx, final int cy, final int ground) {
        final int x = cx - ROUTER / 2;
        final int y = cy - ROUTER / 2;
        g.fill(x, y, x + ROUTER, y + ROUTER, ground);
        Draw.outline(g, x, y, ROUTER, ROUTER, colours().fibre());
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, colours().fibre());
    }

    /** A place as the screens say it, its x, y and z. */
    static String place(final long pos) {
        final BlockPos at = BlockPos.of(pos);
        return GameText.resolve(NetworkLinkTexts.PLACE.with(at.getX(), at.getY(), at.getZ()));
    }

    /** Why {@code node} lost its link, the line under the Devices list says; empty for a node whose link is up. */
    static String reason(final NetworkNodeInfo node) {
        final NodeLink link = node.link();
        final DataLink cable = link.dataLink();
        if (link.reason() == NodeLink.REASON_BENDS) {
            return GameText.resolve(NetworkLinkTexts.BENDS.with(node.displayName(), place(link.where())));
        }
        if (link.reason() == NodeLink.REASON_TOO_LONG && cable != null) {
            return GameText.resolve(NetworkLinkTexts.TOO_LONG.with(node.displayName(), link.runLength(),
                    DataLinkNames.of(cable).text(), cable.range()));
        }
        return "";
    }

    /** The hover card's NETWORK lines for {@code link}: none for a node whose link the screen cannot tell. */
    static List<String> cardLines(final NodeLink link) {
        final DataLink cable = link.dataLink();
        final List<String> lines = new ArrayList<>();
        if (cable == null && !link.optical()) {
            return lines;
        }
        lines.add(GameText.resolve(NetworkLinkTexts.SECTION));
        if (link.optical()) {
            lines.add(GameText.resolve(NetworkLinkTexts.OPTICAL_CARD));
        }
        if (cable == null) {
            return lines;
        }
        if (link.up()) {
            final Text name = DataLinkNames.of(cable).text();
            final Text word = lineWord(cable);
            final String speed = JsTechTheme.fmt(cable.throughput());
            lines.add(GameText.resolve(word == null ? NetworkLinkTexts.CARD_NAMED.with(name, speed)
                    : NetworkLinkTexts.CARD_LINK.with(name, word, speed)));
            lines.add(GameText.resolve(NetworkLinkTexts.RUN.with(link.runLength(), cable.range())));
            if (link.router() != NodeLink.NO_PLACE) {
                lines.add(GameText.resolve(NetworkLinkTexts.THROUGH.with(place(link.router()))));
            }
        } else if (link.reason() == NodeLink.REASON_BENDS) {
            lines.add(GameText.resolve(NetworkLinkTexts.CARD_BENDS.with(place(link.where()))));
        } else {
            lines.add(GameText.resolve(NetworkLinkTexts.CARD_TOO_LONG.with(link.runLength(), cable.range())));
        }
        return lines;
    }

    /**
     * What the Map's legend explains for {@code nodes}: a line for each cable they are linked by, fastest first, then a
     * link down, the card's square and the optical router, each only where the Map shows it; and its size.
     */
    static Legend legend(final Font font, final List<NetworkNodeInfo> nodes, @Nullable final HardwareEra era) {
        final Map<String, Style> lines = new LinkedHashMap<>();
        final List<NodeLink> up = new ArrayList<>();
        boolean down = false;
        boolean card = false;
        boolean router = false;
        for (final NetworkNodeInfo node : nodes) {
            final NodeLink link = node.link();
            down |= !link.up();
            card |= link.optical();
            router |= link.router() != NodeLink.NO_PLACE;
            if (link.up() && link.dataLink() != null) {
                up.add(link);
            }
        }
        up.sort(Comparator.comparingLong((NodeLink link) -> -link.dataLink().throughput()));
        for (final NodeLink link : up) {
            lines.putIfAbsent(legendWords(link.dataLink()), style(link, era));
        }
        if (down) {
            lines.put(GameText.resolve(NetworkLinkTexts.MAP_DOWN), style(new NodeLink("", false, false, 0,
                    NodeLink.REASON_NONE, NodeLink.NO_PLACE, NodeLink.NO_PLACE), era));
        }
        final int entries = lines.size() + (card ? 1 : 0) + (router ? 1 : 0);
        int widest = font.width(GameText.resolve(NetworkLinkTexts.LEGEND));
        for (final String words : lines.keySet()) {
            widest = Math.max(widest, font.width(words));
        }
        widest = Math.max(widest, Math.max(card ? font.width(GameText.resolve(NetworkLinkTexts.OPTICAL_CARD)) : 0,
                router ? font.width(GameText.resolve(NetworkLinkTexts.OPTICAL_ROUTER)) : 0));
        return new Legend(lines, card, router, entries == 0 ? 0 : NetworkLinksLayout.legendW(widest),
                entries == 0 ? 0 : NetworkLinksLayout.legendH(entries));
    }

    /** Draws {@code legend} with its top right corner at {@code right}, {@code top}; nothing for an empty one. */
    static void drawLegend(final GuiGraphics g, final Font font, final Legend legend, final int right, final int top,
                           final int ground, final int text, final int dim) {
        if (legend.width() == 0) {
            return;
        }
        final int x = right - legend.width();
        final int pad = NetworkLinksLayout.LEGEND_PAD;
        g.fill(x, top, right, top + legend.height(), ground);
        Draw.outline(g, x, top, legend.width(), legend.height(), colours().fibre());
        Draw.text(g, font, GameText.resolve(NetworkLinkTexts.LEGEND), x + pad, top + pad, dim, ground);
        final int wordsX = x + pad + NetworkLinksLayout.LEGEND_SWATCH_W + 4;
        int y = top + pad + NetworkLinksLayout.LINE_H;
        for (final Map.Entry<String, Style> line : legend.lines().entrySet()) {
            horizontal(g, x + pad, x + pad + NetworkLinksLayout.LEGEND_SWATCH_W, y + 4, line.getValue());
            Draw.text(g, font, line.getKey(), wordsX, y, text, ground);
            y += NetworkLinksLayout.LINE_H;
        }
        if (legend.card()) {
            cardSquare(g, x + pad + 4, y + 2);
            Draw.text(g, font, GameText.resolve(NetworkLinkTexts.OPTICAL_CARD), wordsX, y, text, ground);
            y += NetworkLinksLayout.LINE_H;
        }
        if (legend.router()) {
            router(g, x + pad + NetworkLinksLayout.LEGEND_SWATCH_W / 2, y + 4, ground);
            Draw.text(g, font, GameText.resolve(NetworkLinkTexts.OPTICAL_ROUTER), wordsX, y, text, ground);
        }
    }

    /* A cable as the legend names it: its short name and its line, the crafting cable by its name alone. */
    private static String legendWords(final DataLink cable) {
        final Text word = lineWord(cable);
        return word == null ? GameText.resolve(DataLinkNames.of(cable))
                : GameText.resolve(NetworkLinkTexts.LEGEND_ENTRY.with(DataLinkNames.of(cable).text(), word));
    }

    /* The word for a cable's line, or null for the crafting cable, which its name already says. */
    @Nullable
    private static Text lineWord(final DataLink cable) {
        final TextKey word = switch (cable.line()) {
            case ACCESS -> NetworkLinkTexts.ACCESS;
            case BACKBONE -> NetworkLinkTexts.BACKBONE;
            case LONG_DISTANCE -> NetworkLinkTexts.LONG_DISTANCE;
            case HPC -> NetworkLinkTexts.HIGH_COMPUTE;
            case CRAFTING -> null;
        };
        return word == null ? null : word.text();
    }

    private static void horizontal(final GuiGraphics g, final int from, final int to, final int y, final Style style) {
        final int top = y - style.thickness() / 2;
        if (!style.dashed()) {
            g.fill(from, top, to, top + style.thickness(), style.colour());
            return;
        }
        for (int x = from; x < to; x += DASH + DASH_GAP) {
            g.fill(x, top, Math.min(x + DASH, to), top + style.thickness(), style.colour());
        }
    }

    private static void vertical(final GuiGraphics g, final int x, final int from, final int to, final Style style) {
        final int left = x - style.thickness() / 2;
        if (!style.dashed()) {
            g.fill(left, from, left + style.thickness(), to, style.colour());
            return;
        }
        for (int y = from; y < to; y += DASH + DASH_GAP) {
            g.fill(left, y, left + style.thickness(), Math.min(y + DASH, to), style.colour());
        }
    }

    private static Colours colours() {
        return PALETTE.get();
    }

    /**
     * The links' colours: the fibre (and what marks a card or an optical router), a copper backbone, the access line,
     * the high compute fabric, a long distance line, the crafting cable, a link down, and the words of any other link.
     */
    private record Colours(int fibre, int copper, int access, int compute, int longDistance, int crafting, int down,
                           int other) {
    }
}
