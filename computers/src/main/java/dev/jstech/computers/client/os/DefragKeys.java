/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.os.defrag.DefragLook;
import dev.jstech.computers.os.defrag.DefragMap;
import dev.jstech.computers.os.defrag.DefragState;
import dev.jstech.core.gui.TextScreen;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Locale;
import net.minecraft.Util;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * MC-DOS's DEFRAG on a terminal it has taken whole (computers/docs/design/storage.md), drawn in the adapter's sixteen
 * colours: the grey bar on top, the map of the disk's clusters on blue (used cyan, unused blue, read yellow, corrupted
 * red, bad black, unmovable grey), the status box with its bar and what the pass gave back, and the legend.
 *
 * <p>Nothing of the machine is held here but what it last said; the screen asks again a few times a second while a
 * pass runs. Enter starts a pass, P pauses it, Esc gives the terminal back.
 */
public final class DefragKeys implements TtyEditor.IKeys {

    /* How often the screen asks again while a pass runs, in milliseconds, and otherwise. */
    private static final long ASK_RUNNING_MS = 400L;
    private static final long ASK_IDLE_MS = 2_000L;
    /* The map's cell, two characters wide, and the boxes' height under it. */
    private static final String BLOCK = "██";
    private static final int BOX_ROWS = 6;

    private DefragState state = new DefragState(DefragLook.DOS, List.of(), null, null, false);
    private int disk;
    private long askedAt;

    @Override
    public String status(final TtyEditor editor) {
        return "";
    }

    @Override
    public void opened(final TtyEditor editor, final boolean existed) {
        if (DefragState.names(editor.path())) {
            this.disk = DefragState.diskOf(editor.path());
        }
        this.state = DefragState.read(editor.document().text());
        this.askedAt = Util.getMillis();
    }

    @Override
    public boolean typed(final TtyEditor editor, final char c) {
        if (Character.toLowerCase(c) == 'p' && this.state.running() != null) {
            ask(editor, DefragState.PAUSE);
        }
        return true;
    }

    @Override
    public boolean key(final TtyEditor editor, final int key, final int modifiers) {
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_F3 -> editor.quit();
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (this.state.running() == null) {
                    ask(editor, DefragState.START);
                }
            }
            default -> {
                // Any other key is not one of this program's.
            }
        }
        return true;
    }

    @Override
    public TextScreen screen(final TtyEditor editor, final int columns, final int rows) {
        final long now = Util.getMillis();
        if (now - this.askedAt >= (this.state.running() != null ? ASK_RUNNING_MS : ASK_IDLE_MS)) {
            ask(editor, DefragState.LOOK);
        }
        final TextScreen screen = new TextScreen(columns, rows, TextScreen.WHITE, TextScreen.BLUE);
        screen.fill(0, 0, columns, 1, TextScreen.BLACK, TextScreen.GREY);
        screen.put(1, 0, resolve(DefragTexts.DOS_BAR), TextScreen.BLACK, TextScreen.GREY);
        final String title = resolve(DefragTexts.DOS_TITLE);
        screen.put(Math.max(0, columns - title.length() - 1), 0, title, TextScreen.BLACK, TextScreen.GREY);
        final DefragState.Disk shown = this.state.disk(this.disk);
        final int mapCols = Math.max(1, (columns - 4) / BLOCK.length());
        final int mapRows = Math.max(1, rows - BOX_ROWS - 4);
        if (shown != null) {
            final DefragMap.Cell[] cells = DefragMap.cells(shown, this.state.running(), mapCols * mapRows);
            for (int i = 0; i < cells.length; i++) {
                final int ink = colour(cells[i]);
                screen.put(2 + i % mapCols * BLOCK.length(), 2 + i / mapCols, BLOCK, ink, TextScreen.BLUE);
            }
        }
        statusBox(screen, columns, rows, shown);
        legendBox(screen, columns, rows);
        return screen;
    }

    /* The status box on the left: the cluster, the percentage and its bar, and what the pass found and gave back. */
    private void statusBox(final TextScreen screen, final int columns, final int rows,
                           @Nullable final DefragState.Disk shown) {
        final int top = rows - BOX_ROWS - 1;
        final int width = Math.max(20, columns / 2 - 3);
        screen.fill(2, top, width, BOX_ROWS, TextScreen.BLACK, TextScreen.GREY);
        screen.put(3, top, resolve(DefragTexts.DOS_STATUS), TextScreen.BLACK, TextScreen.GREY);
        final DefragState.Run run = this.state.running();
        final boolean here = run != null && run.disk() == this.disk;
        if (shown != null && shown.dead()) {
            screen.put(3, top + 1, resolve(DefragTexts.DOS_FAILED.with(shown.drive())), TextScreen.RED,
                    TextScreen.GREY);
            return;
        }
        final int percent = here ? run.percent() : 0;
        final long cluster = shown == null ? 0L : shown.capacityMb() * percent / 100L;
        screen.put(3, top + 1, resolve(DefragTexts.DOS_CLUSTER.with(String.format(Locale.ROOT, "%,d", cluster)))
                + "   " + percent + "%", TextScreen.BLACK, TextScreen.GREY);
        final int barWidth = width - 2;
        screen.fill(3, top + 2, barWidth, 1, TextScreen.BLACK, TextScreen.BLACK);
        screen.fill(3, top + 2, barWidth * percent / 100, 1, TextScreen.CYAN, TextScreen.CYAN);
        final DefragState.Result last = this.state.last();
        if (here) {
            screen.put(3, top + 3, resolve(DefragTexts.DOS_CORRUPTED.with(run.found())), TextScreen.BLACK,
                    TextScreen.GREY);
            screen.put(3, top + 4, resolve(DefragTexts.DOS_RECOVERED.with(run.recovered(), run.lost())),
                    TextScreen.BLACK, TextScreen.GREY);
        } else if (last != null && last.disk() == this.disk) {
            screen.put(3, top + 3, resolve(DefragTexts.DOS_FINISHED.with(this.state.look().drive(this.disk))),
                    TextScreen.BLACK, TextScreen.GREY);
            screen.put(3, top + 4, resolve(DefragTexts.DOS_RECOVERED.with(last.recovered(), last.lost())),
                    TextScreen.BLACK, TextScreen.GREY);
        } else {
            screen.put(3, top + 3, resolve(DefragTexts.DOS_CORRUPTED.with(shown == null ? 0 : shown.corrupted())),
                    TextScreen.BLACK, TextScreen.GREY);
            screen.put(3, top + 4, resolve(DefragTexts.DOS_READY), TextScreen.BLACK, TextScreen.GREY);
        }
    }

    /* The legend on the right, in two columns of three. */
    private static void legendBox(final TextScreen screen, final int columns, final int rows) {
        final int top = rows - BOX_ROWS - 1;
        final int left = columns / 2;
        final int width = Math.max(20, columns - left - 2);
        screen.fill(left, top, width, BOX_ROWS, TextScreen.BLACK, TextScreen.GREY);
        screen.put(left + 1, top, resolve(DefragTexts.LEGEND), TextScreen.BLACK, TextScreen.GREY);
        final DefragMap.Cell[] kinds = {DefragMap.Cell.USED, DefragMap.Cell.FREE, DefragMap.Cell.READING,
                DefragMap.Cell.CORRUPTED, DefragMap.Cell.BAD, DefragMap.Cell.UNMOVABLE};
        final TextKey[] names = {DefragTexts.DOS_USED, DefragTexts.DOS_UNUSED, DefragTexts.DOS_READING,
                DefragTexts.DOS_CORRUPT, DefragTexts.DOS_BAD, DefragTexts.DOS_UNMOVABLE};
        for (int i = 0; i < kinds.length; i++) {
            final int x = left + 1 + i / 3 * (width / 2);
            final int y = top + 1 + i % 3;
            screen.put(x, y, "█", colour(kinds[i]), TextScreen.GREY);
            screen.put(x + 2, y, resolve(names[i]), TextScreen.BLACK, TextScreen.GREY);
        }
    }

    private void ask(final TtyEditor editor, final String action) {
        this.askedAt = Util.getMillis();
        final String path = DefragState.path(action, this.disk);
        editor.read(path.substring(DefragState.SCHEME.length()), (content, existed) -> {
            if (existed) {
                this.state = DefragState.read(content);
            }
        });
    }

    private static int colour(final DefragMap.Cell cell) {
        return switch (cell) {
            case USED, DONE -> TextScreen.LIGHT_CYAN;
            case FREE -> TextScreen.BLUE;
            case READING -> TextScreen.YELLOW;
            case CORRUPTED -> TextScreen.LIGHT_RED;
            case BAD -> TextScreen.BLACK;
            case UNMOVABLE -> TextScreen.GREY;
        };
    }

    private static String resolve(final TextKey key) {
        return GameText.resolve(key);
    }

    private static String resolve(final Text text) {
        return GameText.resolve(text);
    }
}
