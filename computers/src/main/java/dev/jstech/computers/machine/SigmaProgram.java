/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import com.mojang.logging.LogUtils;
import dev.jstech.computers.vm.listing.Shape;
import dev.jstech.computers.vm.program.Process;
import dev.jstech.computers.vm.program.SnapshotException;
import dev.jstech.computers.vm.program.Values;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import org.slf4j.Logger;

/**
 * One running Σ# program, as a machine sees it.
 *
 * <p>Everything on this side of it is in the machine's words (a budget, a state, what it printed)
 * and everything on the other side is the language's. The three lifecycle calls are the translation:
 * a machine says "another tick has come", and only here does that mean {@code OnTick}.
 */
final class SigmaProgram implements IMachineRuntime {

    /** What a program is allowed to spend on its farewell, out of nobody's tick. */
    private static final int FAREWELL = 4096;

    private static final Logger LOGGER = LogUtils.getLogger();

    private final Process process;

    SigmaProgram(final Process process) {
        this.process = process;
    }

    /** The program underneath, for the parts of this mod that know it is Σ#. */
    Process process() {
        return this.process;
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
        return switch (this.process.state()) {
            case RUNNING -> State.RUNNING;
            case PARKED -> State.PARKED;
            case FINISHED -> State.FINISHED;
            case HALTED -> State.HALTED;
        };
    }

    @Override
    public String message() {
        return this.process.message() == null ? "" : this.process.message();
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
        return this.process.heap().used();
    }

    @Override
    public long heapBytes() {
        return this.process.heap().budget();
    }

    /* A script stays up until it says it is done: one that called Program.Exit is over like any other. */
    @Override
    public boolean isService() {
        return this.process.shape() == Shape.SCRIPT && !this.process.exited();
    }

    @Override
    public void onTick() {
        final Values.Obj script = this.process.script();
        if (script != null && this.process.readyForTurn("OnTick")) {
            this.process.begin(script, "OnTick");
        }
    }

    @Override
    public void onStop(final int budget) {
        final Values.Obj script = this.process.script();
        if (script == null || this.process.state() == Process.State.HALTED) {
            return;
        }
        this.process.beginFirst(script, "OnDestroy");
        this.process.step(budget > 0 ? budget : FAREWELL);
    }

    @Override
    public List<String> watching() {
        return this.process.watching();
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
    public void deliver(final Map<String, Long> totals) {
        this.process.deliver(totals);
    }

    @Override
    public int exitCode() {
        return this.process.exitCode();
    }

    @Override
    public boolean deliverMessage(final int from, final String text, final long tick) {
        return this.process.deliverMessage(from, text, tick);
    }

    @Override
    public boolean deliverGatewayMessage(final int from, final String text, final long tick) {
        return this.process.deliverGatewayMessage(from, text, tick);
    }

    @Override
    public boolean deliverUiEvent(final long window, final long widget, final String kind,
                                  final List<Object> values) {
        return this.process.deliverUiEvent(window, widget, kind, values);
    }

    @Override
    public List<Values.Obj> windows() {
        return this.process.windows();
    }

    @Override
    public void save(final CompoundTag tag) {
        try {
            tag.put("Snapshot", SnapshotTag.write(this.process.save()));
        } catch (final SnapshotException unwritable) {
            /*
             * A program is never saved with a part of it silently empty: it is left out of the save instead, the log
             * says why, and it does not come back when the world loads.
             */
            LOGGER.error("The program '{}' could not be saved and will not come back after a load: {}",
                    this.process.name(), unwritable.getMessage());
        }
    }

    /** Where {@link #save} puts it, for the language to read back. */
    static CompoundTag snapshotOf(final CompoundTag tag) {
        return tag.getCompound("Snapshot");
    }
}
