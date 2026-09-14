/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.core.language.ILanguageProcess;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;

/**
 * A program of a language other than the machine's own, as the machine runs it.
 *
 * <p>Everything a language gives a machine is passed straight through. What only the machine's own language can do
 * takes the table's answers for a language that has none of it, and the program ends with one for a halt and zero
 * otherwise.
 */
final class HostedRuntime implements IMachineRuntime {

    private final ILanguageProcess process;

    HostedRuntime(final ILanguageProcess process) {
        this.process = process;
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
    public String message() {
        return this.process.message();
    }

    @Override
    public List<String> console() {
        return this.process.console();
    }

    @Override
    public long written() {
        return this.process.written();
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
        return this.process.heapBytes();
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
