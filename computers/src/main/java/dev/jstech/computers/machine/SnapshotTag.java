/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.vm.program.Snapshot;
import dev.jstech.computers.vm.program.SnapshotException;
import dev.jstech.core.text.LongText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextTags;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * A frozen process, written to a save and read back.
 *
 * <p>The freezing itself is done elsewhere and knows nothing about the game; this is only the writing
 * down, so the part worth testing on its own can be, and the part that has to know about tags stays
 * small enough to read in one sitting.
 *
 * <p>Every kind is written with its name beside it rather than guessed from the shape of what was
 * stored, because a whole number and a truth value look alike once written and a process that came
 * back with one in place of the other would be wrong in a way nothing would catch.
 *
 * <p>Reading is strict for the same reason: a save of another format, or holding a kind no snapshot writes, is
 * refused with a {@link SnapshotException} rather than read as something else.
 */
public final class SnapshotTag {

    private static final String KIND = "kind";
    private static final String VALUE = "value";
    private static final String ID = "id";
    private static final String BYTES = "bytes";
    private static final String LINE = "line";
    private static final String FREED = "freed";
    private static final String TYPE = "type";
    private static final String NAME = "name";
    private static final String OWNER = "owner";
    private static final String PARAMETERS = "parameters";
    private static final String FIELDS = "fields";
    private static final String VALUES = "values";
    private static final String KEYS = "keys";
    private static final String ITEMS = "items";
    private static final String CHAIN = "chain";
    private static final String TARGET = "target";
    private static final String METHOD = "method";
    private static final String RETURNS = "returns";
    private static final String SLOTS = "slots";
    private static final String STACK = "stack";
    private static final String SELF = "self";
    private static final String AT = "at";
    private static final String DISCARD = "discard";
    private static final String HELD = "held";
    private static final String FRAMES = "frames";
    private static final String WAITING = "waiting";
    private static final String STATICS = "statics";
    /* What INTERCAL's words left switched: absent from a save written before them, which then has nothing switched. */
    private static final String IGNORED = "ignored";
    private static final String SWITCHED_OFF = "switched_off";
    /* What the finally blocks running in a call go on with: absent from a save written before exceptions. */
    private static final String PENDINGS = "pendings";
    private static final String LEAVING = "leaving";
    private static final String FINALLIES = "finallies";
    private static final String NEXT = "next";
    private static final String EXCEPTION = "exception";
    private static final String RUNNING = "running";
    private static final String SCRIPT = "script";
    private static final String CONSOLE = "console";
    private static final String CONSOLE_OPEN = "console_open";
    private static final String WRITTEN = "written";
    private static final String RANDOM = "random";
    private static final String INPUT = "input";
    private static final String DROPPED = "dropped";
    private static final String WATCHES = "watches";
    private static final String THRESHOLD = "threshold";
    private static final String LAST = "last";
    private static final String ARMED = "armed";
    private static final String SEEN = "seen";
    private static final String STATE = "state";
    private static final String MESSAGE = "message";
    private static final String SPENT = "spent";
    private static final String BUDGET = "budget";
    private static final String PROGRAM_NAME = "programName";
    private static final String THREADS = "threads";
    private static final String PARKED = "parked";
    private static final String UNTIL = "until";
    private static final String ON = "on";
    private static final String TOKEN = "token";
    private static final String TIMED_OUT = "timedOut";
    private static final String ON_HOST = "onHost";
    private static final String MONITORS = "monitors";
    private static final String COUNT = "count";
    private static final String NEXT_THREAD = "nextThread";
    private static final String ARGS = "args";
    private static final String MACHINE_ID = "machineId";
    private static final String EXITED = "exited";
    private static final String EXIT_CODE = "exitCode";
    private static final String ON_MESSAGE = "onMessage";
    private static final String WINDOWS = "windows";
    private static final String ON_GATEWAY_MESSAGE = "onGatewayMessage";
    private static final String GATEWAY = "gateway";
    private static final String ON_SETTLED = "onSettled";
    private static final String SETTLED_FOR = "settledFor";
    private static final String OPERATION = "operation";
    private static final String HANDLER = "handler";
    private static final String ON_EVENT = "onEvent";
    private static final String CHANNELS = "channels";
    private static final String NEXT_WINDOW = "nextWindow";
    private static final String NEXT_WIDGET = "nextWidget";
    private static final String END_WITH_WINDOWS = "endWithWindows";
    private static final String FORMAT = "format";
    private static final String LISTING = "listing";
    private static final String ADDRESS = "address";
    private static final String CONTENTS = "contents";
    private static final String ON_STACK = "onStack";
    private static final String BLOCKS = "blocks";
    private static final String NEXT_ADDRESS = "nextAddress";

    private SnapshotTag() {
    }

    /** Writes a frozen process down. */
    public static CompoundTag write(final Snapshot shot) {
        final CompoundTag tag = new CompoundTag();
        tag.putInt(FORMAT, shot.format());
        LongText.put(tag, LISTING, shot.listing());
        tag.putLong(BUDGET, shot.heap().budget());
        if (shot.heap().nextAddress() != 0) {
            tag.putLong(NEXT_ADDRESS, shot.heap().nextAddress());
        }
        final ListTag held = new ListTag();
        for (final Snapshot.IHeld one : shot.heap().held()) {
            held.add(write(one));
        }
        tag.put(HELD, held);
        final ListTag threads = new ListTag();
        for (final Snapshot.ThreadShot thread : shot.threads().running()) {
            final CompoundTag each = new CompoundTag();
            each.putInt(ID, thread.id());
            each.put(FRAMES, frames(thread.frames()));
            each.putString(PARKED, thread.parked());
            each.putLong(UNTIL, thread.until());
            each.put(ON, write(thread.on()));
            each.put(TOKEN, write(thread.token()));
            each.putBoolean(TIMED_OUT, thread.timedOut());
            each.putString(ON_HOST, thread.onHost());
            threads.add(each);
        }
        tag.put(THREADS, threads);
        final ListTag monitors = new ListTag();
        for (final Snapshot.MonitorShot monitor : shot.monitors()) {
            final CompoundTag each = new CompoundTag();
            each.put(TARGET, write(monitor.target()));
            each.putInt(OWNER, monitor.owner());
            each.putInt(COUNT, monitor.count());
            monitors.add(each);
        }
        tag.put(MONITORS, monitors);
        tag.putInt(NEXT_THREAD, shot.threads().nextThread());
        final Snapshot.IdentityShot identity = shot.identity();
        tag.put(ARGS, names(identity.args()));
        tag.putInt(MACHINE_ID, identity.machineId());
        tag.putBoolean(EXITED, identity.exited());
        tag.putInt(EXIT_CODE, identity.exitCode());
        tag.put(ON_MESSAGE, write(shot.listeners().onMessage()));
        tag.put(WINDOWS, values(shot.windows().open()));
        tag.put(ON_GATEWAY_MESSAGE, write(shot.listeners().onGatewayMessage()));
        tag.putString(GATEWAY, shot.listeners().gateway());
        tag.put(ON_SETTLED, write(shot.listeners().onSettled()));
        tag.put(SETTLED_FOR, writeHandlers(shot.listeners().settledFor()));
        tag.put(ON_EVENT, write(shot.listeners().onEvent()));
        tag.put(CHANNELS, writeHandlers(shot.listeners().channels()));
        tag.putLong(NEXT_WINDOW, shot.windows().nextWindow());
        tag.putLong(NEXT_WIDGET, shot.windows().nextWidget());
        tag.putBoolean(END_WITH_WINDOWS, shot.windows().endWithWindows());
        tag.put(WAITING, frames(shot.callbacks().waiting()));
        final ListTag statics = new ListTag();
        for (final Map.Entry<String, Map<String, Snapshot.IValue>> entry : shot.statics().entrySet()) {
            final CompoundTag owner = new CompoundTag();
            owner.putString(OWNER, entry.getKey());
            owner.put(FIELDS, fields(entry.getValue()));
            owner.put(IGNORED, names(shot.intercal().staticsIgnored().getOrDefault(entry.getKey(), List.of())));
            statics.add(owner);
        }
        tag.put(STATICS, statics);
        tag.put(SWITCHED_OFF, names(shot.intercal().switchedOff()));
        tag.put(SCRIPT, write(shot.script()));
        final ListTag watches = new ListTag();
        for (final Snapshot.WatchShot watch : shot.watches()) {
            final CompoundTag each = new CompoundTag();
            each.putInt(ID, watch.id());
            each.putString(NAME, watch.item());
            each.putString(KIND, watch.kind());
            each.putLong(THRESHOLD, watch.threshold());
            each.put(METHOD, write(watch.handler()));
            each.put(TARGET, write(watch.token()));
            each.putLong(LAST, watch.last());
            each.putBoolean(ARMED, watch.armed());
            each.putBoolean(SEEN, watch.seen());
            watches.add(each);
        }
        tag.put(WATCHES, watches);
        // Lines kept as sentences, so what the runtime said of a halt reads in its player's words after a reload too.
        tag.put(CONSOLE, TextTags.writeAll(shot.console().lines()));
        LongText.put(tag, CONSOLE_OPEN, shot.console().open());
        tag.putLong(WRITTEN, shot.console().written());
        tag.putLong(RANDOM, shot.console().random());
        tag.put(INPUT, names(shot.input()));
        tag.putLong(DROPPED, shot.callbacks().dropped());
        tag.putString(STATE, identity.state());
        tag.put(MESSAGE, TextTags.write(identity.message()));
        tag.putLong(SPENT, identity.spent());
        if (!identity.name().isEmpty()) {
            tag.putString(PROGRAM_NAME, identity.name());
        }
        return tag;
    }

    /** Reads one back, refusing a save of another format or one holding a kind no snapshot writes. */
    public static Snapshot read(final CompoundTag tag) {
        final int format = tag.getInt(FORMAT);
        if (format != Snapshot.FORMAT) {
            throw new SnapshotException("the saved program is of format " + format + ", and only format "
                    + Snapshot.FORMAT + " is read");
        }
        final List<Snapshot.IHeld> held = new ArrayList<>();
        final ListTag written = tag.getList(HELD, Tag.TAG_COMPOUND);
        for (int i = 0; i < written.size(); i++) {
            held.add(readHeld(written.getCompound(i)));
        }
        final Map<String, Map<String, Snapshot.IValue>> statics = new LinkedHashMap<>();
        final Map<String, List<String>> staticsIgnored = new LinkedHashMap<>();
        final ListTag owners = tag.getList(STATICS, Tag.TAG_COMPOUND);
        for (int i = 0; i < owners.size(); i++) {
            final CompoundTag owner = owners.getCompound(i);
            statics.put(owner.getString(OWNER), readFields(owner.getList(FIELDS, Tag.TAG_COMPOUND)));
            final List<String> ignored = readNames(owner.getList(IGNORED, Tag.TAG_STRING));
            if (!ignored.isEmpty()) {
                staticsIgnored.put(owner.getString(OWNER), ignored);
            }
        }
        final List<Text> console = TextTags.readAll(tag.getList(CONSOLE, Tag.TAG_COMPOUND));
        final List<Snapshot.WatchShot> watches = new ArrayList<>();
        final ListTag watching = tag.getList(WATCHES, Tag.TAG_COMPOUND);
        for (int i = 0; i < watching.size(); i++) {
            final CompoundTag each = watching.getCompound(i);
            watches.add(new Snapshot.WatchShot(each.getInt(ID), each.getString(NAME),
                    each.getString(KIND), each.getLong(THRESHOLD), readValue(each.getCompound(METHOD)),
                    readValue(each.getCompound(TARGET)), each.getLong(LAST), each.getBoolean(ARMED),
                    each.getBoolean(SEEN)));
        }
        final List<Snapshot.ThreadShot> threads = new ArrayList<>();
        final ListTag running = tag.getList(THREADS, Tag.TAG_COMPOUND);
        for (int i = 0; i < running.size(); i++) {
            final CompoundTag each = running.getCompound(i);
            threads.add(new Snapshot.ThreadShot(each.getInt(ID),
                    readFrames(each.getList(FRAMES, Tag.TAG_COMPOUND)), each.getString(PARKED),
                    each.getLong(UNTIL), readValue(each.getCompound(ON)), readValue(each.getCompound(TOKEN)),
                    each.getBoolean(TIMED_OUT), each.getString(ON_HOST)));
        }
        final List<Snapshot.MonitorShot> monitors = new ArrayList<>();
        final ListTag locked = tag.getList(MONITORS, Tag.TAG_COMPOUND);
        for (int i = 0; i < locked.size(); i++) {
            final CompoundTag each = locked.getCompound(i);
            monitors.add(new Snapshot.MonitorShot(readValue(each.getCompound(TARGET)), each.getInt(OWNER),
                    each.getInt(COUNT)));
        }
        return new Snapshot(format, LongText.get(tag, LISTING),
                new Snapshot.HeapShot(tag.getLong(BUDGET), held, tag.getLong(NEXT_ADDRESS)),
                new Snapshot.IdentityShot(tag.getString(STATE), TextTags.read(tag.getCompound(MESSAGE)),
                        tag.getLong(SPENT),
                        tag.getString(PROGRAM_NAME), readNames(tag.getList(ARGS, Tag.TAG_STRING)),
                        tag.getInt(MACHINE_ID), tag.getBoolean(EXITED), tag.getInt(EXIT_CODE)),
                new Snapshot.ConsoleShot(console, tag.getLong(WRITTEN), tag.getLong(RANDOM),
                        LongText.get(tag, CONSOLE_OPEN)),
                readNames(tag.getList(INPUT, Tag.TAG_STRING)),
                new Snapshot.CallbacksShot(readFrames(tag.getList(WAITING, Tag.TAG_COMPOUND)),
                        tag.getLong(DROPPED)),
                new Snapshot.WindowsShot(readValues(tag.getList(WINDOWS, Tag.TAG_COMPOUND)),
                        Math.max(1, tag.getLong(NEXT_WINDOW)), Math.max(1, tag.getLong(NEXT_WIDGET)),
                        tag.getBoolean(END_WITH_WINDOWS)),
                watches,
                new Snapshot.ListenersShot(readValue(tag.getCompound(ON_MESSAGE)),
                        readValue(tag.getCompound(ON_GATEWAY_MESSAGE)), tag.getString(GATEWAY),
                        tag.contains(ON_SETTLED) ? readValue(tag.getCompound(ON_SETTLED)) : null,
                        readHandlers(tag.getList(SETTLED_FOR, Tag.TAG_COMPOUND)),
                        tag.contains(ON_EVENT) ? readValue(tag.getCompound(ON_EVENT)) : null,
                        readHandlers(tag.getList(CHANNELS, Tag.TAG_COMPOUND))),
                new Snapshot.ThreadsShot(threads, tag.getInt(NEXT_THREAD)),
                monitors, statics, readValue(tag.getCompound(SCRIPT)),
                new Snapshot.IntercalShot(readNames(tag.getList(SWITCHED_OFF, Tag.TAG_STRING)), staticsIgnored));
    }

    // what the program allocated

    private static CompoundTag write(final Snapshot.IHeld one) {
        final CompoundTag tag = new CompoundTag();
        tag.putInt(ID, one.id());
        tag.putLong(BYTES, one.bytes());
        tag.putInt(LINE, one.line());
        tag.putBoolean(FREED, one.freed());
        switch (one) {
            case Snapshot.IHeld.Text text -> {
                tag.putString(KIND, "text");
                LongText.put(tag, VALUE, text.value());
            }
            case Snapshot.IHeld.Object object -> {
                tag.putString(KIND, "object");
                tag.putString(TYPE, object.type());
                tag.put(FIELDS, fields(object.fields()));
                if (!object.ignored().isEmpty()) {
                    tag.put(IGNORED, names(object.ignored()));
                }
            }
            case Snapshot.IHeld.Array array -> {
                tag.putString(KIND, "array");
                tag.putString(TYPE, array.element());
                tag.put(VALUES, values(array.values()));
            }
            case Snapshot.IHeld.Listing list -> {
                tag.putString(KIND, "list");
                tag.put(ITEMS, values(list.items()));
            }
            case Snapshot.IHeld.Keyed keyed -> {
                tag.putString(KIND, "map");
                tag.put(KEYS, values(keyed.keys()));
                tag.put(VALUES, values(keyed.values()));
            }
            case Snapshot.IHeld.Handler handler -> {
                tag.putString(KIND, "handler");
                tag.putString(TYPE, handler.type());
                final ListTag chain = new ListTag();
                for (final Snapshot.BoundShot bound : handler.chain()) {
                    final CompoundTag written = new CompoundTag();
                    written.put(TARGET, write(bound.target()));
                    written.putString(OWNER, bound.owner());
                    written.putString(METHOD, bound.method());
                    written.put(PARAMETERS, names(bound.parameters()));
                    written.putString(RETURNS, bound.returns());
                    chain.add(written);
                }
                tag.put(CHAIN, chain);
            }
            case Snapshot.IHeld.Memory block -> {
                tag.putString(KIND, "memory");
                tag.putLong(ADDRESS, block.address());
                tag.putByteArray(CONTENTS, block.held());
                tag.putBoolean(ON_STACK, block.onStack());
            }
        }
        return tag;
    }

    private static Snapshot.IHeld readHeld(final CompoundTag tag) {
        final int id = tag.getInt(ID);
        final long bytes = tag.getLong(BYTES);
        final int line = tag.getInt(LINE);
        final boolean freed = tag.getBoolean(FREED);
        return switch (tag.getString(KIND)) {
            case "text" -> new Snapshot.IHeld.Text(id, bytes, line, freed, LongText.get(tag, VALUE));
            case "object" -> new Snapshot.IHeld.Object(id, bytes, line, freed, tag.getString(TYPE),
                    readFields(tag.getList(FIELDS, Tag.TAG_COMPOUND)), readNames(tag.getList(IGNORED, Tag.TAG_STRING)));
            case "array" -> new Snapshot.IHeld.Array(id, bytes, line, freed, tag.getString(TYPE),
                    readValues(tag.getList(VALUES, Tag.TAG_COMPOUND)));
            case "list" -> new Snapshot.IHeld.Listing(id, bytes, line, freed,
                    readValues(tag.getList(ITEMS, Tag.TAG_COMPOUND)));
            case "map" -> new Snapshot.IHeld.Keyed(id, bytes, line, freed,
                    readValues(tag.getList(KEYS, Tag.TAG_COMPOUND)),
                    readValues(tag.getList(VALUES, Tag.TAG_COMPOUND)));
            case "handler" -> {
                final List<Snapshot.BoundShot> chain = new ArrayList<>();
                final ListTag written = tag.getList(CHAIN, Tag.TAG_COMPOUND);
                for (int i = 0; i < written.size(); i++) {
                    final CompoundTag bound = written.getCompound(i);
                    chain.add(new Snapshot.BoundShot(readValue(bound.getCompound(TARGET)),
                            bound.getString(OWNER), bound.getString(METHOD),
                            readNames(bound.getList(PARAMETERS, Tag.TAG_STRING)),
                            bound.getString(RETURNS)));
                }
                yield new Snapshot.IHeld.Handler(id, bytes, line, freed, tag.getString(TYPE), chain);
            }
            case "memory" -> Snapshot.IHeld.Memory.of(id, bytes, line, freed, tag.getLong(ADDRESS),
                    tag.getByteArray(CONTENTS), tag.getBoolean(ON_STACK));
            default -> throw new SnapshotException("no snapshot writes a held thing of kind '"
                    + tag.getString(KIND) + "'");
        };
    }

    // what each call was doing

    private static ListTag frames(final List<Snapshot.FrameShot> shots) {
        final ListTag written = new ListTag();
        for (final Snapshot.FrameShot frame : shots) {
            final CompoundTag tag = new CompoundTag();
            tag.putString(OWNER, frame.owner());
            tag.putString(NAME, frame.name());
            tag.put(PARAMETERS, names(frame.parameters()));
            tag.putInt(AT, frame.at());
            tag.put(SELF, write(frame.self()));
            tag.put(SLOTS, values(frame.slots()));
            tag.put(STACK, values(frame.stack()));
            tag.putBoolean(DISCARD, frame.discard());
            if (!frame.ignored().isEmpty()) {
                tag.putIntArray(IGNORED, frame.ignored());
            }
            if (!frame.pendings().isEmpty()) {
                tag.put(PENDINGS, pendings(frame.pendings()));
            }
            if (!frame.blocks().isEmpty()) {
                tag.putLongArray(BLOCKS, frame.blocks());
            }
            written.add(tag);
        }
        return written;
    }

    private static ListTag pendings(final List<Snapshot.PendingShot> shots) {
        final ListTag written = new ListTag();
        for (final Snapshot.PendingShot pending : shots) {
            final CompoundTag tag = new CompoundTag();
            tag.putBoolean(LEAVING, pending.leaving());
            tag.putInt(LINE, pending.line());
            tag.putIntArray(FINALLIES, pending.finallies());
            tag.putInt(NEXT, pending.next());
            tag.put(EXCEPTION, write(pending.exception()));
            tag.putInt(RUNNING, pending.running());
            written.add(tag);
        }
        return written;
    }

    private static List<Snapshot.PendingShot> readPendings(final ListTag written) {
        final List<Snapshot.PendingShot> shots = new ArrayList<>();
        for (int i = 0; i < written.size(); i++) {
            final CompoundTag tag = written.getCompound(i);
            shots.add(new Snapshot.PendingShot(tag.getBoolean(LEAVING), tag.getInt(LINE),
                    Arrays.stream(tag.getIntArray(FINALLIES)).boxed().toList(), tag.getInt(NEXT),
                    readValue(tag.getCompound(EXCEPTION)), tag.getInt(RUNNING)));
        }
        return shots;
    }

    private static List<Snapshot.FrameShot> readFrames(final ListTag written) {
        final List<Snapshot.FrameShot> shots = new ArrayList<>();
        for (int i = 0; i < written.size(); i++) {
            final CompoundTag tag = written.getCompound(i);
            shots.add(new Snapshot.FrameShot(tag.getString(OWNER), tag.getString(NAME),
                    readNames(tag.getList(PARAMETERS, Tag.TAG_STRING)), tag.getInt(AT),
                    readValue(tag.getCompound(SELF)), readValues(tag.getList(SLOTS, Tag.TAG_COMPOUND)),
                    readValues(tag.getList(STACK, Tag.TAG_COMPOUND)), tag.getBoolean(DISCARD),
                    Arrays.stream(tag.getIntArray(IGNORED)).boxed().toList(),
                    readPendings(tag.getList(PENDINGS, Tag.TAG_COMPOUND)),
                    Arrays.stream(tag.getLongArray(BLOCKS)).boxed().toList()));
        }
        return shots;
    }

    // single values

    private static CompoundTag write(final Snapshot.IValue value) {
        final CompoundTag tag = new CompoundTag();
        switch (value) {
            case Snapshot.IValue.Nothing ignored -> tag.putString(KIND, "none");
            case Snapshot.IValue.I4 number -> {
                tag.putString(KIND, "i4");
                tag.putInt(VALUE, number.value());
            }
            case Snapshot.IValue.I8 number -> {
                tag.putString(KIND, "i8");
                tag.putLong(VALUE, number.value());
            }
            case Snapshot.IValue.R4 number -> {
                tag.putString(KIND, "r4");
                tag.putFloat(VALUE, number.value());
            }
            case Snapshot.IValue.R8 number -> {
                tag.putString(KIND, "r8");
                tag.putDouble(VALUE, number.value());
            }
            case Snapshot.IValue.Bool flag -> {
                tag.putString(KIND, "bool");
                tag.putBoolean(VALUE, flag.value());
            }
            case Snapshot.IValue.Ch letter -> {
                tag.putString(KIND, "char");
                tag.putInt(VALUE, letter.value());
            }
            case Snapshot.IValue.Ref reference -> {
                tag.putString(KIND, "ref");
                tag.putInt(VALUE, reference.id());
            }
        }
        return tag;
    }

    private static Snapshot.IValue readValue(final CompoundTag tag) {
        return switch (tag.getString(KIND)) {
            case "i4" -> new Snapshot.IValue.I4(tag.getInt(VALUE));
            case "i8" -> new Snapshot.IValue.I8(tag.getLong(VALUE));
            case "r4" -> new Snapshot.IValue.R4(tag.getFloat(VALUE));
            case "r8" -> new Snapshot.IValue.R8(tag.getDouble(VALUE));
            case "bool" -> new Snapshot.IValue.Bool(tag.getBoolean(VALUE));
            case "char" -> new Snapshot.IValue.Ch((char) tag.getInt(VALUE));
            case "ref" -> new Snapshot.IValue.Ref(tag.getInt(VALUE));
            case "none" -> new Snapshot.IValue.Nothing();
            default -> throw new SnapshotException("no snapshot writes a value of kind '"
                    + tag.getString(KIND) + "'");
        };
    }

    private static ListTag values(final List<Snapshot.IValue> values) {
        final ListTag written = new ListTag();
        for (final Snapshot.IValue value : values) {
            written.add(write(value));
        }
        return written;
    }

    private static List<Snapshot.IValue> readValues(final ListTag written) {
        final List<Snapshot.IValue> values = new ArrayList<>();
        for (int i = 0; i < written.size(); i++) {
            values.add(readValue(written.getCompound(i)));
        }
        return values;
    }

    private static ListTag fields(final Map<String, Snapshot.IValue> fields) {
        final ListTag written = new ListTag();
        for (final Map.Entry<String, Snapshot.IValue> field : fields.entrySet()) {
            final CompoundTag tag = write(field.getValue());
            tag.putString(NAME, field.getKey());
            written.add(tag);
        }
        return written;
    }

    private static Map<String, Snapshot.IValue> readFields(final ListTag written) {
        final Map<String, Snapshot.IValue> fields = new LinkedHashMap<>();
        for (int i = 0; i < written.size(); i++) {
            final CompoundTag tag = written.getCompound(i);
            fields.put(tag.getString(NAME), readValue(tag));
        }
        return fields;
    }

    private static ListTag names(final List<String> names) {
        final ListTag written = new ListTag();
        for (final String name : names) {
            written.add(StringTag.valueOf(name));
        }
        return written;
    }

    private static List<String> readNames(final ListTag written) {
        final List<String> names = new ArrayList<>();
        for (int i = 0; i < written.size(); i++) {
            names.add(written.getString(i));
        }
        return names;
    }

    /*
     * Handlers each kept under a name: the Operation each waits on, by its short id, or the channel each hears. The
     * name is written under the key the first of them was, so a save of either reads back the same.
     */
    private static ListTag writeHandlers(final Map<String, Snapshot.IValue> handlers) {
        final ListTag written = new ListTag();
        handlers.forEach((name, handler) -> {
            final CompoundTag one = new CompoundTag();
            one.putString(OPERATION, name);
            one.put(HANDLER, write(handler));
            written.add(one);
        });
        return written;
    }

    /* Handlers each kept under a name, as {@link #writeHandlers} wrote them. */
    private static Map<String, Snapshot.IValue> readHandlers(final ListTag written) {
        final Map<String, Snapshot.IValue> waiting = new LinkedHashMap<>();
        for (int i = 0; i < written.size(); i++) {
            final CompoundTag one = written.getCompound(i);
            waiting.put(one.getString(OPERATION), readValue(one.getCompound(HANDLER)));
        }
        return waiting;
    }
}
