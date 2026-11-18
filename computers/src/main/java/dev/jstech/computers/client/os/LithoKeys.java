/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.chain.machine.MaskWriting;
import dev.jstech.computers.lithography.LithographyLook;
import dev.jstech.computers.operation.payload.lithography.LithographyRequestPayload;
import dev.jstech.computers.operation.payload.lithography.LithographyStatePayload;
import dev.jstech.core.gui.TextScreen;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Midsoft Lithography on MC-DOS (computers/docs/design/survival.md, "Masks"): the text screen of the lithography
 * program, in the look of Midsoft's diagnostics of that age. The station linked to the computer down the left, with
 * what it is doing, its wafer, the layer it exposes, what the computer holds for it, its liquids and gases and what
 * stands in its mask slot; the masks it writes on the right, a kind of chip a tab; what the program says over the keys.
 *
 * <p>The screen is drawn from what the computer answers, asked again every second, as the window is: the arrows pick a
 * mask, Tab shows the next kind, W writes the one picked, S shows the next station when several are linked, and F3
 * leaves.
 */
@PaletteHolder
public final class LithoKeys implements TtyEditor.IKeys {

    private @Nullable BlockPos machine;
    private @Nullable LithographyStatePayload state;
    private int station;
    private int kind;
    private int picked;
    private long askedAt;

    /* The green phosphor of the screen: its bright ink, its dim ink, the cursor's bar, the keys' bar and the glass. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/lithography_text",
            new Colours(0xFF2BD756, 0xFF1A8134, 0xFF115522, 0xFF030E06, 0xFF000000));
    private static final String RULE = "─";
    private static final String CELL = "█";
    private static final long ASK_MS = 1_000L;
    /* Where things stand on the glass, in cells. */
    private static final int LEFT = 2;
    private static final int EXIT_AT = 40;
    private static final int HEAD_ROW = 3;
    private static final int FIRST_ROW = 5;
    private static final int LABEL_W = 15;
    private static final int LIST_INDENT = 2;
    private static final int FOOT_ROWS = 5;
    private static final int LAYER_BAR = 12;
    private static final int LEAST_COLUMNS = 60;
    private static final double PERMILLE = 1000.0;
    private static final int PERCENT = 100;

    private static final Set<LithoKeys> OPEN = Collections.newSetFromMap(new WeakHashMap<>());

    /** Routes the computer's answer to the screens open on that computer. */
    public static void accept(final LithographyStatePayload payload) {
        for (final LithoKeys keys : List.copyOf(OPEN)) {
            if (payload.hostPos().equals(keys.machine)) {
                keys.state = payload;
                keys.station = payload.look().shown();
            }
        }
    }

    @Override
    public String status(final TtyEditor editor) {
        return "";
    }

    @Override
    public void opened(final TtyEditor editor, final boolean existed) {
        this.machine = editor.machine();
        OPEN.add(this);
        ask(LithographyRequestPayload.LOOK, null);
    }

    @Override
    public void closed(final TtyEditor editor) {
        OPEN.remove(this);
    }

    @Override
    public boolean typed(final TtyEditor editor, final char c) {
        switch (Character.toLowerCase(c)) {
            case 'w' -> {
                final LithographyLook.Pick pick = pick();
                if (pick != null) {
                    ask(LithographyRequestPayload.WRITE, pick.mask());
                }
            }
            case 's' -> {
                final int stations = look().stations();
                if (stations > 1) {
                    station = (station + 1) % stations;
                    picked = 0;
                    ask(LithographyRequestPayload.LOOK, null);
                }
            }
            default -> {
                // Any other letter is not one of this program's.
            }
        }
        return true;
    }

    @Override
    public boolean key(final TtyEditor editor, final int key, final int modifiers) {
        final int kinds = kinds().size();
        switch (key) {
            case GLFW.GLFW_KEY_F3, GLFW.GLFW_KEY_ESCAPE -> editor.quit();
            case GLFW.GLFW_KEY_UP -> picked = Math.max(0, picked - 1);
            case GLFW.GLFW_KEY_DOWN -> picked = Math.min(Math.max(0, shown().size() - 1), picked + 1);
            case GLFW.GLFW_KEY_TAB, GLFW.GLFW_KEY_RIGHT -> {
                kind = kinds == 0 ? 0 : (kind + 1) % kinds;
                picked = 0;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                kind = kinds == 0 ? 0 : (kind + kinds - 1) % kinds;
                picked = 0;
            }
            default -> {
                // Any other key is not one of this program's.
            }
        }
        return true;
    }

    @Override
    public TextScreen screen(final TtyEditor editor, final int columns, final int rows) {
        if (Util.getMillis() - askedAt >= ASK_MS) {
            ask(LithographyRequestPayload.LOOK, null);
        }
        final Colours ink = PALETTE.get();
        final int wide = Math.max(LEAST_COLUMNS, columns);
        final TextScreen screen = new TextScreen(wide, rows, ink.bright(), ink.glass());
        final LithographyLook look = look();
        screen.put(LEFT, 0, resolve(LithoTuiTexts.PROGRAM), ink.bright());
        screen.put(EXIT_AT, 0, resolve(LithoTuiTexts.EXIT), ink.bright());
        if (look.stations() == 0) {
            if (state != null) {
                screen.put(LEFT, HEAD_ROW, resolve(LithographyTexts.NO_STATION), ink.bright());
                screen.put(LEFT, HEAD_ROW + 1, resolve(LithographyTexts.NO_STATION_HINT), ink.dim());
            }
            foot(screen, ink, look, rows);
            return screen;
        }
        final Text named = LithoTuiTexts.STATION_LINE.with(look.station());
        screen.put(LEFT, 1, resolve(look.stations() > 1 ? LithoTuiTexts.OF_STATIONS.with(named, look.shown() + 1,
                look.stations()) : named), ink.dim());
        final int half = wide / 2;
        stationSide(screen, ink, look, half);
        maskSide(screen, ink, half, wide, rows);
        say(screen, ink, look, wide, rows);
        foot(screen, ink, look, rows);
        return screen;
    }

    // what the client tests read

    /** What the screen shows, or a look of no station until the computer has answered. */
    public LithographyLook look() {
        return state == null ? LithographyLook.NONE : state.look();
    }

    /* The station down the left: its rows, a label led by dots to its value. */
    private void stationSide(final TextScreen screen, final Colours ink, final LithographyLook look, final int half) {
        screen.put(LEFT, HEAD_ROW, resolve(LithoTuiTexts.HEAD_STATION), ink.bright());
        screen.put(LEFT, HEAD_ROW + 1, RULE.repeat(Math.max(0, half - LEFT - 2)), ink.dim());
        final int valueAt = LEFT + LABEL_W + 1;
        final int room = half - valueAt - 2;
        int row = FIRST_ROW;
        row = dots(screen, ink, row, resolve(LithographyTexts.ROW_STATUS), resolve(look.status()), valueAt, room);
        row = dots(screen, ink, row, resolve(LithographyTexts.ROW_WAFER), resolve(look.wafer()), valueAt, room);
        if (look.exposing()) {
            dots(screen, ink, row, resolve(LithographyTexts.ROW_LAYER), "", valueAt, room);
            bar(screen, ink, valueAt, row, LAYER_BAR, look.layerPermille() / PERMILLE);
            screen.put(valueAt + LAYER_BAR + 1, row, resolve(LithographyTexts.SECONDS.with(look.layerSeconds())),
                    ink.bright());
            row++;
        } else {
            row = dots(screen, ink, row, resolve(LithographyTexts.ROW_LAYER), "-", valueAt, room);
        }
        row = dots(screen, ink, row, resolve(LithographyTexts.ROW_LITHO), resolve(look.lithoMb() > 0
                ? LithographyTexts.HELD.with(look.lithoMb()) : LithographyTexts.NOT_RUNNING.text()), valueAt, room);
        for (final LithographyLook.Supply supply : look.supplies()) {
            row = dots(screen, ink, row, resolve(supply.name()), resolve(LithographyTexts.MILLIBUCKETS.with(
                    String.format(Locale.ROOT, "%,d", supply.amount()))), valueAt, room);
        }
        dots(screen, ink, row, resolve(LithographyTexts.ROW_SLOT), resolve(look.slot()), valueAt, room);
    }

    /* The masks on the right: the kinds as tabs, as many rows as they take, and the list of the one shown. */
    private void maskSide(final TextScreen screen, final Colours ink, final int half, final int wide, final int rows) {
        final int left = half + LEFT;
        screen.put(left, HEAD_ROW, resolve(LithoTuiTexts.HEAD_MASKS), ink.bright());
        screen.put(left, HEAD_ROW + 1, RULE.repeat(Math.max(0, wide - left - 1)), ink.dim());
        final List<String> kinds = kinds();
        kind = kinds.isEmpty() ? 0 : Math.min(kind, kinds.size() - 1);
        int row = FIRST_ROW;
        int column = left;
        for (int i = 0; i < kinds.size(); i++) {
            final String label = resolve(MaskWriting.title(MaskWriting.Kind.of(kinds.get(i))));
            if (column + label.length() > wide - 1 && column > left) {
                row++;
                column = left;
            }
            if (i == kind) {
                screen.fill(column - 1, row, label.length() + 2, 1, ink.bright(), ink.cursor());
            }
            screen.put(column, row, label, i == kind ? ink.bright() : ink.dim());
            column += label.length() + 2;
        }
        final List<LithographyLook.Pick> shown = shown();
        picked = shown.isEmpty() ? 0 : Math.min(picked, shown.size() - 1);
        final int top = row + 2;
        final int room = Math.max(1, rows - FOOT_ROWS - 1 - top);
        final int from = Math.max(0, Math.min(picked - room + 1, shown.size() - room));
        for (int i = from; i < Math.min(shown.size(), from + room); i++) {
            final int y = top + i - from;
            if (i == picked) {
                screen.fill(left - 1, y, wide - left, 1, ink.bright(), ink.cursor());
                screen.put(left, y, ">", ink.bright());
            }
            screen.put(left + LIST_INDENT, y, clip(resolve(shown.get(i).name()), wide - left - LIST_INDENT - 1),
                    ink.bright());
        }
    }

    /* What the program says over the keys: a refusal, the writing and how far it is, or what W would do. */
    private void say(final TextScreen screen, final Colours ink, final LithographyLook look, final int wide,
                     final int rows) {
        final int line = rows - FOOT_ROWS;
        screen.put(1, line, RULE.repeat(Math.max(0, wide - 2)), ink.dim());
        final String said = state == null ? "" : resolve(state.said());
        if (!said.isEmpty()) {
            final String shown = clip(said, wide - LEFT * 2);
            screen.fill(LEFT, line + 1, shown.length() + 2, 1, ink.glass(), ink.bright());
            screen.put(LEFT + 1, line + 1, shown, ink.glass(), ink.bright());
            return;
        }
        if (look.writes()) {
            screen.put(LEFT, line + 1, clip(resolve(LithographyTexts.WRITING_THE.with(look.writing())), wide - 4),
                    ink.bright());
            final int cells = Math.max(1, wide / 2);
            bar(screen, ink, LEFT, line + 2, cells, look.writingPermille() / PERMILLE);
            screen.put(LEFT + cells + 1, line + 2, resolve(LithoTuiTexts.PERCENT.with(
                    Math.round(look.writingPermille() / PERMILLE * PERCENT))), ink.bright());
            return;
        }
        final LithographyLook.Pick pick = pick();
        final String hint;
        if (look.blank()) {
            hint = pick == null ? "" : resolve(LithoTuiTexts.WRITE_HINT.with(pick.name()));
        } else if (look.masked()) {
            hint = resolve(LithographyTexts.MASK_HINT.with(look.slot()));
        } else {
            hint = resolve(LithographyTexts.EMPTY_HINT);
        }
        screen.put(LEFT, line + 1, clip(hint, wide - LEFT * 2), ink.bright());
    }

    /* The keys along the foot of the glass, on their bar, and the version at its end. */
    private static void foot(final TextScreen screen, final Colours ink, final LithographyLook look, final int rows) {
        final int row = rows - 1;
        screen.fill(0, row, screen.columns(), 1, ink.bright(), ink.bar());
        screen.put(1, row, resolve(look.stations() > 1 ? LithoTuiTexts.KEYS_STATIONS : LithoTuiTexts.KEYS),
                ink.bright(), ink.bar());
        final String version = resolve(LithoTuiTexts.VERSION);
        screen.put(screen.columns() - version.length() - 2, row, version, ink.dim(), ink.bar());
    }

    private void ask(final int action, @Nullable final ResourceLocation mask) {
        askedAt = Util.getMillis();
        if (machine != null) {
            PacketDistributor.sendToServer(new LithographyRequestPayload(machine, action, station,
                    mask == null ? ResourceLocation.withDefaultNamespace("air") : mask));
        }
    }

    private @Nullable LithographyLook.Pick pick() {
        final List<LithographyLook.Pick> shown = shown();
        return shown.isEmpty() ? null : shown.get(Math.min(picked, shown.size() - 1));
    }

    /* The masks of the kind shown. */
    private List<LithographyLook.Pick> shown() {
        final List<String> kinds = kinds();
        if (kinds.isEmpty()) {
            return List.of();
        }
        final String shownKind = kinds.get(Math.min(kind, kinds.size() - 1));
        return look().picks().stream().filter(pick -> pick.kind().equals(shownKind)).toList();
    }

    /* The kinds the station writes masks of, in the order of the program's lists, as the picks come. */
    private List<String> kinds() {
        return look().picks().stream().map(LithographyLook.Pick::kind).distinct().toList();
    }

    /* A row of the station: its label led by dots to its value; the next row. */
    private static int dots(final TextScreen screen, final Colours ink, final int row, final String label,
                            final String value, final int valueAt, final int room) {
        final String shown = clip(label, LABEL_W - 2);
        screen.put(LEFT, row, shown + " " + ".".repeat(Math.max(0, LABEL_W - shown.length() - 1)), ink.bright());
        screen.put(valueAt, row, clip(value, room), ink.bright());
        return row + 1;
    }

    /* A text-mode bar of {@code cells} cells, the done share lit. */
    private static void bar(final TextScreen screen, final Colours ink, final int column, final int row,
                            final int cells, final double share) {
        final int lit = (int) Math.round(Math.max(0.0, Math.min(1.0, share)) * cells);
        screen.put(column, row, CELL.repeat(lit), ink.bright());
        screen.put(column + lit, row, CELL.repeat(cells - lit), ink.cursor());
    }

    private static String clip(final String text, final int room) {
        return room <= 0 ? "" : text.length() <= room ? text : text.substring(0, room);
    }

    private static String resolve(final Text text) {
        return GameText.resolve(text);
    }

    private static String resolve(final TextKey key) {
        return GameText.resolve(key);
    }

    /*
     * The screen's colours.
     *
     * @param bright the phosphor's bright ink
     * @param dim    its dim ink, for what stands back
     * @param cursor the bar under what is picked
     * @param bar    the bar of the keys along the foot
     * @param glass  the glass behind it all
     */
    private record Colours(int bright, int dim, int cursor, int bar, int glass) {
    }
}
