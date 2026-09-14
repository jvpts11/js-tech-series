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
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * What the world says to a program, turned into calls waiting their turn.
 *
 * <p>A message from another program, a message through a Gateway, a player's click and a watched stock that changed
 * each become an object on the program's heap and a call to the handler the program gave, queued for the main thread.
 * Handlers run in the order they arrived, in the same slice as the work that was already there and out of the same
 * budget, so a process that fires a great many of them does not get more of the tick than one that fires none. A
 * delegate joined from several handlers queues each of them, in the order they were joined and with the same
 * arguments, so every listener hears the event; one whose method cannot be found is passed over and the rest still
 * run.
 *
 * <p>An event that finds no room for its calls, or for what their arguments hold, is dropped and counted: the widget
 * or the watch behind it already holds the latest value, so the next handler that runs reads what is true now. A
 * message that finds no room is refused instead, so its sender knows. Closing a window always gets in, ahead of the
 * rest.
 */
final class ProgramEvents {

    /** What an event handed to a handler holds before its text: a header and its three fields. */
    static final long EVENT_BYTES = Heap.HEADER + 3L * Heap.REFERENCE;

    private final Process process;
    private final Heap heap;
    private final ProgramImage program;
    private final Library library;
    private final CallbackQueue callbacks;
    private final ProgramListeners listeners;
    private final ProgramWindows windows;
    private final ProgramWatches watches;
    private final ProgramIdentity identity;

    ProgramEvents(final Process process) {
        this.process = process;
        this.heap = process.heap0();
        this.program = process.program();
        this.library = process.library();
        this.callbacks = process.callbacks();
        this.listeners = process.listeners();
        this.windows = process.windows0();
        this.watches = process.watches();
        this.identity = process.identity();
    }

    /** Puts a handler's calls in the queue with these arguments; false when the event found no room and was dropped. */
    boolean post(final Values.DelegateValue handler, final List<Object> arguments) {
        if (this.offer(handler, this.weigh(arguments), () -> arguments)) {
            return true;
        }
        this.callbacks.drop();
        return false;
    }

    /**
     * Hands the program what a ComputerCraft computer said through a Gateway.
     *
     * <p>It is queued for the handler the program gave {@code Gateway.OnMessage}. A program that gave none does not
     * hear it, and neither does one with too many calls already waiting to take it; false says so.
     */
    boolean deliverGatewayMessage(final int from, final String text, final long tick) {
        final Values.DelegateValue handler = this.listeners.onGatewayMessage();
        if (this.identity.over() || handler == null) {
            return false;
        }
        final String said = text == null ? "" : text;
        return this.offer(handler, EVENT_BYTES + Heap.sizeOfText(said),
                () -> List.of(this.gatewayMessageOf(from, said, tick)));
    }

    /**
     * Hands the program a line from another program.
     *
     * <p>It is queued for the handler the program gave {@code Program.OnMessage}; a program that gave none simply does
     * not hear it. False when the program is over, or when it has too many calls already waiting to take this one, so
     * the sender knows it was not heard.
     */
    boolean deliverMessage(final int from, final String text, final long tick) {
        if (this.identity.over()) {
            return false;
        }
        final String said = text == null ? "" : text;
        return this.offer(this.listeners.onMessage(), EVENT_BYTES + Heap.sizeOfText(said),
                () -> List.of(this.messageOf(from, said, tick)));
    }

    /**
     * Tells the program what a player did to one of its widgets.
     *
     * <p>What the widget holds is changed first, the way the player changed it (a box is ticked, a line is typed), and
     * only then is the program's handler queued: a handler that reads the widget reads what the player sees. A widget
     * with no handler still changes, and so does one whose handler finds no room among the calls waiting and is
     * dropped. Closing a window always gets in, ahead of the rest.
     */
    boolean deliverUiEvent(final long window, final long widget, final String kind, final List<Object> values) {
        if (this.identity.over()) {
            return false;
        }
        final Values.Obj open = this.windows.of(window);
        if (open == null) {
            return false;
        }
        if ("close".equals(kind)) {
            this.windows.close(open);
            this.ahead(handlerOf(open, "OnClose"));
            /*
             * Shutting the last window of a program is how a person ends it, as it is on any desktop. The
             * program hears it first, and one that opens another window in its OnClose carries on.
             */
            this.windows.closedByPerson();
            return true;
        }
        final Values.Obj found = UiWidgets.widgetOf(open, widget);
        if (found == null || !Boolean.TRUE.equals(found.get(UiWidgets.ENABLED))
                || !Boolean.TRUE.equals(found.get(UiWidgets.VISIBLE))) {
            return false;
        }
        final String handler;
        try {
            handler = this.library.ui().accept(found, kind, values);
        } catch (final Halt halt) {
            this.process.halt(halt);
            return false;
        }
        if (handler == null) {
            return false;
        }
        this.post(handlerOf(found, handler), List.of());
        return true;
    }

    /**
     * Hands in what the world now holds, and queues a call for every watch that was waiting for it.
     *
     * <p>A watch whose token the program has thrown away is dropped rather than fired: stopping is the program's to
     * decide, and it decided. A call that finds no room among the calls waiting is dropped and counted; the watch
     * already holds the new number, so the next one to fire reads it.
     */
    void deliver(final Map<String, Long> totals) {
        this.watches.deliver(totals, this.heap::isFreed, this::fired);
    }

    /** The program's method of that name bound to {@code self}, as a handler, or null when its type has none. */
    Values.DelegateValue handlerFor(final Values.Obj self, final String name) {
        final TypeImage type = this.program.type(self.type());
        for (final MethodImage method : type.methods().values()) {
            if (method.name().equals(name)) {
                return new Values.DelegateValue(self.type(), List.of(new Values.Bound(self,
                        method.owner(), method.name(), method.parameters(), method.returns())));
            }
        }
        return null;
    }

    /* What a call read back out of a save holds on the heap: what it was handed sits in its frame's first slots. */
    long weigh(final Frame call) {
        final int handed = Math.min(call.method.parameters().size(), call.slots.length);
        return this.weigh(Arrays.asList(call.slots).subList(0, handed));
    }

    private void fired(final ProgramWatches.Watch watch, final long before, final long now, final boolean first) {
        if (!this.offer(watch.handler(), EVENT_BYTES,
                () -> List.of(this.stockEvent(watch.item(), before, now, first)))) {
            this.callbacks.drop();
        }
    }

    private Values.Obj stockEvent(final String item, final long before, final long now, final boolean first) {
        final Values.Obj made = new Values.Obj("StockEvent");
        made.set("Item", item);
        made.set("Total", now);
        made.set("Previous", first ? now : before);
        this.heap.allocate(made, EVENT_BYTES, 0);
        return made;
    }

    /** What a Gateway message is as a value the program holds. */
    private Values.Obj gatewayMessageOf(final int from, final String text, final long tick) {
        final Values.Obj made = new Values.Obj("GatewayMessage");
        made.set("From", (long) from);
        made.set("Text", this.heap.text(text == null ? "" : text, 0));
        made.set("Tick", tick);
        this.heap.allocate(made, EVENT_BYTES, 0);
        return made;
    }

    private Values.Obj messageOf(final int from, final String text, final long tick) {
        final Values.Obj made = new Values.Obj("ProcessMessage");
        made.set("From", from);
        made.set("Text", this.heap.text(text == null ? "" : text, 0));
        made.set("Tick", tick);
        this.heap.allocate(made, EVENT_BYTES, 0);
        return made;
    }

    /**
     * Queues a handler's calls when there is room for them and for what their arguments hold, each call
     * counted with all of them, and only then builds the arguments, so a call turned away leaves nothing
     * behind on the heap. False when there was no room; a handler with nothing to call always fits.
     */
    private boolean offer(final Values.DelegateValue handler, final long bytes,
                          final Supplier<List<Object>> arguments) {
        final List<Frame> queued = this.callsOf(handler);
        if (queued.isEmpty()) {
            return true;
        }
        if (!this.callbacks.fits(queued.size(), bytes * queued.size())) {
            return false;
        }
        final List<Object> handed = arguments.get();
        for (final Frame call : queued) {
            CallDispatch.fill(call, handed);
            this.callbacks.add(call, bytes);
        }
        return true;
    }

    /** Puts a handler's calls ahead of everything waiting, in their own order; nothing turns these away. */
    private void ahead(final Values.DelegateValue handler) {
        final List<Frame> queued = this.callsOf(handler);
        for (int i = queued.size() - 1; i >= 0; i--) {
            this.callbacks.addFirst(queued.get(i), 0);
        }
    }

    /* One call for each method joined to the handler, in order, passing over any that cannot be found. */
    private List<Frame> callsOf(final Values.DelegateValue handler) {
        if (handler == null) {
            return List.of();
        }
        final List<Frame> made = new ArrayList<>(handler.chain().size());
        for (final Values.Bound bound : handler.chain()) {
            final MethodImage method =
                    this.program.method(bound.owner(), bound.method(), bound.parameters());
            if (method != null && method.hasCode()) {
                made.add(new Frame(method, bound.target()));
            }
        }
        return made;
    }

    /* What the arguments handed to a call hold on the heap: each one, and whatever its fields hold. */
    private long weigh(final List<Object> arguments) {
        long bytes = 0;
        for (final Object argument : arguments) {
            bytes += this.heap.bytesOf(argument);
            if (argument instanceof Values.Obj object) {
                for (final Object field : object.all().values()) {
                    bytes += this.heap.bytesOf(field);
                }
            }
        }
        return bytes;
    }

    private static Values.DelegateValue handlerOf(final Values.Obj widget, final String name) {
        return widget.get(name) instanceof Values.DelegateValue handler ? handler : null;
    }
}
