/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.msd;

import dev.jstech.computers.program.cli.CliText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;

/**
 * The diagnostics screen of the Vintage systems, drawn in text: a bar along the top, the machine's parts as a grid
 * of buttons with what each found beside it, and over them, when it is opened, the dialog of the LPT and COM ports
 * with what is attached to each, whether it is on, and the buttons that enable and disable it.
 *
 * <p>Pure. It is handed what the machine found and gives back rows of text of the width it was told, so the screen
 * is held to account without a world, and the machine draws the same screen for a monitor and a desktop window.
 *
 * <p>Its words are declared, but the machine draws the screen and sends it as the rows of a file, so they are put in
 * English, the machine's language, until the screen travels as words still to be put in the reader's.
 */
@TextHolder
public final class MsdScreen {

    /** One port of the dialog: its name, what is attached to it, and whether that is on. */
    public record PortRow(String port, String attached, String status) {
    }

    /**
     * What the machine hands over to be drawn.
     *
     * @param found what each button of the main screen found, in the buttons' order
     * @param ports the rows of the ports dialog, in the order the ports run
     */
    public record Data(List<String> found, List<PortRow> ports) {

        public Data {
            found = List.copyOf(found);
            ports = List.copyOf(ports);
        }
    }

    private static final TextKey PROGRAM = TextKey.of("jsc.cli.msd.program", "Midsoft Diagnostics 2.01");
    private static final TextKey EXIT = TextKey.of("jsc.cli.msd.exit", "F3=Exit");
    private static final TextKey COMPUTER = TextKey.of("jsc.cli.msd.computer", "Computer...");
    private static final TextKey MEMORY = TextKey.of("jsc.cli.msd.memory", "Memory...");
    private static final TextKey VIDEO = TextKey.of("jsc.cli.msd.video", "Video...");
    private static final TextKey NETWORK = TextKey.of("jsc.cli.msd.network", "Network...");
    private static final TextKey OS_VERSION = TextKey.of("jsc.cli.msd.os_version", "OS Version...");
    private static final TextKey DISK_DRIVES = TextKey.of("jsc.cli.msd.disk_drives", "Disk Drives...");
    private static final TextKey LPT_PORTS = TextKey.of("jsc.cli.msd.lpt_ports", "LPT Ports...");
    private static final TextKey COM_PORTS = TextKey.of("jsc.cli.msd.com_ports", "COM Ports...");
    private static final TextKey DIALOG = TextKey.of("jsc.cli.msd.dialog", "LPT and COM Ports");
    private static final TextKey PORT = TextKey.of("jsc.cli.msd.port", "Port");
    private static final TextKey ATTACHED = TextKey.of("jsc.cli.msd.attached", "Attached");
    private static final TextKey STATUS = TextKey.of("jsc.cli.msd.status", "Status");
    private static final TextKey OK = TextKey.of("jsc.cli.msd.ok", "OK");
    private static final TextKey ENABLE = TextKey.of("jsc.cli.msd.enable", "Enable");
    private static final TextKey DISABLE = TextKey.of("jsc.cli.msd.disable", "Disable");
    private static final TextKey NONE = TextKey.of("jsc.cli.msd.none", "(none)");

    /** The buttons of the main screen, left to right and down, as that program laid them out in two columns. */
    public static final String[] BUTTONS = {english(COMPUTER), english(MEMORY), english(VIDEO), english(NETWORK),
            english(OS_VERSION), english(DISK_DRIVES), english(LPT_PORTS), english(COM_PORTS)};

    /** The two buttons that open the ports dialog. */
    public static final int LPT_BUTTON = 6;
    public static final int COM_BUTTON = 7;

    /** The buttons along the foot of the ports dialog, in their order. */
    public static final String[] DIALOG_BUTTONS = {"< " + english(OK) + " >", "<" + english(ENABLE) + ">",
            "<" + english(DISABLE) + ">"};

    /** What an empty port says is attached to it. */
    public static final String NOTHING_ATTACHED = english(NONE);

    /** Which row the grid of buttons starts on, and how many rows apart its rows are. */
    public static final int GRID_TOP = 2;
    public static final int GRID_STEP = 2;

    /** Which row the ports dialog's frame starts on, under the empty row after the last buttons, and its first port. */
    public static final int DIALOG_TOP = GRID_TOP + GRID_STEP * (BUTTONS.length / 2);
    public static final int PORTS_TOP = DIALOG_TOP + 2;

    /** How wide the label column of a button is; what it found starts after it. */
    private static final int LABEL_W = 15;

    /** How wide the port and status columns of the dialog are; what is attached takes the rest. */
    private static final int PORT_W = 14;
    private static final int STATUS_W = 15;

    /** How far in from each side of the glass the dialog stands. */
    private static final int DIALOG_MARGIN = 6;

    /** Where the bar's exit key is written, as that program had it, a little short of the middle. */
    private static final int EXIT_AT = 37;

    /** The gap between two of the dialog's buttons. */
    private static final int BUTTON_GAP = 4;

    /** How narrow a glass may be before the screen stops getting narrower with it. */
    private static final int LEAST_WIDE = 60;

    private MsdScreen() {
    }

    /** The whole screen, row by row, as wide and as tall as the state says the glass is. */
    public static List<String> render(final MsdState state, final Data data) {
        final int wide = Math.max(LEAST_WIDE, state.columns());
        final List<String> out = new ArrayList<>();
        out.add(CliText.pad(CliText.pad(" " + english(PROGRAM), EXIT_AT) + english(EXIT), wide));
        out.add(CliText.pad("", wide));
        final int half = wide / 2;
        for (int row = 0; row < BUTTONS.length / 2; row++) {
            final int left = row * 2;
            out.add(CliText.pad(button(left, data, state, half) + button(left + 1, data, state, half), wide));
            out.add(CliText.pad("", wide));
        }
        if (state.ports()) {
            out.addAll(dialog(state, data, wide));
        }
        while (out.size() < state.rows()) {
            out.add(CliText.pad("", wide));
        }
        return out.size() > state.rows() ? new ArrayList<>(out.subList(0, state.rows())) : out;
    }

    /** How many port rows the dialog shows on a glass that state describes: as many as fit under the grid. */
    public static int portsShown(final MsdState state, final int ports) {
        final int room = state.rows() - PORTS_TOP - 4;
        return Math.max(1, Math.min(ports, room));
    }

    /**
     * How many port rows a drawn screen has, read back off it.
     *
     * <p>A terminal showing the screen knows only what is on the glass, so where the ports end is read from the glass
     * rather than remembered: the first row under them is the dialog's empty one.
     */
    public static int portsSaid(final List<String> screen) {
        int count = 0;
        for (int row = PORTS_TOP; row < screen.size(); row++) {
            final String inside = inside(screen.get(row));
            if (inside == null || inside.isBlank()) {
                break;
            }
            count++;
        }
        return count;
    }

    /** Which port row of a drawn dialog is picked, read back off it: the one marked, the first when none is. */
    public static int pickedSaid(final List<String> screen) {
        final int count = portsSaid(screen);
        for (int i = 0; i < count; i++) {
            final String inside = inside(screen.get(PORTS_TOP + i));
            if (inside != null && inside.startsWith(">")) {
                return i;
            }
        }
        return 0;
    }

    /** The button of the main screen at that row and column, or {@code -1} for a place between them. */
    public static int buttonAt(final int row, final int column, final int columns) {
        final int fromTop = row - GRID_TOP;
        if (fromTop < 0 || fromTop % GRID_STEP != 0 || fromTop / GRID_STEP >= BUTTONS.length / 2) {
            return -1;
        }
        return fromTop / GRID_STEP * 2 + (column >= Math.max(LEAST_WIDE, columns) / 2 ? 1 : 0);
    }

    /** The row the dialog's buttons are on, for that many ports drawn. */
    public static int dialogButtonsRow(final int shown) {
        return PORTS_TOP + shown + 1;
    }

    /** The dialog button at that column of its row, or {@code -1} for a place between them. */
    public static int dialogButtonAt(final int column, final int columns) {
        final int wide = Math.max(LEAST_WIDE, columns);
        int at = buttonsStart(wide);
        for (int i = 0; i < DIALOG_BUTTONS.length; i++) {
            if (column >= at && column < at + DIALOG_BUTTONS[i].length()) {
                return i;
            }
            at += DIALOG_BUTTONS[i].length() + BUTTON_GAP;
        }
        return -1;
    }

    /** One button of the main screen with what it found, the picked one marked the way a text screen marks it. */
    private static String button(final int index, final Data data, final MsdState state, final int half) {
        final String found = index < data.found().size() ? data.found().get(index) : "";
        final boolean picked = !state.ports() && state.picked() == index;
        final String cell = (picked ? ">" : " ") + CliText.pad(BUTTONS[index], LABEL_W) + found;
        return CliText.pad(cell.length() > half - 1 ? cell.substring(0, half - 1) : cell, half);
    }

    /** The ports dialog: its frame, the column headings, a row per port, and its buttons. */
    private static List<String> dialog(final MsdState state, final Data data, final int wide) {
        final int boxW = wide - 2 * DIALOG_MARGIN;
        final int attachedW = boxW - 4 - PORT_W - STATUS_W;
        final String margin = " ".repeat(DIALOG_MARGIN);
        final List<String> out = new ArrayList<>();
        out.add(margin + "+" + centred(" " + english(DIALOG) + " ", boxW - 2, '-') + "+");
        out.add(margin + "| " + CliText.pad(english(PORT), PORT_W) + CliText.pad(english(ATTACHED), attachedW)
                + CliText.pad(english(STATUS), STATUS_W) + " |");
        final int shown = portsShown(state, data.ports().size());
        for (int i = 0; i < shown; i++) {
            final PortRow port = i < data.ports().size() ? data.ports().get(i)
                    : new PortRow("", NOTHING_ATTACHED, "");
            final String mark = i == state.picked() ? ">" : " ";
            out.add(margin + "|" + mark + clip(port.port(), PORT_W) + clip(port.attached(), attachedW)
                    + clip(port.status(), STATUS_W) + " |");
        }
        out.add(margin + "|" + " ".repeat(boxW - 2) + "|");
        final String buttons = String.join(" ".repeat(BUTTON_GAP), DIALOG_BUTTONS);
        out.add(margin + "|" + centred(buttons, boxW - 2, ' ') + "|");
        out.add(margin + "+" + "-".repeat(boxW - 2) + "+");
        return out;
    }

    /** The column the dialog's first button starts at on a glass that wide. */
    private static int buttonsStart(final int wide) {
        final int boxW = wide - 2 * DIALOG_MARGIN;
        final int length = String.join(" ".repeat(BUTTON_GAP), DIALOG_BUTTONS).length();
        return DIALOG_MARGIN + 1 + (boxW - 2 - length) / 2;
    }

    /** What stands between a dialog row's two side lines, or null for a row that is not one of the dialog's. */
    private static String inside(final String row) {
        final int left = row.indexOf('|');
        final int right = row.lastIndexOf('|');
        return left < 0 || right <= left ? null : row.substring(left + 1, right);
    }

    /* Words padded to a column, cut where they would run into the next one. */
    private static String clip(final String words, final int width) {
        return CliText.pad(words.length() >= width ? words.substring(0, width - 1) : words, width);
    }

    /* Words in the middle of a run of that character, as a frame's title or a row of buttons stands. */
    private static String centred(final String words, final int width, final char fill) {
        final int before = Math.max(0, (width - words.length()) / 2);
        final int after = Math.max(0, width - words.length() - before);
        return String.valueOf(fill).repeat(before) + words + String.valueOf(fill).repeat(after);
    }

    private static String english(final TextKey key) {
        return key.text().english();
    }
}
