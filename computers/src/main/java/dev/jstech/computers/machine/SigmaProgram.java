/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import com.mojang.logging.LogUtils;
import dev.jstech.computers.hardware.IsaFit;
import dev.jstech.computers.vm.listing.Shape;
import dev.jstech.computers.vm.program.InputSent;
import dev.jstech.computers.vm.program.Process;
import dev.jstech.computers.vm.program.SettledOperation;
import dev.jstech.computers.vm.program.SnapshotException;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.core.text.Text;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * One running Σ# program, as a machine sees it.
 *
 * <p>Everything on this side of it is in the machine's words (a budget, a state, what it printed)
 * and everything on the other side is the language's. The three lifecycle calls are the translation:
 * a machine says "another tick has come", and only here does that mean {@code OnTick}.
 */
final class SigmaProgram implements IMachineRuntime {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final Process process;
    /* How far it gets on what it is dealt when it runs through a translator; null when it runs as it is. */
    @Nullable
    private final TranslatedPace pace;

    SigmaProgram(final Process process) {
        this(process, IsaFit.FULL_SPEED);
    }

    /** A program running at {@code speedPercent} hundredths of its own speed, through a translator below 100. */
    SigmaProgram(final Process process, final int speedPercent) {
        this.process = process;
        this.pace = speedPercent >= IsaFit.FULL_SPEED ? null : new TranslatedPace(speedPercent);
    }

    /** The program underneath, for the parts of this mod that know it is Σ#. */
    Process process() {
        return this.process;
    }

    @Override
    public int step(final int budget) {
        return this.paced(budget);
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
    public Text message() {
        return this.process.message();
    }

    @Override
    public List<String> console() {
        return this.process.console();
    }

    @Override
    public List<Text> consoleText() {
        return this.process.consoleText();
    }

    @Override
    public long written() {
        return this.process.written();
    }

    @Override
    public String openLine() {
        return this.process.openLine();
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

    @Override
    public boolean debugged() {
        return this.process.debugger() != null;
    }

    /*
     * A script stays up until it says it is done: one that called Program.Exit is over like any other. A player's
     * operating system stays up the same way, for as long as the machine runs it.
     */
    @Override
    public boolean isService() {
        return (this.process.shape() == Shape.SCRIPT || this.process.shape() == Shape.SYSTEM)
                && !this.process.exited();
    }

    /** Hands a player's operating system what came from the monitor; false when it was not taken. */
    public boolean deliverInput(final InputSent sent) {
        return this.process.deliverInput(sent);
    }

    @Override
    public boolean gaveUpTurn() {
        return this.process.takeGaveUpTurn();
    }

    /** Whether the program's own object has that method, taking that many arguments, for a question to ask it. */
    boolean answers(final String method, final int arguments) {
        final Values.Obj script = this.process.script();
        return script != null && this.process.answers(script, method, arguments);
    }

    /** Whether it is running instructions now, so it cannot be asked anything until it stops. */
    boolean busy() {
        return this.process.busy();
    }

    /** Asks the program's own object that question at once, for at most {@code budget} instructions. */
    Process.Answer answer(final String method, final List<Object> arguments, final int budget) {
        final Values.Obj script = this.process.script();
        return script == null ? Process.Answer.NONE : this.process.answer(script, method, arguments, budget);
    }

    @Override
    public void onTick() {
        final Values.Obj script = this.process.script();
        if (script != null && this.process.declares(script, "OnTick") && this.process.readyForTurn("OnTick")) {
            this.process.begin(script, "OnTick");
        }
    }

    @Override
    public void onStop(final int budget) {
        this.farewell(budget);
    }

    @Override
    public int farewell(final int budget) {
        if (budget <= 0) {
            return 0;
        }
        // The finally blocks it is inside run first, out of the same budget, and then it is told it is going.
        final int unwound = this.process.stopping() ? this.paced(budget) : 0;
        final Values.Obj script = this.process.script();
        // A system is told the machine is going down; a script, that it is being stopped.
        final String told = this.process.shape() == Shape.SYSTEM ? "OnShutdown" : "OnDestroy";
        if (budget - unwound <= 0 || script == null || this.process.state() == Process.State.HALTED
                || !this.process.declares(script, told)) {
            return unwound;
        }
        this.process.beginFirst(script, told);
        return unwound + this.paced(budget - unwound);
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
    public boolean listensToNetwork() {
        return this.process.listensToNetwork();
    }

    @Override
    public boolean deliverSettled(final SettledOperation operation) {
        return this.process.deliverSettled(operation);
    }

    @Override
    public boolean deliverNetworkEvent(final String level, final String machine, final String text,
                                       final long tick) {
        return this.process.deliverNetworkEvent(level, machine, text, tick);
    }

    @Override
    public void programEnded(final int program) {
        this.process.programEnded(program);
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

    /* Runs the program on a grant of {@code budget}, through its translator's pace when it has one. */
    private int paced(final int budget) {
        if (this.pace == null) {
            return this.process.step(budget);
        }
        final int share = this.pace.share(budget);
        final int used = share == 0 ? 0 : this.process.step(share);
        return this.pace.charged(budget, share, used);
    }
}
