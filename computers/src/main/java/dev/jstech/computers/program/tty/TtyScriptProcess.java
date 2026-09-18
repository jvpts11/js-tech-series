/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.tty;

import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliStyle;
import java.util.ArrayDeque;
import java.util.Deque;
import org.jetbrains.annotations.Nullable;

/**
 * Plays a {@link TtyScript} against the machine's clock.
 *
 * <p>The clock is only ever read, never kept: every step is reckoned from the tick the one before it ended
 * on, so being moved along once a tick and being moved along once after an hour come to the same place. That
 * is what lets a machine in a chunk nobody is near carry on with its compile, and what lets a script be played
 * again from the top after a load to find where it had got to.
 *
 * <p>A question stops the clock. A tool waiting to be told whether to go ahead is not getting on with
 * anything, so the time a player takes to answer is not time the download was running.
 */
public final class TtyScriptProcess implements ITtyProcess {

    private final Deque<TtyScript.IStep> steps = new ArrayDeque<>();

    /** The tick the step in front started on. */
    private long cursor;

    /** How much of the step in front has been printed: a flood's lines, or whether a bar has been drawn at all. */
    private int printed;

    /** What the tool said when it was stopped from outside, which the real ones print as they go. */
    private static final CliLine INTERRUPTED = new CliLine("^C", CliStyle.DIM);

    public TtyScriptProcess(final TtyScript script) {
        this.steps.addAll(script.steps());
    }

    @Override
    public void begin(final long now) {
        this.cursor = now;
    }

    @Override
    public void advance(final long now, @Nullable final ITtySink out) {
        while (!this.steps.isEmpty()) {
            final TtyScript.IStep step = this.steps.peekFirst();
            final boolean done = switch (step) {
                case TtyScript.Say say -> said(say, out);
                case TtyScript.Pause pause -> waited(pause.ticks(), now);
                case TtyScript.Redraw redraw -> redrawn(redraw, now, out);
                case TtyScript.Flood flood -> poured(flood, now, out);
                case TtyScript.Effect effect -> ran(effect);
                case TtyScript.Stop stop -> stopped();
                case TtyScript.Ask ask -> false;
            };
            if (!done) {
                return;
            }
            if (!this.steps.isEmpty() && this.steps.peekFirst() == step) {
                this.steps.removeFirst();
            }
            this.printed = 0;
        }
    }

    @Override
    @Nullable
    public TtyQuestion asking() {
        return this.steps.peekFirst() instanceof TtyScript.Ask ask
                ? new TtyQuestion(ask.question(), ask.masked()) : null;
    }

    @Override
    public void answer(final String line, final long now, @Nullable final ITtySink out) {
        if (!(this.steps.peekFirst() instanceof TtyScript.Ask ask)) {
            return;
        }
        this.steps.removeFirst();
        if (out != null) {
            /*
             * The question stays on the glass with what was typed after it, the way it does at a real
             * terminal, unless what was typed is not for showing.
             */
            out.line(ask.masked() ? ask.question()
                    : CliLine.build().add(ask.question()).plain(line).done());
        }
        final TtyScript next = ask.then().next(line);
        if (next != null) {
            final Deque<TtyScript.IStep> rest = new ArrayDeque<>(this.steps);
            this.steps.clear();
            this.steps.addAll(next.steps());
            this.steps.addAll(rest);
        }
        // The clock starts again from the answer: the wait for it was nobody's work.
        this.cursor = now;
        this.printed = 0;
        this.advance(now, out);
    }

    @Override
    public void interrupt(final long now, @Nullable final ITtySink out) {
        if (this.steps.isEmpty()) {
            return;
        }
        this.steps.clear();
        if (out != null) {
            out.line(INTERRUPTED);
        }
    }

    @Override
    public boolean over() {
        return this.steps.isEmpty();
    }

    private static boolean said(final TtyScript.Say say, @Nullable final ITtySink out) {
        if (out != null) {
            out.line(say.line());
        }
        return true;
    }

    private boolean waited(final int ticks, final long now) {
        if (now < this.cursor + ticks) {
            return false;
        }
        this.cursor += ticks;
        return true;
    }

    /**
     * A line drawn over itself until its time is up.
     *
     * <p>The first drawing is a line of its own and every one after it goes over that, so a bar never draws
     * over whatever happened to be printed before it.
     */
    private boolean redrawn(final TtyScript.Redraw redraw, final long now, @Nullable final ITtySink out) {
        final boolean done = now >= this.cursor + redraw.ticks();
        if (out != null) {
            final double progress = done ? 1.0 : Math.max(0.0, (now - this.cursor) / (double) redraw.ticks());
            final CliLine drawn = redraw.drawing().at(progress);
            if (this.printed == 0) {
                out.line(drawn);
            } else {
                out.redraw(drawn);
            }
        }
        this.printed = 1;
        if (done) {
            this.cursor += redraw.ticks();
        }
        return done;
    }

    /** As many of a flood's lines as are due by now, and no line of it twice. */
    private boolean poured(final TtyScript.Flood flood, final long now, @Nullable final ITtySink out) {
        final boolean done = now >= this.cursor + flood.ticks();
        final int due = done ? flood.count()
                : (int) (flood.count() * Math.max(0L, now - this.cursor) / flood.ticks());
        if (out != null) {
            /* Nobody saw the ones before; what they are given is what is going by now, not the backlog. */
            final int from = Math.max(this.printed, due - TtyScript.MAX_LINES_PER_TICK);
            for (int i = from; i < due; i++) {
                out.line(flood.source().line(i));
            }
        }
        this.printed = due;
        if (done) {
            this.cursor += flood.ticks();
        }
        return done;
    }

    private static boolean ran(final TtyScript.Effect effect) {
        effect.run().run();
        return true;
    }

    private boolean stopped() {
        this.steps.clear();
        return true;
    }
}
