/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.machine;

import static dev.jstech.core.machine.MachineScreenLayout.OUT_METER_HEIGHT;
import static dev.jstech.core.machine.MachineScreenLayout.OUT_METER_WIDTH;
import static dev.jstech.core.machine.MachineScreenLayout.OUT_METER_X;
import static dev.jstech.core.machine.MachineScreenLayout.OUT_METER_Y;
import static dev.jstech.core.machine.MachineScreenLayout.READS_KNOB_R;
import static dev.jstech.core.machine.MachineScreenLayout.READS_KNOB_X;
import static dev.jstech.core.machine.MachineScreenLayout.READS_KNOB_Y;
import static dev.jstech.core.machine.MachineScreenLayout.RUNS_KNOB_R;
import static dev.jstech.core.machine.MachineScreenLayout.RUNS_KNOB_X;
import static dev.jstech.core.machine.MachineScreenLayout.RUNS_KNOB_Y;
import static dev.jstech.core.machine.MachineScreenLayout.SIGNAL_PITCH;
import static dev.jstech.core.machine.MachineScreenLayout.SIGNAL_X;
import static dev.jstech.core.machine.MachineScreenLayout.SIGNAL_Y;

import dev.jstech.core.machine.ComparatorReads;
import dev.jstech.core.machine.MachineControl;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.core.machine.MachineScreenTexts;
import dev.jstech.core.machine.RedstoneMode;
import dev.jstech.core.signal.Reading;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The Redstone tab, as the approved design draws it: the knob of how the machine answers redstone, the knob of what
 * a comparator reads of it, a row of fifteen lamps lit as high as the signal coming in, and the meter of what the
 * comparator beside it reads.
 */
public final class RedstoneTab implements IMachineFace {

    private static final List<String> OUT_MARKS = List.of("0", "5", "10", "15");
    private static final int LAMP = 5;
    private static final int LEVEL_X = 172;
    private static final int SIGNAL_WORD_Y = 92;
    private static final int CMP_WORD_Y = 40;
    private static final int CMP_NUMBER_Y = 98;
    private static final int LEVEL_DIGITS = 2;
    private static final int COLUMN_ROOM = 54;

    @Override
    public void draw(final MachineView v) {
        final GuiGraphics g = v.g();
        final MachineArt.Colours c = MachineArt.PALETTE.get();
        final MachineControl control = v.machine().control();
        knob(v, RUNS_KNOB_X, RUNS_KNOB_Y, RUNS_KNOB_R, runsWords(), RedstoneMode.round().indexOf(control.mode()),
                MachineScreenTexts.RUNS);
        knob(v, READS_KNOB_X, READS_KNOB_Y, READS_KNOB_R, readsWords(),
                ComparatorReads.round().indexOf(control.reads()), MachineScreenTexts.COMPARATOR);
        MachineArt.engrave(g, v.font(), MachineWords.upper(MachineScreenTexts.SIGNAL_IN), v.x(SIGNAL_X),
                v.y(SIGNAL_WORD_Y), false);
        final int level = control.signal();
        for (int k = 0; k < Reading.FULL; k++) {
            final int x = v.x(SIGNAL_X + k * SIGNAL_PITCH);
            final int y = v.y(SIGNAL_Y);
            final boolean lit = k < level;
            MachineArt.fill(g, x - 1, y - 1, x + LAMP + 1, y + LAMP + 1, MachineArt.rgb(50, 54, 60));
            MachineArt.fill(g, x, y, x + LAMP, y + LAMP, lit ? c.redstone()
                    : MachineArt.mix(c.redstone(), c.black(), 0.68));
            if (lit) {
                MachineArt.dot(g, x + 1, y + 1, MachineArt.mix(c.redstone(), c.white(), 0.55));
            }
        }
        MachineArt.engrave(g, v.font(), padded(level), v.x(LEVEL_X), v.y(SIGNAL_Y + 1), false);
        final int out = v.machine().strength();
        MachineArt.engrave(g, v.font(), MachineArt.fit(v.font(), MachineWords.upper(MachineScreenTexts.CMP_OUT),
                COLUMN_ROOM), v.x(OUT_METER_X + OUT_METER_WIDTH / 2), v.y(CMP_WORD_Y), true);
        MachineArt.panelMeter(g, v.font(), v.x(OUT_METER_X), v.y(OUT_METER_Y), OUT_METER_WIDTH, OUT_METER_HEIGHT,
                out / (double) Reading.FULL, OUT_MARKS, false);
        MachineArt.engrave(g, v.font(), String.valueOf(out), v.x(OUT_METER_X + OUT_METER_WIDTH / 2),
                v.y(CMP_NUMBER_Y), true);
    }

    @Override
    public boolean clicked(final MachineView v, final double x, final double y, final int button) {
        final MachineControl control = v.machine().control();
        final int runs = choice(v, x, y, RUNS_KNOB_X, RUNS_KNOB_Y, RUNS_KNOB_R, runsWords(),
                RedstoneMode.round().indexOf(control.mode()));
        if (runs >= 0) {
            v.press().accept(MachineMenu.FIRST_MODE + runs);
            return true;
        }
        final int reads = choice(v, x, y, READS_KNOB_X, READS_KNOB_Y, READS_KNOB_R, readsWords(),
                ComparatorReads.round().indexOf(control.reads()));
        if (reads >= 0) {
            v.press().accept(MachineMenu.FIRST_READS + reads);
            return true;
        }
        return false;
    }

    @Override
    public List<Component> tooltip(final MachineView v, final double x, final double y) {
        final MachineControl control = v.machine().control();
        if (MachineScreenLayout.onDisc(x, y, RUNS_KNOB_X, RUNS_KNOB_Y, RUNS_KNOB_R)) {
            return List.of(GameText.component(MachineScreenTexts.TIP_RUNS.with(control.mode().text())),
                    GameText.component(MachineScreenTexts.TIP_KNOB_USE));
        }
        if (MachineScreenLayout.onDisc(x, y, READS_KNOB_X, READS_KNOB_Y, READS_KNOB_R)) {
            return List.of(GameText.component(MachineScreenTexts.TIP_READS.with(control.reads().text())),
                    GameText.component(MachineScreenTexts.TIP_KNOB_USE));
        }
        if (MachineScreenLayout.on(x, y, SIGNAL_X - 1, SIGNAL_Y - 1, Reading.FULL * SIGNAL_PITCH, LAMP + 2)) {
            return List.of(GameText.component(MachineScreenTexts.TIP_SIGNAL.with(control.signal())));
        }
        if (MachineScreenLayout.on(x, y, OUT_METER_X - 4, OUT_METER_Y - 4, OUT_METER_WIDTH + 8,
                OUT_METER_HEIGHT + 8)) {
            return List.of(GameText.component(MachineScreenTexts.TIP_CMP_OUT.with(v.machine().strength())));
        }
        return List.of();
    }

    private static List<TextKey> runsWords() {
        final List<TextKey> words = new ArrayList<>();
        for (final RedstoneMode mode : RedstoneMode.round()) {
            words.add(switch (mode) {
                case IGNORE -> MachineScreenTexts.IGNORE;
                case HIGH -> MachineScreenTexts.HIGH;
                case LOW -> MachineScreenTexts.LOW;
                case PULSE -> MachineScreenTexts.PULSE;
            });
        }
        return words;
    }

    private static List<TextKey> readsWords() {
        final List<TextKey> words = new ArrayList<>();
        for (final ComparatorReads reads : ComparatorReads.round()) {
            words.add(switch (reads) {
                case PROGRESS -> MachineScreenTexts.PROG;
                case INPUT -> MachineScreenTexts.MODE_IN;
                case OUTPUT -> MachineScreenTexts.MODE_OUT;
                case ENERGY -> MachineScreenTexts.NRG;
            });
        }
        return words;
    }

    /* A knob of four positions, its {@code words} round it, pointing at {@code chosen}, its {@code title} above. */
    private static void knob(final MachineView v, final int cx, final int cy, final int r, final List<TextKey> words,
                             final int chosen, final TextKey title) {
        final List<MachineArt.KnobLabel> labels = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            labels.add(new MachineArt.KnobLabel(MachineWords.upper(words.get(i)), MachineScreenLayout.knobAngle(i),
                    i == chosen));
        }
        MachineArt.knob(v.g(), v.font(), v.x(cx), v.y(cy), r, MachineScreenLayout.knobAngle(chosen), labels,
                MachineWords.upper(title));
    }

    /*
     * The position a click at ({@code x}, {@code y}) turns a knob to: the word clicked, or the next one for the knob
     * itself; -1 for neither.
     */
    private static int choice(final MachineView v, final double x, final double y, final int cx, final int cy,
                              final int r, final List<TextKey> words, final int chosen) {
        if (MachineScreenLayout.onDisc(x, y, cx, cy, r)) {
            return (chosen + 1) % words.size();
        }
        for (int i = 0; i < words.size(); i++) {
            if (MachineScreenLayout.onWord(x, y, MachineScreenLayout.knobWord(cx, cy, r,
                    MachineScreenLayout.knobAngle(i)), MachineArt.width(v.font(), MachineWords.upper(words.get(i))))) {
                return i;
            }
        }
        return -1;
    }

    /* The level written two wide, so its last digit stays put as it changes. */
    private static String padded(final int level) {
        final String written = String.valueOf(level);
        return " ".repeat(Math.max(0, LEVEL_DIGITS - written.length())) + written;
    }
}
