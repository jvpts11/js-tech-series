/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import java.util.List;
import java.util.Map;

/**
 * A running program, frozen.
 *
 * <p>A process that stopped because the world was put away should carry on where it left off when the
 * world comes back, which means everything it was holding has to be written down: what it had
 * allocated, what each frame was doing, and where each of them was in its method. It is written in the
 * same parts the process keeps itself in: its heap, who it is, its console, the lines typed ahead, the
 * calls waiting their turn, its windows, its watches, who it tells when something is said to it, its
 * threads, the locks they hold, the fields its types keep, and the object its script runs on.
 *
 * <p>Everything here is plain data with no Minecraft in it, so freezing and thawing can be tested on
 * its own, and whatever writes it to a save file is a thin layer over records that already hold the
 * whole truth. References between allocated things become numbers, because two objects can point at
 * each other and a tree cannot say that.
 *
 * <p>A snapshot names the format it is written in and the listing it was taken from, by a checksum of that
 * listing's text. A save of any other format is not read back, and neither is one taken from a listing that has
 * changed since, because its calls in progress would point into a program that is no longer there.
 */
public record Snapshot(int format, String listing, HeapShot heap, IdentityShot identity, ConsoleShot console,
                       List<String> input, CallbacksShot callbacks, WindowsShot windows, List<WatchShot> watches,
                       ListenersShot listeners, ThreadsShot threads, List<MonitorShot> monitors,
                       Map<String, Map<String, IValue>> statics, IValue script) {

    /** The format this runtime writes, and the only one it reads back: an older save is never migrated. */
    public static final int FORMAT = 1;

    public Snapshot {
        listing = listing == null ? "" : listing;
        input = input == null ? List.of() : List.copyOf(input);
        watches = List.copyOf(watches);
        monitors = List.copyOf(monitors);
        statics = Map.copyOf(statics);
    }

    /** What the program had allocated, and the most it may allocate. */
    public record HeapShot(long budget, List<IHeld> held) {

        public HeapShot {
            held = List.copyOf(held);
        }
    }

    /** Who the program is, what it has spent, and how it ended if it did. */
    public record IdentityShot(String state, String message, long spent, String name, List<String> args,
                               int machineId, boolean exited, int exitCode) {

        public IdentityShot {
            name = name == null ? "" : name;
            args = args == null ? List.of() : List.copyOf(args);
        }
    }

    /** What the program printed, how much it has ever written, and where its random numbers stand. */
    public record ConsoleShot(List<String> lines, long written, long random) {

        public ConsoleShot {
            lines = List.copyOf(lines);
        }
    }

    /** The calls waiting their turn, and how many found no room among them. */
    public record CallbacksShot(List<FrameShot> waiting, long dropped) {

        public CallbacksShot {
            waiting = List.copyOf(waiting);
        }
    }

    /** The windows the program has open, the numbers the next window and widget get, and whether it ends with them. */
    public record WindowsShot(List<IValue> open, long nextWindow, long nextWidget, boolean endWithWindows) {

        public WindowsShot {
            open = open == null ? List.of() : List.copyOf(open);
        }
    }

    /** Who the program tells when something is said to it, and the Gateway it chose to reach through. */
    public record ListenersShot(IValue onMessage, IValue onGatewayMessage, String gateway) {

        public ListenersShot {
            onMessage = onMessage == null ? new IValue.Nothing() : onMessage;
            onGatewayMessage = onGatewayMessage == null ? new IValue.Nothing() : onGatewayMessage;
            gateway = gateway == null ? "" : gateway;
        }
    }

    /** The program's threads, and the number the next one it starts gets. */
    public record ThreadsShot(List<ThreadShot> running, int nextThread) {

        public ThreadsShot {
            running = List.copyOf(running);
        }
    }

    /**
     * One thread: its calls in progress, bottom first, and what it was waiting for if anything.
     *
     * <p>{@code on} is the object whose lock it waits for, or the number of the thread it is joined to;
     * {@code until} the tick a sleep ends or a timed join gives up on, or zero for never.
     */
    public record ThreadShot(int id, List<FrameShot> frames, String parked, long until, IValue on,
                             IValue token, boolean timedOut, String onHost) {

        public ThreadShot {
            frames = List.copyOf(frames);
            onHost = onHost == null ? "" : onHost;
        }
    }

    /** One lock held: the object, the thread holding it, and how many times over it took it. */
    public record MonitorShot(IValue target, int owner, int count) {
    }

    /**
     * One thing the program asked to be told about.
     *
     * <p>{@code last} and {@code armed} travel with it, because a watch that fires on a crossing has to
     * remember which side of the number it was on. Without them, a world that came back would tell a
     * program the iron had just run low when it had been low for a week.
     */
    public record WatchShot(int id, String item, String kind, long threshold, IValue handler, IValue token,
                            long last, boolean armed, boolean seen) {
    }

    /**
     * One value, as a slot or a stack holds it.
     *
     * <p>A number is written as itself; anything allocated is written as the number of the thing it
     * points at, so two names for one object come back as two names for one object.
     */
    public sealed interface IValue {

        /** Nothing at all. */
        record Nothing() implements IValue {
        }

        /** A whole number of four bytes, which is also how a bool and a character travel. */
        record I4(int value) implements IValue {
        }

        /** A whole number of eight bytes. */
        record I8(long value) implements IValue {
        }

        /** A real of four bytes. */
        record R4(float value) implements IValue {
        }

        /** A real of eight bytes. */
        record R8(double value) implements IValue {
        }

        /** True or false. */
        record Bool(boolean value) implements IValue {
        }

        /** One character. */
        record Ch(char value) implements IValue {
        }

        /** Something on the heap, by the number it was written down under. */
        record Ref(int id) implements IValue {
        }
    }

    /** One method bound to what it belongs to, as a delegate holds it. */
    public record BoundShot(IValue target, String owner, String method, List<String> parameters,
                            String returns) {

        public BoundShot {
            parameters = List.copyOf(parameters);
        }
    }

    /** One thing the program had allocated, with what it costs and where it was made. */
    public sealed interface IHeld {

        /** The number this thing is written down under. */
        int id();

        /** What it costs. */
        long bytes();

        /** The line of the assembly it was made on. */
        int line();

        /** Whether the program has already freed it. */
        boolean freed();

        /** A piece of text. */
        record Text(int id, long bytes, int line, boolean freed, String value) implements IHeld {
        }

        /** An instance of a class, with what each of its fields holds. */
        record Object(int id, long bytes, int line, boolean freed, String type,
                      Map<String, IValue> fields) implements IHeld {

            public Object {
                fields = Map.copyOf(fields);
            }
        }

        /** A fixed run of values. */
        record Array(int id, long bytes, int line, boolean freed, String element, List<IValue> values)
                implements IHeld {

            public Array {
                values = List.copyOf(values);
            }
        }

        /** A run of values that grows. */
        record Listing(int id, long bytes, int line, boolean freed, List<IValue> items) implements IHeld {

            public Listing {
                items = List.copyOf(items);
            }
        }

        /** Values reached by a key, written as two runs that line up. */
        record Keyed(int id, long bytes, int line, boolean freed, List<IValue> keys, List<IValue> values)
                implements IHeld {

            public Keyed {
                keys = List.copyOf(keys);
                values = List.copyOf(values);
            }
        }

        /** A handler, or a run of them. */
        record Handler(int id, long bytes, int line, boolean freed, String type, List<BoundShot> chain)
                implements IHeld {

            public Handler {
                chain = List.copyOf(chain);
            }
        }
    }

    /** One call in progress: which method, how far into it, and everything it was holding. */
    public record FrameShot(String owner, String name, List<String> parameters, int at, IValue self,
                            List<IValue> slots, List<IValue> stack, boolean discard) {

        public FrameShot {
            parameters = List.copyOf(parameters);
            slots = List.copyOf(slots);
            stack = List.copyOf(stack);
        }
    }
}
