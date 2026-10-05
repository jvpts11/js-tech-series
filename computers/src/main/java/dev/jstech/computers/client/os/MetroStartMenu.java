/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.layout.TileGrid;
import dev.jstech.computers.operation.payload.SetSettingPayload;
import dev.jstech.computers.os.OsMotions;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.StartTiles;
import dev.jstech.core.client.gui.component.ContextMenu;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Frames 10's Start menu, dark, flush in the corner over the taskbar: the narrow rail at its left (the menu's button at
 * the top; the account, the files, Settings and the power button at the foot), the list beside it (the programs used
 * most this session, then every program from A to Z under its letter, or what matches the search being typed), and
 * the tiles at its right in their group, each a program's white glyph on the accent with its name, small tiles four to
 * the room of a medium one.
 *
 * <p>The tiles are the machine's, arranged by the player: the right button on a tile unpins or resizes it, on a
 * program in the list pins it, and a tile dragged onto another takes its place. The tiles with something to show are
 * live: the Network Manager's turns over between its glyph and the state of the network, the System Monitor's draws
 * the processor's load and the memory held. With "Play animations" off they stop turning.
 *
 * <p>Its colours are {@code jsc:launcher/frames_10}.
 */
@PaletteHolder
final class MetroStartMenu {

    private final DesktopState desktop;
    /** The machine's tiles, as it last told them, changed here at once when the player arranges them. */
    private final List<StartTiles.Tile> tiles = new ArrayList<>();
    /** How many rows the list is scrolled down. */
    private int listScroll;
    /** Whether the rail is unfolded with its words, as its top button unfolds it. */
    private boolean railOpen;
    /** The tile the button went down on, where it went down, and whether it has moved far enough to be a drag. */
    @Nullable
    private String pressedTile;
    private int pressX;
    private int pressY;
    private boolean draggingTile;
    private int dragX;
    private int dragY;
    /** The processor's load as the System Monitor's tile reads it, drifting round what the machine is doing. */
    private double load = 3.0;
    private long lastSample;
    private final Random noise = new Random();

    static final int MENU_W = 272;
    static final int RAIL_W = 18;
    static final int LIST_W = 96;
    static final int CELL = 18;
    static final int ROW_H = 18;
    private static final int TILES_X = RAIL_W + LIST_W + 6;
    private static final int HEADER_H = 14;
    private static final int RAIL_OPEN_W = 80;
    private static final int RAIL_ROW = 18;
    private static final int MOST_USED = 4;
    private static final int DRAG_SLOP = 3;
    /** A live tile turns every seven seconds, the turn taking six tenths of one. */
    private static final long TURN_EVERY_MS = 7_000L;
    private static final long TURN_MS = 600L;
    private static final long SAMPLE_MS = 1_000L;
    /** The look the white glyphs of the tiles are drawn from. */
    private static final String TILE_LOOK = "frames_10_tile";
    private static final String NETWORK_MANAGER = "network_manager";
    private static final String SYSTEM_MONITOR = "system_monitor";

    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "launcher/frames_10",
            new Colours(0xF21F1F1F, 0xFF1F1F1F, 0xFFFFFFFF, 0xFFA6A6A6, 0x1FFFFFFF, 0x8CFFFFFF, 0xCCFFFFFF,
                    0xFF7FC26B, 0xFF6B4A2C));

    MetroStartMenu(final DesktopState desktop) {
        this.desktop = desktop;
    }

    /** Takes the tiles the machine keeps, each {@code <program>:<size>}. */
    void takeTiles(final List<String> encoded) {
        tiles.clear();
        for (final String each : encoded) {
            final StartTiles.Tile tile = StartTiles.parse(each);
            if (tile != null) {
                tiles.add(tile);
            }
        }
    }

    /** The tiles of the programs this machine has, in the player's order, with the program each one starts. */
    List<Shown> shown() {
        final List<Shown> out = new ArrayList<>();
        for (final StartTiles.Tile tile : tiles) {
            final Launcher launcher = launcherFor(tile.id());
            if (launcher != null) {
                out.add(new Shown(tile, launcher));
            }
        }
        return out;
    }

    /** The menu's height: its tiles' grid, never less than the list wants, never more than the desktop has. */
    int height() {
        final int rows = TileGrid.rows(TileGrid.place(sizes(shown())));
        final int tilesH = HEADER_H + rows * CELL + 10;
        final int room = desktop.view().height() - DesktopScreen.TASKBAR_H - 2;
        return Math.max(Math.min(170, room), Math.min(room, tilesH));
    }

    void render(final GuiGraphics g, final int tbY) {
        final Colours c = PALETTE.get();
        final int h = height();
        final int y = tbY - h;
        final boolean solid = desktop.prefs().effects().isOff(OsMotions.TRANSPARENCY);
        g.fill(0, y, MENU_W, tbY, solid ? c.groundSolid() : c.ground());
        drawList(g, c, y, h);
        drawTiles(g, c, y);
        drawRail(g, c, y, h);
    }

    /**
     * A click on the menu: on the rail, the list or a tile. The right button opens the menu of what it lands on; the
     * left one on a tile only marks it, since the tile starts its program when the button comes up on it unmoved.
     */
    boolean click(final int mx, final int my, final int button, final int tbY) {
        final int h = height();
        final int y = tbY - h;
        if (mx < 0 || mx >= MENU_W || my < y || my >= tbY) {
            return false;
        }
        final int railW = railOpen ? RAIL_OPEN_W : RAIL_W;
        if (mx < railW) {
            clickRail(my, y, h);
            return true;
        }
        if (mx < RAIL_W + LIST_W) {
            final Launcher hit = listAt(my, y, h);
            if (hit != null) {
                if (button == 1) {
                    openListMenu(hit, mx, my);
                } else {
                    desktop.start().choose(hit);
                    desktop.start().close();
                }
            }
            return true;
        }
        final Shown tile = tileAt(mx, my, y);
        if (tile != null) {
            if (button == 1) {
                openTileMenu(tile, mx, my);
            } else {
                pressedTile = tile.tile().id();
                pressX = mx;
                pressY = my;
                draggingTile = false;
            }
        }
        return true;
    }

    /** The cursor moved with the button down: a tile pressed and moved past the slop is being dragged. */
    boolean dragged(final double mx, final double my) {
        if (pressedTile == null) {
            return false;
        }
        if (!draggingTile && Math.abs(mx - pressX) + Math.abs(my - pressY) > DRAG_SLOP) {
            draggingTile = true;
        }
        dragX = (int) mx;
        dragY = (int) my;
        return true;
    }

    /**
     * The button came up. On the tile it went down on, unmoved, that tile's program starts; a dragged tile lands in the
     * place of the tile it is let go over, or at the end when it is let go over none.
     */
    boolean released(final double mx, final double my) {
        if (pressedTile == null) {
            return false;
        }
        final String id = pressedTile;
        final boolean moved = draggingTile;
        pressedTile = null;
        draggingTile = false;
        final int tbY = desktop.view().height() - DesktopScreen.TASKBAR_H;
        final int y = tbY - height();
        final Shown over = tileAt((int) mx, (int) my, y);
        if (!moved) {
            if (over != null && over.tile().id().equals(id)) {
                desktop.start().choose(over.launcher());
                desktop.start().close();
            }
            return true;
        }
        final int from = indexOfTile(id);
        if (from < 0) {
            return true;
        }
        final StartTiles.Tile dragged = tiles.get(from);
        final int to = over == null ? tiles.size() - 1 : indexOfTile(over.tile().id());
        place(dragged.id(), dragged.size(), to);
        return true;
    }

    /** The wheel over the list scrolls it a row a notch. */
    boolean scrolled(final double mx, final double my, final double dy) {
        final int tbY = desktop.view().height() - DesktopScreen.TASKBAR_H;
        final int h = height();
        if (mx < RAIL_W || mx >= RAIL_W + LIST_W || my < tbY - h || my >= tbY) {
            return false;
        }
        final int rows = listRows().size();
        final int visible = (h - 8) / ROW_H;
        listScroll = Math.max(0, Math.min(Math.max(0, rows - visible), listScroll + (dy > 0 ? -1 : 1)));
        return true;
    }

    /** The menu was closed or opened afresh: the list goes back to its top and nothing is held. */
    void reset() {
        listScroll = 0;
        railOpen = false;
        pressedTile = null;
        draggingTile = false;
    }

    /** The desktop-local centre of the tile that starts {@code programPath}, or null when it has none. */
    @Nullable
    int[] tilePoint(final String programPath) {
        final int tbY = desktop.view().height() - DesktopScreen.TASKBAR_H;
        final int y = tbY - height();
        final List<Shown> shown = shown();
        final List<TileGrid.Cell> cells = TileGrid.place(sizes(shown));
        for (int i = 0; i < shown.size(); i++) {
            if (shown.get(i).tile().id().equals(programPath)) {
                final int[] r = rect(cells.get(i), y);
                return new int[] {r[0] + r[2] / 2, r[1] + r[3] / 2};
            }
        }
        return null;
    }

    /** The desktop-local centre of the list's entry for that program, or null when it is not in view. */
    @Nullable
    int[] listPoint(final Launcher target) {
        final int tbY = desktop.view().height() - DesktopScreen.TASKBAR_H;
        final int h = height();
        final List<Row> rows = listRows();
        for (int i = listScroll; i < rows.size(); i++) {
            final int ry = tbY - h + 6 + (i - listScroll) * ROW_H;
            if (ry + ROW_H > tbY - 2) {
                break;
            }
            if (rows.get(i).launcher() == target) {
                return new int[] {RAIL_W + LIST_W / 2, ry + ROW_H / 2};
            }
        }
        return null;
    }

    /** The tiles, in order, as the machine keeps them. */
    List<String> encodedTiles() {
        final List<String> out = new ArrayList<>();
        for (final StartTiles.Tile tile : tiles) {
            out.add(tile.encoded());
        }
        return out;
    }

    /** Pins, moves or resizes a tile here at once, and tells the machine, which keeps the arrangement. */
    void place(final String id, final StartTiles.Size size, final int index) {
        final int at = indexOfTile(id);
        if (at >= 0) {
            tiles.remove(at);
        } else if (tiles.size() >= StartTiles.MAX) {
            return;
        }
        final int to = Math.max(0, Math.min(tiles.size(), index));
        tiles.add(to, new StartTiles.Tile(id, size));
        PacketDistributor.sendToServer(new SetSettingPayload(desktop.hostPos(), "tile",
                id + " " + size.letter() + " " + to));
    }

    /** Takes a tile off Start here at once, and tells the machine. */
    void unpin(final String id) {
        final int at = indexOfTile(id);
        if (at >= 0) {
            tiles.remove(at);
            PacketDistributor.sendToServer(new SetSettingPayload(desktop.hostPos(), "untile", id));
        }
    }

    /* The rail: its button at the top, the account, the files, Settings and the power at the foot, worded when open. */
    private void drawRail(final GuiGraphics g, final Colours c, final int y, final int h) {
        final int w = railOpen ? RAIL_OPEN_W : RAIL_W;
        if (railOpen) {
            g.fill(0, y, w, y + h, c.groundSolid());
        }
        // The menu's button: three lines.
        for (int i = 0; i < 3; i++) {
            g.fill(5, y + 6 + i * 3, 13, y + 7 + i * 3, c.ink());
        }
        final int[] rows = railRows(y, h);
        for (int i = 0; i < rows.length; i++) {
            final int ry = rows[i];
            if (desktop.hoverIn(0, ry, w, RAIL_ROW)) {
                g.fill(0, ry, w, ry + RAIL_ROW, c.hover());
            }
            switch (i) {
                case 0 -> avatar(g, c, 4, ry + 4);
                case 1 -> ProgramIcons.draw(g, 1, ry + 1, ProgramIcons.SIZE, ProgramIcons.SIZE, Programs.FILES,
                        TILE_LOOK);
                case 2 -> ProgramIcons.draw(g, 1, ry + 1, ProgramIcons.SIZE, ProgramIcons.SIZE, Programs.SETTINGS,
                        TILE_LOOK);
                default -> power(g, 5, ry + 5, c.ink());
            }
            if (railOpen) {
                Draw.text(g, desktop.textFont(), desktop.textFont().plainSubstrByWidth(railLabel(i),
                        RAIL_OPEN_W - 22), 20, ry + 5, c.ink());
            }
        }
    }

    private void clickRail(final int my, final int y, final int h) {
        if (my < y + 18) {
            railOpen = !railOpen;
            return;
        }
        final int[] rows = railRows(y, h);
        for (int i = 0; i < rows.length; i++) {
            if (my >= rows[i] && my < rows[i] + RAIL_ROW) {
                switch (i) {
                    case 0 -> railOpen = !railOpen;
                    case 1 -> runProgram(Programs.FILES);
                    case 2 -> runProgram(Programs.SETTINGS);
                    default -> {
                        desktop.start().close();
                        desktop.power().open();
                    }
                }
                return;
            }
        }
    }

    /* Where the rail's four rows at the foot start. */
    private static int[] railRows(final int y, final int h) {
        final int bottom = y + h - 2;
        return new int[] {bottom - 4 * RAIL_ROW, bottom - 3 * RAIL_ROW, bottom - 2 * RAIL_ROW, bottom - RAIL_ROW};
    }

    private String railLabel(final int row) {
        return switch (row) {
            case 0 -> desktop.accountLabel();
            case 1 -> launcherLabel(Programs.FILES);
            case 2 -> launcherLabel(Programs.SETTINGS);
            default -> GameText.resolve(DesktopTexts.SHUT_DOWN);
        };
    }

    /* The list: the most used and then A to Z under their letters, or the search's matches. */
    private void drawList(final GuiGraphics g, final Colours c, final int y, final int h) {
        final List<Row> rows = listRows();
        final int x = RAIL_W;
        Draw.pushScissor(g, x, y, x + LIST_W, y + h);
        for (int i = listScroll; i < rows.size(); i++) {
            final int ry = y + 6 + (i - listScroll) * ROW_H;
            if (ry > y + h) {
                break;
            }
            final Row row = rows.get(i);
            if (row.launcher() == null) {
                Draw.text(g, desktop.textFont(), row.heading(), x + 4, ry + 5, row.dim() ? c.dim() : c.ink());
                continue;
            }
            if (desktop.hoverIn(x, ry, LIST_W, ROW_H)) {
                g.fill(x, ry, x + LIST_W, ry + ROW_H, c.hover());
            }
            // Each program on its small square of the accent, its white glyph drawn whole on it.
            g.fill(x + 2, ry + 1, x + 2 + ProgramIcons.SIZE, ry + 1 + ProgramIcons.SIZE,
                    desktop.prefs().skin().accent());
            ProgramIcons.draw(g, x + 2, ry + 1, ProgramIcons.SIZE, ProgramIcons.SIZE, row.launcher().programId(),
                    TILE_LOOK);
            Draw.text(g, desktop.textFont(), desktop.textFont().plainSubstrByWidth(row.launcher().label(),
                    LIST_W - 23), x + 21, ry + 5, c.ink());
        }
        Draw.popScissor(g);
    }

    @Nullable
    private Launcher listAt(final int my, final int y, final int h) {
        final List<Row> rows = listRows();
        final int i = listScroll + (my - (y + 6)) / ROW_H;
        if (my < y + 6 || i < 0 || i >= rows.size()) {
            return null;
        }
        return rows.get(i).launcher();
    }

    /** What the list shows, top to bottom: headings and programs. */
    private List<Row> listRows() {
        final List<Row> out = new ArrayList<>();
        final String typed = desktop.start().searchText();
        if (!typed.isEmpty()) {
            final List<Launcher> hits = desktop.start().filtered();
            out.add(Row.titled(GameText.resolve(hits.isEmpty() ? DesktopTexts.NO_RESULTS : DesktopTexts.BEST_MATCH),
                    true));
            for (final Launcher l : hits) {
                out.add(Row.of(l));
            }
            return out;
        }
        final List<Launcher> all = new ArrayList<>(desktop.launcherList());
        final List<Launcher> used = new ArrayList<>();
        for (final Launcher l : all) {
            if (desktop.wm().openedCount(l.key()) > 0) {
                used.add(l);
            }
        }
        used.sort(Comparator.comparingInt((Launcher l) -> -desktop.wm().openedCount(l.key())));
        if (!used.isEmpty()) {
            out.add(Row.titled(GameText.resolve(DesktopTexts.MOST_USED), true));
            for (int i = 0; i < Math.min(MOST_USED, used.size()); i++) {
                out.add(Row.of(used.get(i)));
            }
        }
        all.sort(Comparator.comparing(l -> l.label().toLowerCase(Locale.ROOT)));
        String letter = "";
        for (final Launcher l : all) {
            final String first = l.label().isEmpty() ? "#" : l.label().substring(0, 1).toUpperCase(Locale.ROOT);
            if (!first.equals(letter)) {
                letter = first;
                out.add(Row.titled(first, false));
            }
            out.add(Row.of(l));
        }
        return out;
    }

    /* The tiles: their group's name, and each tile in its place, the live ones with what they show. */
    private void drawTiles(final GuiGraphics g, final Colours c, final int y) {
        Draw.text(g, desktop.textFont(), GameText.resolve(DesktopTexts.LIFE_AT_A_GLANCE), TILES_X, y + 4, c.ink());
        final List<Shown> shown = shown();
        final List<TileGrid.Cell> cells = TileGrid.place(sizes(shown));
        final int accent = desktop.prefs().skin().accent();
        for (int i = 0; i < shown.size(); i++) {
            final Shown tile = shown.get(i);
            final int[] r = rect(cells.get(i), y);
            final boolean held = draggingTile && tile.tile().id().equals(pressedTile);
            if (held) {
                g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], c.hover());
                continue;
            }
            drawTile(g, c, tile, r, accent);
            if (desktop.hoverIn(r[0], r[1], r[2], r[3])) {
                Draw.outline(g, r[0], r[1], r[2], r[3], c.tileRim());
            }
        }
        if (draggingTile && pressedTile != null) {
            for (int i = 0; i < shown.size(); i++) {
                if (shown.get(i).tile().id().equals(pressedTile)) {
                    final int[] r = rect(cells.get(i), y);
                    drawTile(g, c, shown.get(i), new int[] {dragX - r[2] / 2, dragY - r[3] / 2, r[2], r[3]},
                            accent);
                }
            }
        }
    }

    /** One tile: the accent, the program's white glyph, its name on the larger ones, and what a live one shows. */
    private void drawTile(final GuiGraphics g, final Colours c, final Shown tile, final int[] r, final int accent) {
        g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], accent);
        final StartTiles.Size size = tile.tile().size();
        final String id = tile.tile().id();
        final boolean live = size != StartTiles.Size.SMALL && (NETWORK_MANAGER.equals(id) || SYSTEM_MONITOR.equals(id));
        if (live && liveFace()) {
            Draw.pushScissor(g, r[0], r[1], r[0] + r[2], r[1] + r[3]);
            final int lift = turnLift(r[3]);
            g.pose().pushPose();
            g.pose().translate(0, -lift, 0);
            drawFace(g, c, tile, r, false);
            g.pose().translate(0, r[3], 0);
            drawFace(g, c, tile, r, true);
            g.pose().popPose();
            Draw.popScissor(g);
            return;
        }
        drawFace(g, c, tile, r, false);
    }

    /** The side of a tile: its glyph and name, or, on the back of a live one, what that program shows. */
    private void drawFace(final GuiGraphics g, final Colours c, final Shown tile, final int[] r, final boolean back) {
        final StartTiles.Size size = tile.tile().size();
        if (!back) {
            final int glyph = size == StartTiles.Size.SMALL ? 8 : 16;
            final int gy = size == StartTiles.Size.SMALL ? r[1] + (r[3] - glyph) / 2 : r[1] + (r[3] - glyph) / 2 - 3;
            ProgramIcons.draw(g, r[0] + (r[2] - glyph) / 2, gy, glyph, glyph, tile.launcher().programId(),
                    TILE_LOOK);
            if (size != StartTiles.Size.SMALL) {
                Texts.small(g, desktop.textFont(), desktop.textFont().plainSubstrByWidth(tile.launcher().label(),
                        Texts.smallFits(r[2] - 4)), r[0] + 2, r[1] + r[3] - 7, c.ink());
            }
            return;
        }
        if (NETWORK_MANAGER.equals(tile.tile().id())) {
            Texts.small(g, desktop.textFont(), GameText.resolve(desktop.onNetwork() ? DesktopTexts.TILE_ONLINE
                    : DesktopTexts.TILE_OFFLINE), r[0] + 2, r[1] + 3, c.ink());
            Texts.small(g, desktop.textFont(), desktop.textFont().plainSubstrByWidth(desktop.accountLabel(),
                    Texts.smallFits(r[2] - 4)), r[0] + 2, r[1] + 11, c.ink());
        } else {
            sample();
            final int percent = (int) Math.round(load);
            // The load as a bar standing in the tile, and the figures beside it.
            final int barH = Math.max(1, (r[3] - 12) * percent / 100);
            g.fill(r[0] + 3, r[1] + r[3] - 9 - barH, r[0] + 7, r[1] + r[3] - 9, c.bar());
            Texts.small(g, desktop.textFont(), GameText.resolve(DesktopTexts.TILE_CPU.with(percent)), r[0] + 10,
                    r[1] + 3, c.ink());
            Texts.small(g, desktop.textFont(), GameText.resolve(DesktopTexts.TILE_MEMORY.with(
                    desktop.memory().meterText())), r[0] + 10, r[1] + 11, c.ink());
        }
        Texts.small(g, desktop.textFont(), desktop.textFont().plainSubstrByWidth(tile.launcher().label(),
                Texts.smallFits(r[2] - 4)), r[0] + 2, r[1] + r[3] - 7, c.ink());
    }

    /** Whether the live tiles turn: they do while animations are on. */
    private boolean liveFace() {
        return !desktop.prefs().effects().isOff(OsMotions.ANIMATIONS);
    }

    /** How far up a live tile has turned, from its front (0) to its back (its height), on the clock. */
    private static int turnLift(final int height) {
        final long at = System.currentTimeMillis() % (2 * TURN_EVERY_MS);
        final long within = at % TURN_EVERY_MS;
        final boolean showingBack = at >= TURN_EVERY_MS;
        final double t = within < TURN_MS ? within / (double) TURN_MS : 1.0;
        final double eased = 1 - Math.pow(1 - t, 3);
        return (int) Math.round(height * (showingBack ? eased : 1 - eased));
    }

    /*
     * The System Monitor tile's reading of the processor: the same idle drift round a small baseline the Task Manager
     * reads, rising a little for each window open on the machine, sampled once a second.
     */
    private void sample() {
        final long now = System.currentTimeMillis();
        if (now - lastSample < SAMPLE_MS) {
            return;
        }
        lastSample = now;
        final double target = 2.0 + desktop.wm().all().size() * 0.6;
        load = Math.max(0.5, Math.min(100.0, load + (target - load) * 0.2 + (noise.nextDouble() - 0.5) * 1.8));
    }

    @Nullable
    private Shown tileAt(final int mx, final int my, final int y) {
        final List<Shown> shown = shown();
        final List<TileGrid.Cell> cells = TileGrid.place(sizes(shown));
        for (int i = 0; i < shown.size(); i++) {
            final int[] r = rect(cells.get(i), y);
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                return shown.get(i);
            }
        }
        return null;
    }

    /** A tile's box on the desktop: its cells, less the gap between tiles. */
    private static int[] rect(final TileGrid.Cell cell, final int y) {
        return new int[] {TILES_X + cell.col() * CELL, y + HEADER_H + cell.row() * CELL,
                cell.across() * CELL - 1, cell.down() * CELL - 1};
    }

    private void openTileMenu(final Shown tile, final int mx, final int my) {
        final String id = tile.tile().id();
        final int at = indexOfTile(id);
        final List<ContextMenu.Item> items = new ArrayList<>();
        items.add(DeskMenu.item(DesktopTexts.UNPIN_FROM_START, true, () -> unpin(id)));
        items.add(ContextMenu.Item.separator());
        sizeItem(items, DesktopTexts.TILE_SMALL, id, StartTiles.Size.SMALL, tile.tile().size(), at);
        sizeItem(items, DesktopTexts.TILE_MEDIUM, id, StartTiles.Size.MEDIUM, tile.tile().size(), at);
        sizeItem(items, DesktopTexts.TILE_WIDE, id, StartTiles.Size.WIDE, tile.tile().size(), at);
        openMenu(items, mx, my);
    }

    private void sizeItem(final List<ContextMenu.Item> items, final TextKey label, final String id,
                          final StartTiles.Size size, final StartTiles.Size now, final int at) {
        items.add(DeskMenu.item(label, size != now, () -> place(id, size, at)));
    }

    private void openListMenu(final Launcher launcher, final int mx, final int my) {
        final String id = launcher.programId().getPath();
        final List<ContextMenu.Item> items = new ArrayList<>();
        if (indexOfTile(id) >= 0) {
            items.add(DeskMenu.item(DesktopTexts.UNPIN_FROM_START, true, () -> unpin(id)));
        } else {
            items.add(DeskMenu.item(DesktopTexts.PIN_TO_START, true,
                    () -> place(id, StartTiles.Size.MEDIUM, tiles.size())));
        }
        openMenu(items, mx, my);
    }

    private void openMenu(final List<ContextMenu.Item> items, final int mx, final int my) {
        final DesktopViewport view = desktop.view();
        desktop.taskbar().menu().open(items, mx, my, 0, 0, view.width(), view.height());
    }

    private int indexOfTile(final String id) {
        for (int i = 0; i < tiles.size(); i++) {
            if (tiles.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    @Nullable
    private Launcher launcherFor(final String programPath) {
        for (final Launcher l : desktop.launcherList()) {
            if (l.programId() != null && l.programId().getPath().equals(programPath)) {
                return l;
            }
        }
        return null;
    }

    private String launcherLabel(final ResourceLocation program) {
        final Launcher l = launcherFor(program.getPath());
        return l == null ? program.getPath() : l.label();
    }

    private void runProgram(final ResourceLocation program) {
        final Launcher l = launcherFor(program.getPath());
        if (l != null) {
            desktop.start().close();
            desktop.opener().run(l);
        }
    }

    private static List<int[]> sizes(final List<Shown> shown) {
        final List<int[]> out = new ArrayList<>(shown.size());
        for (final Shown each : shown) {
            out.add(new int[] {each.tile().size().across(), each.tile().size().down()});
        }
        return out;
    }

    /** The round picture of the account, a grass block. */
    private static void avatar(final GuiGraphics g, final Colours c, final int x, final int y) {
        final int[] half = {3, 4, 5, 5, 5, 5, 4, 3};
        for (int row = 0; row < half.length; row++) {
            final int colour = row < 3 ? c.grass() : c.dirt();
            g.fill(x + 5 - half[row], y + row + 1, x + 5 + half[row], y + row + 2, colour);
        }
    }

    /** The power button's mark: a ring open at the top with the stroke through the gap. */
    private static void power(final GuiGraphics g, final int x, final int y, final int colour) {
        g.fill(x + 1, y + 2, x + 2, y + 7, colour);
        g.fill(x + 6, y + 2, x + 7, y + 7, colour);
        g.fill(x + 2, y + 7, x + 6, y + 8, colour);
        g.fill(x + 3, y, x + 5, y + 4, colour);
    }

    /** A tile shown, with the program it starts. */
    record Shown(StartTiles.Tile tile, Launcher launcher) {
    }

    /** A row of the list: a heading, dim or not, or a program. */
    private record Row(String heading, boolean dim, @Nullable Launcher launcher) {

        static Row titled(final String text, final boolean dim) {
            return new Row(text, dim, null);
        }

        static Row of(final Launcher launcher) {
            return new Row("", false, launcher);
        }
    }

    /**
     * The menu's colours: its ground with transparency and without, its ink and the dim ink of its headings, a row
     * under the cursor (and the place of a tile being dragged), the rim of a tile under it, the System Monitor's bar
     * on the accent, and the grass block of the account.
     */
    private record Colours(int ground, int groundSolid, int ink, int dim, int hover, int tileRim, int bar, int grass,
                           int dirt) {
    }
}
