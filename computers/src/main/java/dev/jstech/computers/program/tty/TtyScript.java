/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.tty;

import dev.jstech.computers.program.cli.CliLine;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * What a tool does from the moment it is started to the moment it gives the prompt back, written down in
 * order: say this, wait, redraw that line for a while, pour these lines out, ask, and at the end do the thing
 * the tool was run for.
 *
 * <p>Written down rather than run, because the machine plays it on its own clock. The same script can be
 * played with nobody watching, played again from the top after a world is loaded to find out where it had got
 * to, or cut short by Ctrl+C, and in none of those cases does the script itself have to know.
 *
 * <p>What a tool was run for happens at the end of it, never at the start. A fetch interrupted half way has
 * fetched nothing and a build interrupted has built nothing, so the machine the player comes back to is the
 * one the terminal said it was.
 */
public final class TtyScript {

    private final List<IStep> steps;

    /**
     * The most lines a tool pours out in one tick.
     *
     * <p>A real archive names a hundred thousand paths. Forty a tick is eight hundred a second, which is past
     * what anybody reads and well under what a machine with several terminals open can afford to send.
     */
    public static final int MAX_LINES_PER_TICK = 40;

    /** A script with nothing in it, which is what a question answers with when there is nothing more to do. */
    public static final TtyScript EMPTY = new TtyScript(List.of());

    private TtyScript(final List<IStep> steps) {
        this.steps = List.copyOf(steps);
    }

    public static Builder script() {
        return new Builder();
    }

    public List<IStep> steps() {
        return this.steps;
    }

    /** How long the script takes when nothing it asks is left waiting, in ticks. */
    public int ticks() {
        int total = 0;
        for (final IStep step : this.steps) {
            total += switch (step) {
                case Pause pause -> pause.ticks();
                case Redraw redraw -> redraw.ticks();
                case Flood flood -> flood.ticks();
                default -> 0;
            };
        }
        return total;
    }

    /** One thing a tool does. */
    public sealed interface IStep permits Say, Pause, Redraw, Flood, Ask, Effect, Stop {
    }

    /** Prints a line, at once. */
    public record Say(CliLine line) implements IStep {
    }

    /** Says nothing for a while, which is a tool working. */
    public record Pause(int ticks) implements IStep {
    }

    /** Draws one line over and over for a while, as a bar or a counter does, and leaves its last drawing. */
    public record Redraw(int ticks, IDrawing drawing) implements IStep {
    }

    /** Pours out that many lines, spread over that long. */
    public record Flood(int ticks, int count, ILineSource source) implements IStep {
    }

    /** Stops and asks, and carries on with whatever the answer leads to. */
    public record Ask(CliLine question, boolean masked, IAnswered then) implements IStep {
    }

    /** Does what the tool was run for. */
    public record Effect(Runnable run) implements IStep {
    }

    /** Ends the tool here, leaving whatever came after not done: what a "no" leads to. */
    public record Stop() implements IStep {
    }

    /** A line that changes as something proceeds, from nothing done to all of it. */
    @FunctionalInterface
    public interface IDrawing {

        /**
         * @param progress how far along, from 0 to 1, and exactly 1 on the last drawing
         */
        CliLine at(double progress);
    }

    /**
     * The lines of a flood, by where each comes in it.
     *
     * <p>Asked a line at a time rather than handed over as a list, so a tool that prints ten thousand lines
     * makes only the ones somebody is there to read.
     */
    @FunctionalInterface
    public interface ILineSource {

        CliLine line(int index);
    }

    /** What an answer leads to. */
    @FunctionalInterface
    public interface IAnswered {

        /**
         * @return what the tool does next before carrying on with the rest, or null to carry straight on
         */
        @Nullable
        TtyScript next(String answer);
    }

    /** Writes a script down a step at a time. */
    public static final class Builder {

        private final List<IStep> steps = new ArrayList<>();

        private Builder() {
        }

        public Builder say(final CliLine line) {
            this.steps.add(new Say(line));
            return this;
        }

        public Builder say(final String text) {
            return this.say(CliLine.plain(text));
        }

        /** Several lines at once, which is a tool that had them all ready. */
        public Builder sayAll(final List<CliLine> lines) {
            for (final CliLine line : lines) {
                this.say(line);
            }
            return this;
        }

        public Builder pause(final int ticks) {
            if (ticks > 0) {
                this.steps.add(new Pause(ticks));
            }
            return this;
        }

        public Builder redraw(final int ticks, final IDrawing drawing) {
            this.steps.add(new Redraw(Math.max(1, ticks), drawing));
            return this;
        }

        /**
         * Pours lines out over a while, no faster than the terminal is allowed to be sent them.
         *
         * <p>Asked for more than fit, it pours out what fits: the lines a tool prints are there to be watched
         * going by, and how many of them went by is not something anybody counts.
         */
        public Builder flood(final int ticks, final int count, final ILineSource source) {
            final int time = Math.max(1, ticks);
            this.steps.add(new Flood(time, Math.max(0, Math.min(count, time * MAX_LINES_PER_TICK)), source));
            return this;
        }

        public Builder ask(final CliLine question, final IAnswered then) {
            this.steps.add(new Ask(question, false, then));
            return this;
        }

        /** Asks for something that is not to be shown as it is typed. */
        public Builder askUnseen(final CliLine question, final IAnswered then) {
            this.steps.add(new Ask(question, true, then));
            return this;
        }

        public Builder effect(final Runnable run) {
            this.steps.add(new Effect(run));
            return this;
        }

        public Builder stop() {
            this.steps.add(new Stop());
            return this;
        }

        /** Everything another script does, after what is here already. */
        public Builder then(final TtyScript other) {
            this.steps.addAll(other.steps());
            return this;
        }

        public TtyScript done() {
            return new TtyScript(this.steps);
        }
    }
}
