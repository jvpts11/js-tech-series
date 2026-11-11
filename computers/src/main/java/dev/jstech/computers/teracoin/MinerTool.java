/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.computers.program.tty.ITtyProcess;
import dev.jstech.computers.program.tty.ITtySink;
import dev.jstech.computers.program.tty.TtyQuestion;
import dev.jstech.core.text.Text;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/**
 * Blockwright's miner in front of a terminal (computers/docs/design/teracoin.md, "Mining"): it keeps the computer
 * mining while it runs, stamps each new block of the chain, says what it hashes once a minute and, alone, how long a
 * block would take it, until Q is typed.
 */
public final class MinerTool implements ITtyProcess {

    private final MinerConsole console;
    private long lastLine = Long.MIN_VALUE;
    private int lastHeight = -1;
    private boolean hinted;
    private boolean waitingTold;
    private boolean firstHashing = true;
    private boolean over;

    /* The ticks between two hashing lines: a minute; the first one comes a second in. */
    private static final long LINE_TICKS = 1_200L;
    private static final long FIRST_LINE = 20L;

    public MinerTool(final MinerConsole console) {
        this.console = console;
    }

    @Override
    public void begin(final long now) {
        lastLine = now - LINE_TICKS + FIRST_LINE;
        console.settings().toolRan(now);
    }

    @Override
    public void advance(final long now, @Nullable final ITtySink out) {
        if (over) {
            return;
        }
        console.settings().toolRan(now);
        final MinerConsole.Snapshot state = console.snapshot();
        switch (state.status()) {
            case NO_ADDRESS, NO_POOL, NOTHING_HASHES -> {
                say(out, console.stall(state.status(), state.pool()));
                stop(out);
                return;
            }
            case WAITING_FOR_OWNER -> {
                if (!waitingTold) {
                    waitingTold = true;
                    say(out, console.stall(state.status(), state.pool()));
                }
                return;
            }
            default -> waitingTold = false;
        }
        if (state.height() != lastHeight) {
            if (lastHeight >= 0 && !state.newest().isEmpty()
                    && state.newest().get(0).machine().equals(console.machine())) {
                stamped(out, MinerTexts.FOUND.with(grouped(state.height())), CliStyle.OK);
            }
            lastHeight = state.height();
            stamped(out, MinerTexts.BLOCK_LINE.with(grouped(state.height()), grouped(state.difficulty()),
                    Trc.format(state.reward(), state.reward() % Trc.UNITS == 0L ? 0 : Trc.DECIMALS)), CliStyle.CYAN);
        }
        if (now - lastLine < LINE_TICKS || state.hashrate() <= 0L) {
            return;
        }
        lastLine = now;
        final String rate = TeracoinHashrate.format(state.hashrate());
        if (firstHashing) {
            firstHashing = false;
            stamped(out, MinerTexts.HASHING_WATTS.with(rate, grouped(state.watts())), CliStyle.PLAIN);
        } else if (state.status() == MinerSettings.Status.ALONE && !hinted) {
            hinted = true;
            stamped(out, MinerTexts.ALONE_EVERY.with(rate, MinerConsole.duration(state.difficulty()
                    / Math.max(1L, state.hashrate()))), CliStyle.PLAIN);
            stamped(out, MinerTexts.POOL_HINT.text(), CliStyle.DIM);
        } else {
            stamped(out, MinerTexts.HASHING.with(rate), CliStyle.PLAIN);
        }
    }

    @Override
    @Nullable
    public TtyQuestion asking() {
        return over ? null : new TtyQuestion(CliLine.of(MinerTexts.PRESS_Q, CliStyle.DIM), false);
    }

    @Override
    public void answer(final String line, final long now, @Nullable final ITtySink out) {
        if (line.trim().toLowerCase(Locale.ROOT).equals("q")) {
            stop(out);
        }
    }

    @Override
    public void interrupt(final long now, @Nullable final ITtySink out) {
        stop(out);
    }

    @Override
    public boolean over() {
        return over;
    }

    private void stop(@Nullable final ITtySink out) {
        if (over) {
            return;
        }
        over = true;
        console.settings().toolStopped();
        say(out, CliLine.of(MinerTexts.STOPPED, CliStyle.DIM));
    }

    private void stamped(@Nullable final ITtySink out, final Text text, final CliStyle style) {
        say(out, new CliLine(MinerTexts.STAMP.with(console.clock(), text), style));
    }

    private static void say(@Nullable final ITtySink out, final CliLine line) {
        if (out != null) {
            out.line(line);
        }
    }

    private static String grouped(final long number) {
        return String.format(Locale.ROOT, "%,d", number);
    }
}
