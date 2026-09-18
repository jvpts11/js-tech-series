/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.computers.program.tty.ITtyProcess;
import dev.jstech.computers.program.tty.ITtySink;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/**
 * The tool running in front of a machine's terminal, and enough written down about it to find it again.
 *
 * <p>A machine has one prompt, so it has at most one of these. While there is one the prompt stays away,
 * what is typed goes to the tool, and the machine moves the tool along on its own clock whether or not anybody
 * is watching it.
 *
 * <p>A tool is a thing in memory and a world is a thing on disk, and a compile that takes five minutes has to
 * outlast the player walking out of the chunk. So what is written down is not the tool but how it was made:
 * the line that started it, the tick it started on, and what it was told when it asked. A tool does what it
 * was run for only at its end, so until then the machine is exactly as it was when the line was typed, and
 * typing the line again in the same place makes the same tool, which is then told the same answers at the
 * same ticks and moved along to now without printing any of it.
 */
public final class TerminalForeground {

    private final List<Answer> answers = new ArrayList<>();
    @Nullable
    private ITtyProcess tool;
    private String line = "";
    private long startedAt;

    private static final String LINE = "Line";
    private static final String STARTED = "Started";
    private static final String ANSWERS = "Answers";
    private static final String TICK = "Tick";
    private static final String TEXT = "Text";

    /** The tool in front, or null when the prompt is there instead. */
    @Nullable
    public ITtyProcess tool() {
        return this.tool;
    }

    /** Whether something is written down as running, found again yet or not. */
    public boolean running() {
        return this.tool != null || !this.line.isEmpty();
    }

    /**
     * Puts a tool in front of the terminal.
     *
     * @param line what was typed to start it, which is what it is found again by
     */
    public void begin(final ITtyProcess tool, final String line, final long now) {
        this.tool = tool;
        this.line = line;
        this.startedAt = now;
        this.answers.clear();
        tool.begin(now);
    }

    /**
     * Moves the tool along, and lets it go if that was the end of it.
     *
     * @return whether the tool ended on this turn, which is when the prompt comes back
     */
    public boolean advance(final long now, @Nullable final ITtySink out) {
        if (this.tool == null) {
            return false;
        }
        this.tool.advance(now, out);
        return this.settle();
    }

    /**
     * Hands the tool what was typed at it.
     *
     * @param unseen whether what was typed was not for showing, in which case it is not for keeping either
     * @return whether the tool ended on it
     */
    public boolean answer(final String typed, final boolean unseen, final long now,
                          @Nullable final ITtySink out) {
        if (this.tool == null || this.tool.asking() == null) {
            return false;
        }
        this.answers.add(new Answer(now, unseen ? "" : typed));
        this.tool.answer(typed, now, out);
        return this.settle();
    }

    /** Ctrl+C. The tool stops and the prompt comes back; whatever it had not finished stays not done. */
    public void interrupt(final long now, @Nullable final ITtySink out) {
        if (this.tool != null) {
            this.tool.interrupt(now, out);
        }
        this.clear();
    }

    /** Lets go of whatever was in front, as a machine switched off does. */
    public void clear() {
        this.tool = null;
        this.line = "";
        this.answers.clear();
    }

    /**
     * Finds again a tool that was running when the world was saved.
     *
     * @param remake makes a tool from the line that was typed, or nothing when that line no longer starts one
     */
    public void findAgain(final Function<String, ITtyProcess> remake, final long now) {
        if (this.tool != null || this.line.isEmpty()) {
            return;
        }
        final ITtyProcess again = remake.apply(this.line);
        if (again == null) {
            this.clear();
            return;
        }
        again.begin(this.startedAt);
        for (final Answer answer : this.answers) {
            again.advance(answer.tick(), null);
            again.answer(answer.text(), answer.tick(), null);
        }
        again.advance(now, null);
        this.tool = again;
        this.settle();
    }

    public void save(final CompoundTag tag) {
        if (this.line.isEmpty()) {
            return;
        }
        tag.putString(LINE, this.line);
        tag.putLong(STARTED, this.startedAt);
        final ListTag told = new ListTag();
        for (final Answer answer : this.answers) {
            final CompoundTag one = new CompoundTag();
            one.putLong(TICK, answer.tick());
            one.putString(TEXT, answer.text());
            told.add(one);
        }
        tag.put(ANSWERS, told);
    }

    public void load(final CompoundTag tag) {
        this.clear();
        this.line = tag.getString(LINE);
        this.startedAt = tag.getLong(STARTED);
        for (final Tag each : tag.getList(ANSWERS, Tag.TAG_COMPOUND)) {
            final CompoundTag one = (CompoundTag) each;
            this.answers.add(new Answer(one.getLong(TICK), one.getString(TEXT)));
        }
    }

    private boolean settle() {
        if (this.tool != null && this.tool.over()) {
            this.clear();
            return true;
        }
        return false;
    }

    /** What a tool was told, and when, so it can be told again. */
    private record Answer(long tick, String text) {
    }
}
