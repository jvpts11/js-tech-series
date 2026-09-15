/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writing a whole process down: what it has allocated, what each thread was doing, and where each of its calls had
 * got to.
 *
 * <p>Two names for one object come back as two names for one object, because everything still held, and anything
 * freed that something written still reaches, is written under a number and every reference is written as that
 * number.
 *
 * <p>Everything a program can hold is on its heap, so a thing it holds that is not has no number to be written under.
 * Writing stops there with a {@link SnapshotException} rather than writing it as nothing, which would bring the
 * program back quietly wrong.
 */
final class ProcessSnapshotWriter {

    private ProcessSnapshotWriter() {
    }

    /** The snapshot of the process as it stands. */
    static Snapshot write(final Process process) {
        final Heap heap = process.heap0();
        final HeldNumbers numbers = new HeldNumbers(heap);
        final List<Snapshot.ThreadShot> running = new ArrayList<>();
        for (final ProgramThread thread : process.scheduler().threads()) {
            running.add(freeze(thread, numbers));
        }
        final List<Snapshot.FrameShot> queued = new ArrayList<>();
        for (final Frame frame : process.callbacks()) {
            queued.add(freeze(frame, numbers));
        }
        final Map<String, Map<String, Snapshot.IValue>> kept = new LinkedHashMap<>();
        for (final Map.Entry<String, Values.Obj> entry : process.fieldAccess().statics().entrySet()) {
            kept.put(entry.getKey(), fields(entry.getValue(), numbers));
        }
        final List<Snapshot.WatchShot> watching = new ArrayList<>();
        for (final ProgramWatches.Watch watch : process.watches().all()) {
            watching.add(new Snapshot.WatchShot(watch.id(), watch.item(), watch.kind().serializedName(),
                    watch.threshold(), value(watch.handler(), numbers), value(watch.token(), numbers), watch.last(),
                    watch.armed(), watch.seen()));
        }
        final List<Snapshot.MonitorShot> locked = new ArrayList<>();
        process.locks().forEach((target, owner, count) ->
                locked.add(new Snapshot.MonitorShot(value(target, numbers), owner, count)));
        final ProgramListeners listeners = process.listeners();
        final ProgramWindows windows = process.windows0();
        final Snapshot.IValue scriptShot = value(process.script(), numbers);
        final Snapshot.IValue onMessageShot = value(listeners.onMessage(), numbers);
        final List<Snapshot.IValue> windowShots = values(windows.held(), numbers);
        final Snapshot.IValue onGatewayShot = value(listeners.onGatewayMessage(), numbers);
        /*
         * The objects are written last: writing anything else down can number a freed thing it still
         * reaches, and so can writing an object, so the numbers are walked while they grow.
         */
        final List<Snapshot.IHeld> held = new ArrayList<>();
        for (int number = 0; number < numbers.size(); number++) {
            held.add(freeze(heap, numbers.thing(number), number, numbers));
        }
        final ProgramIdentity identity = process.identity();
        return new Snapshot(Snapshot.FORMAT, process.program().checksum(),
                new Snapshot.HeapShot(heap.budget(), held),
                new Snapshot.IdentityShot(process.state().serializedName(),
                        identity.message() == null ? "" : identity.message(), identity.spent(), identity.name(),
                        identity.args(), identity.machineId(), identity.exited(), identity.givenExitCode()),
                new Snapshot.ConsoleShot(process.console(), process.written(), process.random().state()),
                process.input().lines(),
                new Snapshot.CallbacksShot(queued, process.callbacks().dropped()),
                new Snapshot.WindowsShot(windowShots, windows.nextWindow(), windows.nextWidget(),
                        windows.endWithWindows()),
                watching,
                new Snapshot.ListenersShot(onMessageShot, onGatewayShot, listeners.gateway()),
                new Snapshot.ThreadsShot(running, process.scheduler().nextId()),
                locked, kept, scriptShot);
    }

    private static Snapshot.IHeld freeze(final Heap heap, final Object thing, final int number,
                                         final HeldNumbers numbers) {
        final long bytes = heap.bytesOf(thing);
        final int line = heap.lineOf(thing);
        final boolean freed = heap.isFreed(thing);
        if (thing instanceof String text) {
            return new Snapshot.IHeld.Text(number, bytes, line, freed, text);
        }
        if (thing instanceof Values.Obj object) {
            return new Snapshot.IHeld.Object(number, bytes, line, freed, object.type(),
                    fields(object, numbers));
        }
        if (thing instanceof Values.Arr array) {
            return new Snapshot.IHeld.Array(number, bytes, line, freed, array.element(),
                    values(array.all(), numbers));
        }
        if (thing instanceof Values.ListValue list) {
            return new Snapshot.IHeld.Listing(number, bytes, line, freed, values(list.items(), numbers));
        }
        if (thing instanceof Values.MapValue map) {
            return new Snapshot.IHeld.Keyed(number, bytes, line, freed,
                    values(new ArrayList<>(map.entries().keySet()), numbers),
                    values(new ArrayList<>(map.entries().values()), numbers));
        }
        if (!(thing instanceof Values.DelegateValue delegate)) {
            throw new SnapshotException("the heap holds a value no snapshot writes ("
                    + thing.getClass().getSimpleName() + ")");
        }
        final List<Snapshot.BoundShot> chain = new ArrayList<>();
        for (final Values.Bound bound : delegate.chain()) {
            chain.add(new Snapshot.BoundShot(value(bound.target(), numbers), bound.owner(),
                    bound.method(), bound.parameters(), bound.returns()));
        }
        return new Snapshot.IHeld.Handler(number, bytes, line, freed, delegate.type(), chain);
    }

    /*
     * The frames are a stack, so they come out top first; they are written bottom first, which is the
     * order they have to be put back in.
     */
    private static Snapshot.ThreadShot freeze(final ProgramThread thread, final HeldNumbers numbers) {
        final List<Frame> stack = new ArrayList<>(thread.frames);
        Collections.reverse(stack);
        final List<Snapshot.FrameShot> frames = new ArrayList<>();
        for (final Frame frame : stack) {
            frames.add(freeze(frame, numbers));
        }
        return new Snapshot.ThreadShot(thread.id, frames, IWait.kindOf(thread.wait), IWait.untilOf(thread.wait),
                value(IWait.onOf(thread.wait), numbers), value(thread.token, numbers), thread.givenUp(),
                IWait.hostOf(thread.wait));
    }

    private static Snapshot.FrameShot freeze(final Frame frame, final HeldNumbers numbers) {
        return new Snapshot.FrameShot(frame.method.owner(), frame.method.name(),
                frame.method.parameters(), frame.at, value(frame.self, numbers),
                values(Arrays.asList(frame.slots), numbers), values(frame.stack, numbers),
                frame.discard);
    }

    private static Map<String, Snapshot.IValue> fields(final Values.Obj object, final HeldNumbers numbers) {
        final Map<String, Snapshot.IValue> written = new LinkedHashMap<>();
        for (final Map.Entry<String, Object> field : object.all().entrySet()) {
            written.put(field.getKey(), value(field.getValue(), numbers));
        }
        return written;
    }

    private static List<Snapshot.IValue> values(final List<Object> things, final HeldNumbers numbers) {
        final List<Snapshot.IValue> written = new ArrayList<>();
        for (final Object thing : things) {
            written.add(value(thing, numbers));
        }
        return written;
    }

    private static Snapshot.IValue value(final Object thing, final HeldNumbers numbers) {
        return switch (thing) {
            case null -> new Snapshot.IValue.Nothing();
            case Integer number -> new Snapshot.IValue.I4(number);
            case Long number -> new Snapshot.IValue.I8(number);
            case Float number -> new Snapshot.IValue.R4(number);
            case Double number -> new Snapshot.IValue.R8(number);
            case Boolean flag -> new Snapshot.IValue.Bool(flag);
            case Character letter -> new Snapshot.IValue.Ch(letter);
            default -> {
                final Integer number = numbers.numberOf(thing);
                if (number == null) {
                    throw new SnapshotException("the program holds a value that is not on its heap ("
                            + thing.getClass().getSimpleName() + ")");
                }
                yield new Snapshot.IValue.Ref(number);
            }
        };
    }
}
