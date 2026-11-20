/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.computers.chain.machine.ChainFaceWord;
import dev.jstech.computers.printer.BookJob;
import dev.jstech.computers.printer.LargeScalePrinterBlockEntity;
import dev.jstech.core.client.machine.IMachineFace;
import dev.jstech.core.client.machine.MachineArt;
import dev.jstech.core.client.machine.MachineChrome;
import dev.jstech.core.client.machine.MachineFaces;
import dev.jstech.core.client.machine.MachineView;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Large Scale Printer's Queue tab, in steel, as its approved design draws it (computers/docs/design/survival.md):
 * the queue on its screen, each book with how many, from which computer and whether it prints or waits, the copy it
 * is on and its bar; the PAUSE and CANCEL buttons; the BOOKS TO lever, the network or the chest; the counter of books
 * printed; and the lamp of whether the server lets enchanted books print.
 */
public final class PrinterQueueTab implements IMachineFace {

    /* How wide its name plate may grow: to the PAUSE button's collar. */
    private static final int PLATE_ROOM = 112;
    /* The queue's screen, its frame and its glass. */
    private static final int SCREEN_X0 = 6;
    private static final int SCREEN_Y0 = 32;
    private static final int SCREEN_X1 = 114;
    private static final int SCREEN_Y1 = 98;
    private static final int GLASS_INSET = 2;
    /* The queue's rows: where the first stands, how far apart, how many the screen holds; their words' left. */
    private static final int ROW_X = 10;
    private static final int ROW_Y = 36;
    private static final int ROW_PITCH = 14;
    private static final int ROWS = 3;
    private static final int WORDS_X = 29;
    private static final int SECOND_LINE = 7;
    private static final int WORDS_RIGHT = 112;
    /* The rule under the rows, the copy now, its bar and what a book takes. */
    private static final int RULE_Y = 78;
    private static final int NOW_Y = 80;
    private static final int BAR_Y = 87;
    private static final int BAR_HEIGHT = 3;
    private static final int BAR_WIDTH = 100;
    private static final int SHEETS_Y = 91;
    /* The two push buttons, their middles; and how far round them a click reaches. */
    private static final int PAUSE_X = 128;
    private static final int CANCEL_X = 156;
    private static final int BUTTONS_Y = 42;
    private static final int BUTTON_REACH = 7;
    private static final int CAP = 5;
    private static final int LABEL_BELOW = 9;
    /* The counter of books printed, its word above it. */
    private static final int PRINTED_X = 142;
    private static final int PRINTED_Y = 66;
    private static final int COUNTER_X = 131;
    private static final int COUNTER_Y = 74;
    private static final int COUNTER_DIGITS = 5;
    /* The lamp of enchanted books, its word above it. */
    private static final int ENCHANTED_X = 202;
    private static final int ENCHANTED_Y = 82;
    private static final int LAMP_X = 198;
    private static final int LAMP_Y = 90;
    private static final int LAMP_SIZE = 7;
    /* The lever of where the books go, its word above it and the two places beside it. */
    private static final int LEVER_X = 184;
    private static final int LEVER_Y = 46;
    private static final int LEVER_WORD_X = 202;
    private static final int LEVER_WORD_Y = 37;
    private static final int PLACE_X = 200;
    private static final int NET_Y = 49;
    private static final int CHEST_Y = 70;
    /* The screen's colours: its frame, its dark glass, its bright and its dim greens, the rule and the bar. */
    private static final int FRAME = MachineArt.rgb(60, 64, 70);
    private static final int GLASS = MachineArt.rgb(10, 16, 12);
    private static final int BRIGHT = MachineArt.rgb(150, 255, 160);
    private static final int DIM = MachineArt.rgb(90, 160, 100);
    private static final int WAITING = MachineArt.rgb(110, 200, 120);
    private static final int PRINTING = MachineArt.rgb(255, 176, 40);
    private static final int REFUSED = MachineArt.rgb(239, 106, 90);
    private static final int RULE = MachineArt.rgb(40, 70, 44);
    private static final int TRACK = MachineArt.rgb(24, 40, 26);
    private static final int FILLED = MachineArt.rgb(90, 230, 120);
    /* The buttons' caps, their collar, and the counter's wheels and digits. */
    private static final int AMBER = MachineArt.rgb(240, 178, 40);
    private static final int RED = MachineArt.rgb(220, 50, 40);
    private static final int COLLAR = MachineArt.rgb(40, 42, 46);
    private static final int WHEELS = MachineArt.rgb(12, 12, 14);
    private static final int DIGITS = MachineArt.rgb(236, 236, 228);
    private static final int ALLOWED = MachineArt.rgb(95, 224, 122);
    private static final int OFF = MachineArt.rgb(239, 106, 90);
    private static final String ELLIPSIS = "...";

    private PrinterQueueTab() {
    }

    /** The printer's Queue tab, after the five every machine has: a written book its icon. */
    public static MachineFaces.OwnTab tab() {
        return new MachineFaces.OwnTab(new ItemStack(Items.WRITTEN_BOOK), LargeScalePrinterTexts.QUEUE,
                machine -> new PrinterQueueTab());
    }

    /* Its plates, with the name its Main tab's plate writes, as far as its buttons leave room. */
    @Override
    public boolean drawsPlates() {
        return true;
    }

    @Override
    public void draw(final MachineView v) {
        final LargeScalePrinterBlockEntity printer = (LargeScalePrinterBlockEntity) v.machine();
        final GuiGraphics g = v.g();
        MachineChrome.plates(v, GameText.resolve(ChainFaceWord.LS_PRINTER.text().text()), PLATE_ROOM);
        MachineArt.drop(g, v.x(SCREEN_X0), v.y(SCREEN_Y0), v.x(SCREEN_X1), v.y(SCREEN_Y1));
        MachineArt.shade(g, v.x(SCREEN_X0), v.y(SCREEN_Y0), v.x(SCREEN_X1), v.y(SCREEN_Y1), FRAME);
        MachineArt.fill(g, v.x(SCREEN_X0 + GLASS_INSET), v.y(SCREEN_Y0 + GLASS_INSET), v.x(SCREEN_X1 - GLASS_INSET),
                v.y(SCREEN_Y1 - GLASS_INSET), GLASS);
        queue(v, printer);
        now(v, printer);
        button(v, PAUSE_X, AMBER, printer.paused() ? LargeScalePrinterTexts.RESUME : LargeScalePrinterTexts.PAUSE,
                true);
        button(v, CANCEL_X, RED, LargeScalePrinterTexts.CANCEL, !printer.rows().isEmpty());
        MachineArt.engrave(g, v.font(), say(LargeScalePrinterTexts.PRINTED), v.x(PRINTED_X), v.y(PRINTED_Y), true);
        final String counted = String.format(Locale.ROOT, "%0" + COUNTER_DIGITS + "d", printer.printed());
        MachineArt.fill(g, v.x(COUNTER_X), v.y(COUNTER_Y), v.x(COUNTER_X + 4 * counted.length() + 2),
                v.y(COUNTER_Y + LAMP_SIZE), WHEELS);
        MachineArt.text(g, v.font(), counted, v.x(COUNTER_X + 1), v.y(COUNTER_Y + 1), DIGITS);
        MachineArt.engrave(g, v.font(), say(LargeScalePrinterTexts.ENCHANTED), v.x(ENCHANTED_X), v.y(ENCHANTED_Y),
                true);
        MachineArt.lamp(g, v.x(LAMP_X), v.y(LAMP_Y), printer.enchantedAllowed() ? ALLOWED : OFF, true);
        MachineArt.engrave(g, v.font(), say(LargeScalePrinterTexts.GO_TO), v.x(LEVER_WORD_X), v.y(LEVER_WORD_Y),
                true);
        MachineArt.rocker(g, v.x(LEVER_X), v.y(LEVER_Y), printer.toNetwork());
        MachineArt.engrave(g, v.font(), say(LargeScalePrinterTexts.THE_NETWORK), v.x(PLACE_X), v.y(NET_Y), false);
        MachineArt.engrave(g, v.font(), say(LargeScalePrinterTexts.THE_CHEST), v.x(PLACE_X), v.y(CHEST_Y), false);
    }

    @Override
    public boolean clicked(final MachineView v, final double x, final double y, final int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        final LargeScalePrinterBlockEntity printer = (LargeScalePrinterBlockEntity) v.machine();
        if (onButton(x, y, PAUSE_X)) {
            v.press().accept(MachineMenu.FIRST_OWN + LargeScalePrinterBlockEntity.PAUSE);
            return true;
        }
        if (!printer.rows().isEmpty() && onButton(x, y, CANCEL_X)) {
            v.press().accept(MachineMenu.FIRST_OWN + LargeScalePrinterBlockEntity.CANCEL);
            return true;
        }
        if (onLever(x, y)) {
            v.press().accept(MachineMenu.FIRST_OWN + LargeScalePrinterBlockEntity.DESTINATION);
            return true;
        }
        return false;
    }

    @Override
    public List<Component> tooltip(final MachineView v, final double x, final double y) {
        final LargeScalePrinterBlockEntity printer = (LargeScalePrinterBlockEntity) v.machine();
        if (onLever(x, y)) {
            return List.of(GameText.component(LargeScalePrinterTexts.SWITCH_TIP.text()));
        }
        if (MachineScreenLayout.on(x, y, LAMP_X - 2, LAMP_Y - 2, LAMP_SIZE + 4, LAMP_SIZE + 4)) {
            return List.of(GameText.component(LargeScalePrinterTexts.ENCHANTED_TIP.with((printer.enchantedAllowed()
                    ? LargeScalePrinterTexts.ALLOWED : LargeScalePrinterTexts.OFF).text())));
        }
        return List.of();
    }

    /* The queue's rows: each book's icon, its title and how many, where it came from and whether it prints. */
    private static void queue(final MachineView v, final LargeScalePrinterBlockEntity printer) {
        final List<LargeScalePrinterBlockEntity.Row> rows = printer.rows();
        final Font font = v.font();
        final int room = WORDS_RIGHT - WORDS_X;
        if (rows.isEmpty()) {
            int y = ROW_Y;
            for (final TextKey line : List.of(LargeScalePrinterTexts.EMPTY_QUEUE,
                    LargeScalePrinterTexts.EMPTY_QUEUE_2)) {
                for (final String part : wrap(font, say(line), WORDS_RIGHT - ROW_X)) {
                    MachineArt.text(v.g(), font, part, v.x(ROW_X), v.y(y), DIM);
                    y += SECOND_LINE;
                }
            }
            return;
        }
        final int shown = rows.size() > ROWS ? ROWS - 1 : rows.size();
        for (int i = 0; i < shown; i++) {
            final LargeScalePrinterBlockEntity.Row row = rows.get(i);
            final int y = ROW_Y + i * ROW_PITCH;
            v.g().renderItem(icon(row.kind()), v.x(ROW_X), v.y(y));
            MachineArt.text(v.g(), font, clip(font, say(LargeScalePrinterTexts.TIMES.with(row.title(),
                    row.copies())), room), v.x(WORDS_X), v.y(y + 1), BRIGHT);
            // An enchanted book the server has since turned off waits in the queue, saying why it will not print.
            final boolean refused = row.kind() == BookJob.Kind.ENCHANTED && !printer.enchantedAllowed();
            final String state = refused ? say(LargeScalePrinterTexts.OFF_HERE)
                    : say((row.printing() ? LargeScalePrinterTexts.FROM_PRINTING
                            : LargeScalePrinterTexts.FROM_WAITING).with(row.from()));
            MachineArt.text(v.g(), font, clip(font, state, room), v.x(WORDS_X), v.y(y + 1 + SECOND_LINE),
                    refused ? REFUSED : row.printing() ? PRINTING : WAITING);
        }
        if (shown < rows.size()) {
            MachineArt.text(v.g(), font, say(LargeScalePrinterTexts.MORE.with(rows.size() - shown)), v.x(WORDS_X),
                    v.y(ROW_Y + shown * ROW_PITCH + 1), DIM);
        }
    }

    /* The copy it is on, its bar, and what a book takes. */
    private static void now(final MachineView v, final LargeScalePrinterBlockEntity printer) {
        final GuiGraphics g = v.g();
        MachineArt.hline(g, v.x(ROW_X), v.x(ROW_X + BAR_WIDTH), v.y(RULE_Y), RULE);
        final List<LargeScalePrinterBlockEntity.Row> rows = printer.rows();
        final String now = rows.isEmpty() ? say(LargeScalePrinterTexts.NOTHING)
                : say(LargeScalePrinterTexts.COPY_OF.with(printer.done() + 1, rows.getFirst().copies()));
        MachineArt.text(g, v.font(), say(LargeScalePrinterTexts.NOW.with(now)), v.x(ROW_X), v.y(NOW_Y), BRIGHT);
        MachineArt.fill(g, v.x(ROW_X), v.y(BAR_Y), v.x(ROW_X + BAR_WIDTH), v.y(BAR_Y + BAR_HEIGHT), TRACK);
        final float progress = !rows.isEmpty() && printer.status() == MachineStatus.WORKING ? printer.progress() : 0F;
        MachineArt.fill(g, v.x(ROW_X), v.y(BAR_Y), v.x(ROW_X + (int) (BAR_WIDTH * progress)),
                v.y(BAR_Y + BAR_HEIGHT), FILLED);
        MachineArt.text(g, v.font(), say(LargeScalePrinterTexts.A_BOOK.with(
                LargeScalePrinterBlockEntity.SHEETS_A_BOOK)), v.x(ROW_X), v.y(SHEETS_Y), DIM);
    }

    /* A mushroom push button in its collar, its word engraved under it, its cap brighter under the mouse. */
    private static void button(final MachineView v, final int x, final int colour, final TextKey word,
                               final boolean pressable) {
        final boolean lit = pressable && onButton(v.faceMouseX(), v.faceMouseY(), x);
        MachineArt.disc(v.g(), v.x(x), v.y(BUTTONS_Y), BUTTON_REACH, COLLAR, false);
        MachineArt.disc(v.g(), v.x(x), v.y(BUTTONS_Y), CAP, pressable ? lit ? MachineArt.mix(colour,
                MachineArt.rgb(255, 255, 255), 0.15) : colour : MachineArt.mix(colour, COLLAR, 0.6), true);
        MachineArt.engrave(v.g(), v.font(), say(word), v.x(x), v.y(BUTTONS_Y + LABEL_BELOW), true);
    }

    private static boolean onButton(final double x, final double y, final int middle) {
        return Math.hypot(x - middle, y - BUTTONS_Y) <= BUTTON_REACH;
    }

    private static boolean onLever(final double x, final double y) {
        return MachineScreenLayout.on(x, y, LEVER_X, LEVER_Y, MachineScreenLayout.SWITCH_WIDTH,
                MachineScreenLayout.SWITCH_HEIGHT);
    }

    /* A sentence as the tab's stencils and screen write it: in capitals. */
    private static String say(final TextKey key) {
        return say(key.text());
    }

    private static String say(final Text text) {
        return GameText.resolve(text).toUpperCase(Locale.ROOT);
    }

    /* The book a row prints, as its icon: a system's book is drawn as a plain book. */
    private static ItemStack icon(final BookJob.Kind kind) {
        return new ItemStack(switch (kind) {
            case WRITTEN -> Items.WRITTEN_BOOK;
            case QUILL -> Items.WRITABLE_BOOK;
            case ENCHANTED -> Items.ENCHANTED_BOOK;
            case MANUAL -> Items.BOOK;
        });
    }

    /* A small line cut to {@code room} pixels, with an ellipsis. */
    private static String clip(final Font font, final String text, final int room) {
        if (MachineArt.width(font, text) <= room) {
            return text;
        }
        String cut = text;
        while (!cut.isEmpty() && MachineArt.width(font, cut + ELLIPSIS) > room) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + ELLIPSIS;
    }

    /* A small sentence broken into lines of at most {@code room} pixels, at its spaces. */
    private static List<String> wrap(final Font font, final String text, final int room) {
        final List<String> lines = new ArrayList<>();
        String line = "";
        for (final String word : text.split(" ")) {
            final String longer = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && MachineArt.width(font, longer) > room) {
                lines.add(line);
                line = word;
            } else {
                line = longer;
            }
        }
        if (!line.isEmpty()) {
            lines.add(line);
        }
        return lines;
    }
}
