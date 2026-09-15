/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reading a process back out of its snapshot, ready to carry on where it stopped.
 *
 * <p>Every thing it had allocated is made first as an empty shell under its number, so two things that point at each
 * other can both be filled in afterwards; then its threads, locks, waiting calls, fields and the rest are put back
 * pointing at those shells.
 *
 * <p>Reading is strict. A snapshot of another format or taken from another listing, a number written down twice, a
 * reference to nothing written down, a call in a method the listing does not have or past its end, and a wait, watch
 * or state no snapshot writes each stop the reading with a {@link SnapshotException}: a program brought back from a
 * save it does not match would carry on wrong, and nothing would say so.
 */
final class ProcessSnapshotReader {

    private ProcessSnapshotReader() {
    }

    /** The process a snapshot wrote, for the program it was written from. */
    static Process read(final ProgramImage program, final Snapshot shot, final IHost host) {
        if (shot.format() != Snapshot.FORMAT) {
            throw new SnapshotException("the snapshot is of format " + shot.format() + ", and only format "
                    + Snapshot.FORMAT + " is read");
        }
        if (!shot.listing().equals(program.checksum())) {
            throw new SnapshotException("the snapshot was taken from another listing");
        }
        final Snapshot.HeapShot heapShot = shot.heap();
        final Snapshot.IdentityShot identityShot = shot.identity();
        final boolean halted = stateOf(identityShot.state()) == Process.State.HALTED;
        final Process process = new Process(program, heapShot.budget(), host, false);
        final ProgramIdentity identity = process.identity();
        identity.rename(identityShot.name());
        final Map<Integer, Object> byNumber = new LinkedHashMap<>();
        for (final Snapshot.IHeld written : heapShot.held()) {
            if (byNumber.put(written.id(), shell(written)) != null) {
                throw new SnapshotException("the number " + written.id() + " is written down twice");
            }
        }
        /*
         * Handlers are settled before anything is filled in, because one cannot be changed after it is
         * made and whatever points at one has to point at the one that stays.
         */
        for (final Snapshot.IHeld written : heapShot.held()) {
            if (written instanceof Snapshot.IHeld.Handler handler) {
                final List<Values.Bound> chain = new ArrayList<>();
                for (final Snapshot.BoundShot bound : handler.chain()) {
                    chain.add(new Values.Bound(value(bound.target(), byNumber), bound.owner(),
                            bound.method(), bound.parameters(), bound.returns()));
                }
                byNumber.put(handler.id(), new Values.DelegateValue(handler.type(), chain));
            }
        }
        final Heap heap = process.heap0();
        for (final Snapshot.IHeld written : heapShot.held()) {
            fill(written, byNumber);
            heap.restore(byNumber.get(written.id()), written.bytes(), written.line(), written.freed());
        }
        final ThreadScheduler scheduler = process.scheduler();
        for (final Snapshot.ThreadShot written : shot.threads().running()) {
            final ProgramThread thread = scheduler.restore(written.id());
            for (final Snapshot.FrameShot each : written.frames()) {
                thread.frames.push(thaw(program, each, byNumber));
            }
            thread.wait = waitOf(written, byNumber);
            thread.restoreGivenUp(written.timedOut());
            if (value(written.token(), byNumber) instanceof Values.Obj token) {
                thread.token = token;
            }
        }
        scheduler.startFrom(shot.threads().nextThread());
        final MonitorTable locks = process.locks();
        for (final Snapshot.MonitorShot written : shot.monitors()) {
            final Object target = value(written.target(), byNumber);
            if (target == null) {
                throw new SnapshotException("a lock is held on nothing");
            }
            locks.restore(target, written.owner(), written.count());
        }
        // The threads came back before the locks, so a thread waiting for a lock is queued for it only now.
        locks.requeue(scheduler.threads());
        final CallbackQueue callbacks = process.callbacks();
        for (final Snapshot.FrameShot written : shot.callbacks().waiting()) {
            final Frame frame = thaw(program, written, byNumber);
            callbacks.add(frame, process.events().weigh(frame));
        }
        for (final Map.Entry<String, Map<String, Snapshot.IValue>> entry : shot.statics().entrySet()) {
            final Values.Obj holder = process.fieldAccess().statics(entry.getKey());
            for (final Map.Entry<String, Snapshot.IValue> field : entry.getValue().entrySet()) {
                holder.set(field.getKey(), value(field.getValue(), byNumber));
            }
        }
        if (value(shot.script(), byNumber) instanceof Values.Obj script) {
            process.restoreScript(script);
        }
        for (final Snapshot.WatchShot written : shot.watches()) {
            if (!(value(written.handler(), byNumber) instanceof Values.DelegateValue handler)) {
                throw new SnapshotException("a watch on " + written.item() + " has no handler");
            }
            if (!(value(written.token(), byNumber) instanceof Values.Obj token)) {
                throw new SnapshotException("a watch on " + written.item() + " has no token");
            }
            try {
                process.watches().restore(written, handler, token);
            } catch (final IllegalArgumentException unknown) {
                throw new SnapshotException(unknown.getMessage());
            }
        }
        final Snapshot.ConsoleShot console = shot.console();
        process.console0().restore(console.lines(), console.written());
        process.random().startFrom(console.random());
        process.input().restore(shot.input());
        callbacks.startFrom(shot.callbacks().dropped());
        identity.restore(identityShot.args(), identityShot.machineId(), identityShot.spent(), identityShot.exited(),
                identityShot.exitCode(), halted, identityShot.message().isEmpty() ? null : identityShot.message());
        // The windows the program had open come back open, with everything they were showing.
        final Snapshot.WindowsShot windowsShot = shot.windows();
        final ProgramWindows windows = process.windows0();
        for (final Snapshot.IValue written : windowsShot.open()) {
            if (!(value(written, byNumber) instanceof Values.Obj window)) {
                throw new SnapshotException("an open window is not an object");
            }
            windows.restoreOpen(window);
        }
        windows.startFrom(windowsShot.nextWindow(), windowsShot.nextWidget());
        windows.restoreEnding(windowsShot.endWithWindows());
        final Snapshot.ListenersShot listeners = shot.listeners();
        process.listeners().restore(
                value(listeners.onMessage(), byNumber) instanceof Values.DelegateValue handler ? handler : null,
                value(listeners.onGatewayMessage(), byNumber) instanceof Values.DelegateValue listening
                        ? listening : null,
                listeners.gateway());
        return process;
    }

    /** The state a snapshot names, by the name each state is written under. */
    private static Process.State stateOf(final String written) {
        for (final Process.State state : Process.State.values()) {
            if (state.serializedName().equals(written)) {
                return state;
            }
        }
        throw new SnapshotException("no process state is named '" + written + "'");
    }

    /** What a thread was waiting for, refusing a wait no snapshot writes. */
    private static IWait waitOf(final Snapshot.ThreadShot written, final Map<Integer, Object> byNumber) {
        try {
            return IWait.read(written.parked(), written.until(), value(written.on(), byNumber), written.onHost());
        } catch (final IllegalArgumentException unknown) {
            throw new SnapshotException(unknown.getMessage());
        }
    }

    private static Object shell(final Snapshot.IHeld written) {
        return switch (written) {
            case Snapshot.IHeld.Text text -> new String(text.value().toCharArray());
            case Snapshot.IHeld.Object object -> new Values.Obj(object.type());
            case Snapshot.IHeld.Array array -> new Values.Arr(array.element(), array.values().size());
            case Snapshot.IHeld.Listing ignored -> new Values.ListValue();
            case Snapshot.IHeld.Keyed ignored -> new Values.MapValue();
            case Snapshot.IHeld.Handler handler -> new Values.DelegateValue(handler.type(), List.of());
        };
    }

    /*
     * A later pass, because two things can point at each other and neither can be filled in until both
     * exist.
     */
    private static void fill(final Snapshot.IHeld written, final Map<Integer, Object> byNumber) {
        final Object thing = byNumber.get(written.id());
        switch (written) {
            case Snapshot.IHeld.Object object -> {
                for (final Map.Entry<String, Snapshot.IValue> field : object.fields().entrySet()) {
                    ((Values.Obj) thing).set(field.getKey(), value(field.getValue(), byNumber));
                }
            }
            case Snapshot.IHeld.Array array -> {
                for (int i = 0; i < array.values().size(); i++) {
                    ((Values.Arr) thing).set(i, value(array.values().get(i), byNumber), 0);
                }
            }
            case Snapshot.IHeld.Listing list -> {
                for (final Snapshot.IValue item : list.items()) {
                    ((Values.ListValue) thing).items().add(value(item, byNumber));
                }
            }
            case Snapshot.IHeld.Keyed keyed -> {
                for (int i = 0; i < keyed.keys().size(); i++) {
                    ((Values.MapValue) thing).entries().put(value(keyed.keys().get(i), byNumber),
                            value(keyed.values().get(i), byNumber));
                }
            }
            default -> { }
        }
    }

    /** A call in progress put back, refusing one in a method the listing does not have or standing past its end. */
    private static Frame thaw(final ProgramImage program, final Snapshot.FrameShot written,
                              final Map<Integer, Object> byNumber) {
        final MethodImage method = found(program, written);
        if (method == null) {
            throw new SnapshotException("a call in " + written.owner() + "." + written.name()
                    + " has no method in this listing");
        }
        if (written.at() < 0 || written.at() > method.length() || written.slots().size() != method.slots()) {
            throw new SnapshotException("a call in " + written.owner() + "." + written.name()
                    + " does not fit its method");
        }
        final Frame frame = new Frame(method, value(written.self(), byNumber));
        for (int i = 0; i < written.slots().size(); i++) {
            frame.slots[i] = value(written.slots().get(i), byNumber);
        }
        for (final Snapshot.IValue held : written.stack()) {
            frame.push(value(held, byNumber));
        }
        frame.at = written.at();
        frame.discard = written.discard();
        return frame;
    }

    /**
     * The method a frame was in.
     *
     * <p>The one that puts a type's own starting values in place is not among the methods that can be
     * called by name, so it is asked for separately: a process put away before it ran would otherwise
     * come back without it.
     */
    private static MethodImage found(final ProgramImage program, final Snapshot.FrameShot written) {
        final MethodImage named =
                program.method(written.owner(), written.name(), written.parameters());
        if (named != null) {
            return named;
        }
        final TypeImage type = program.type(written.owner());
        if (type == null || type.setUp() == null) {
            return null;
        }
        final MethodImage setUp = type.setUp();
        return setUp.name().equals(written.name()) && setUp.parameters().equals(written.parameters())
                ? setUp : null;
    }

    private static Object value(final Snapshot.IValue written, final Map<Integer, Object> byNumber) {
        return switch (written) {
            case Snapshot.IValue.Nothing ignored -> null;
            case Snapshot.IValue.I4 number -> number.value();
            case Snapshot.IValue.I8 number -> number.value();
            case Snapshot.IValue.R4 number -> number.value();
            case Snapshot.IValue.R8 number -> number.value();
            case Snapshot.IValue.Bool flag -> flag.value();
            case Snapshot.IValue.Ch letter -> letter.value();
            case Snapshot.IValue.Ref reference -> {
                final Object found = byNumber.get(reference.id());
                if (found == null) {
                    throw new SnapshotException("a reference to the number " + reference.id()
                            + ", under which nothing is written down");
                }
                yield found;
            }
        };
    }
}
