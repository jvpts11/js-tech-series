/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.core.language.ILanguageProcess;
import dev.jstech.core.language.IProgrammingLanguage;
import dev.jstech.core.text.Text;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;

/**
 * A program of a language other than the machine's own, as the machine runs it.
 *
 * <p>Everything a language gives a machine is passed straight through, and what the program wrote and the memory it may
 * hold come from the view its language was given. What only the machine's own programs can do takes the table's answers
 * for a language that has none of it, and the program ends with one for a halt and zero otherwise.
 */
final class HostedRuntime implements IMachineRuntime {

    private final ILanguageProcess process;
    /** The program's view of the machine, which keeps what it writes and knows how much it may hold. */
    private final HostedView view;
    /** The language that started or brought the program back, which a save names beside it. */
    private final IProgrammingLanguage language;
    /** Whether the machine has been told the program ended, which its language has no way to say itself. */
    private boolean told;

    HostedRuntime(final ILanguageProcess process, final HostedView view, final IProgrammingLanguage language) {
        this.process = process;
        this.view = view;
        this.language = language;
    }

    @Override
    public IProgrammingLanguage language() {
        return this.language;
    }

    @Override
    public boolean endedUnannounced() {
        if (this.told) {
            return false;
        }
        final State state = this.process.state();
        this.told = state == State.HALTED || (state == State.FINISHED && !this.process.isService());
        return this.told;
    }

    @Override
    public int step(final int budget) {
        return this.process.step(budget);
    }

    @Override
    public void identify(final int id) {
        this.process.identify(id);
    }

    @Override
    public State state() {
        return this.process.state();
    }

    @Override
    public Text message() {
        final Text said = this.process.message();
        return said == null ? Text.EMPTY : said;
    }

    @Override
    public List<String> console() {
        return this.view.lines().stream().map(Text::english).toList();
    }

    @Override
    public List<Text> consoleText() {
        return this.view.lines();
    }

    @Override
    public long written() {
        return this.view.written();
    }

    @Override
    public long spent() {
        return this.process.spent();
    }

    @Override
    public long heldBytes() {
        return this.process.heldBytes();
    }

    @Override
    public long heapBytes() {
        return this.view.memoryQuota();
    }

    @Override
    public boolean isService() {
        return this.process.isService();
    }

    @Override
    public void onTick() {
        this.process.onTick();
    }

    @Override
    public void onStop(final int budget) {
        this.process.onStop(budget);
    }

    @Override
    public List<String> watching() {
        return this.process.watching();
    }

    @Override
    public void deliver(final Map<String, Long> totals) {
        this.process.deliver(totals);
    }

    @Override
    public void offerInput(final String line) {
        this.process.offerInput(line);
    }

    @Override
    public boolean waitingForInput() {
        return this.process.waitingForInput();
    }

    @Override
    public String name() {
        return this.process.name();
    }

    @Override
    public void save(final CompoundTag tag) {
        this.process.save(tag);
    }

    @Override
    public int exitCode() {
        return this.process.state() == State.HALTED ? 1 : 0;
    }
}
