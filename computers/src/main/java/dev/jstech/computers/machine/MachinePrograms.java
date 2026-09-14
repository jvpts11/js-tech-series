/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.vm.program.IProgramParent;
import dev.jstech.computers.vm.program.ProgramEntry;
import dev.jstech.computers.vm.program.ProgramPriority;
import dev.jstech.computers.vm.program.ProgramTable;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.core.JsCore;
import dev.jstech.core.language.ILanguageProcess;
import dev.jstech.core.language.IProgrammingLanguage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The programs one computer is running, in whatever languages are registered.
 *
 * <p>A program is called once when it starts, once per tick after that if it is the sort that stays up,
 * and once more when it is stopped. It is never waited on: each tick the machine hands out the
 * instructions its processor is worth and every program spends its share and stops where it stands, so
 * one that loops forever costs the same tick as one that does nothing.
 *
 * <p>Nothing here knows any language. The programs are kept in a {@link ProgramTable}, and the text a
 * program was started from is kept beside it and handed back to whichever language claims its extension,
 * so a program that came back after a reload is the program that was running even if the file has been
 * deleted or edited since.
 */
public final class MachinePrograms {

    /** What a program gets when it did not ask for a size of its own. */
    public static final int DEFAULT_HEAP_MB = 1;

    /** The most a program may ask for, because a script is not what a machine's memory is for. */
    public static final int MAX_HEAP_MB = 64;

    /**
     * What every farewell on one machine may spend between them in one tick: the programs stopped in a tick share
     * it, and a program that has not finished its farewell by the end of its share is stopped anyway.
     */
    static final int FAREWELL_PER_TICK = 4096;

    /** Who is waiting on a machine with no world to ask: nobody that can be found. */
    private static final Predicate<IProgramParent.Remote> NO_WORLD = parent -> false;

    /** What came of asking for a program to start: its number, or why it did not. */
    public record Started(int id, String message) {

        public boolean ok() {
            return this.id > 0;
        }

        static Started failed(final String why) {
            return new Started(0, why);
        }
    }

    private final ProgramTable<IMachineRuntime> table = new ProgramTable<>();
    private final TerminalFocus focus = new TerminalFocus(this.table, this::stop);
    private final ProgramTicker ticker = new ProgramTicker(this.table, this.focus);

    /** Everything running, in the order it was started. */
    public List<ProgramEntry<IMachineRuntime>> all() {
        return this.table.all();
    }

    /** The program of that number, or null. */
    public ProgramEntry<IMachineRuntime> byId(final int id) {
        return this.table.byId(id);
    }

    /** Whether anything is running at all, which is what lets a machine skip the work entirely. */
    public boolean isEmpty() {
        return this.table.isEmpty();
    }

    /**
     * Charges the machine for work done on its behalf outside its programs (a Gateway answering a
     * ComputerCraft computer, say): the next tick's programs get that much less, and a debt larger than
     * one tick's worth carries over, so a hammered bridge slows this machine and nothing else.
     */
    public void owe(final int credits) {
        this.ticker.owe(credits);
    }

    /** What the machine still owes for work done outside its programs. */
    public int owed() {
        return this.ticker.owed();
    }

    /** The megabytes every running program is holding between them. */
    public int heapMb() {
        return this.table.heapMb();
    }

    /** The program the terminal is holding, or 0; see {@link TerminalFocus}. */
    public int held() {
        return this.focus.held();
    }

    /** Says the terminal is now waiting on that program. */
    public void hold(final int id) {
        this.focus.hold(id);
    }

    /** Hands a line typed at the terminal to the program it is holding; false when it holds none. */
    public boolean offerInput(final String line) {
        return this.focus.offerInput(line);
    }

    /**
     * Whether a program is still going: running, or parked until something it waits for, or a script that has
     * finished its turn and is asked again next tick. A program that returned or halted is not, even while it stays
     * listed for its terminal or its parent to read.
     */
    public static boolean running(final ILanguageProcess process) {
        final ILanguageProcess.State state = process.state();
        return state == ILanguageProcess.State.RUNNING || state == ILanguageProcess.State.PARKED
                || (state == ILanguageProcess.State.FINISHED && process.isService());
    }

    /** How a program's state reads to a person: a program stopped on a read is waiting for input. */
    public static String stateOf(final ILanguageProcess process) {
        return process.waitingForInput() ? "input"
                : process.state().name().toLowerCase(java.util.Locale.ROOT);
    }

    /** Lets the terminal go, clearing the program away if it had already finished. */
    public void release() {
        this.focus.release();
    }

    /** What the held program has printed since this was last asked, and never the same line twice. */
    public List<String> unseen() {
        return this.focus.unseen();
    }

    /**
     * Starts a program from the text it was compiled to.
     *
     * <p>Which language runs it follows from what the file is called, so a machine runs whatever is
     * registered without knowing any of them by name.
     */
    public Started start(final String name, final String binary, final int heapMb,
                         final BlockEntity machine) {
        return this.start(name, binary, heapMb, machine, List.of(), IProgramParent.NONE, ProgramPriority.MEDIUM);
    }

    /**
     * The same, started by another program, here or on another machine, with what it was given and how urgently.
     * One started by a program keeps what it leaves (its output and its exit code) for as long as that program is
     * there to read it.
     *
     * @param args     what the program's {@code Program.Args} will read
     * @param parent   the program that started it, or none for one started at the prompt
     * @param priority how urgently it runs; a low one is passed over every other round of the tick
     */
    public Started start(final String name, final String binary, final int heapMb,
                         final BlockEntity machine, final List<String> args, final IProgramParent parent,
                         final ProgramPriority priority) {
        final int dot = name.lastIndexOf('.');
        final String extension = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        final IProgrammingLanguage language = JsCore.languages().runnerOf(extension);
        if (language == null) {
            return Started.failed(name + ": nothing installed runs a ." + extension);
        }
        /*
         * A language that runs its own source files compiles them here, on the way in, under the
         * file's own name so the program's errors quote it; what the machine keeps is the listing.
         */
        String runnable = binary;
        if (language.sourceExtensions().contains(extension)) {
            final List<IProgrammingLanguage.SourceText> sources = new ArrayList<>();
            sources.add(new IProgrammingLanguage.SourceText(name, binary));
            final IProgrammingLanguage.CompileResult built = language.compile(sources);
            if (!built.ok()) {
                final List<IProgrammingLanguage.Complaint> complaints = built.complaints();
                return Started.failed(complaints.isEmpty() ? name + " does not compile"
                        : complaints.getFirst().format() + (complaints.size() > 1
                        ? " (and " + (complaints.size() - 1) + " more)" : ""));
            }
            runnable = built.binary();
        }
        final int room = Math.clamp(heapMb <= 0 ? DEFAULT_HEAP_MB : heapMb, 1, MAX_HEAP_MB);
        final ILanguageProcess started =
                language.start(runnable, (long) room * 1024 * 1024, machine, args == null ? List.of() : args);
        if (started == null) {
            return Started.failed(name + ": this is not something " + language.displayName() + " can run");
        }
        final IMachineRuntime process = IMachineRuntime.of(started);
        final int id = this.table.takeId();
        process.identify(id);
        this.table.add(new ProgramEntry<>(id, name, runnable, room, process, parent, args, priority));
        return new Started(id, name + " started as " + id);
    }

    /**
     * Hands every program listening what a ComputerCraft computer said through a Gateway; how many heard
     * it. A machine where nobody is listening simply drops it, which is what a message nobody wants is.
     */
    public int deliverGatewayMessage(final int from, final String text, final long tick) {
        int heard = 0;
        // Hearing a message only queues a call, so the table cannot change under this walk.
        for (final ProgramEntry<IMachineRuntime> one : this.table.running()) {
            if (one.process().deliverGatewayMessage(from, text, tick)) {
                heard++;
            }
        }
        return heard;
    }

    /** The windows a program has open on the machine's desktop; empty for one that has none. */
    public List<Values.Obj> windowsOf(final int id) {
        final ProgramEntry<IMachineRuntime> one = this.byId(id);
        return one == null ? List.of() : one.process().windows();
    }

    /**
     * Hands a program what a player did to one of its widgets; false when there is no such program, no
     * such window, or nothing there that answers.
     */
    public boolean deliverUiEvent(final int id, final long window, final long widget, final String kind,
                                  final List<Object> values) {
        final ProgramEntry<IMachineRuntime> one = this.byId(id);
        return one != null && one.process().deliverUiEvent(window, widget, kind, values);
    }

    /**
     * Hands a line from one program to another on this machine.
     *
     * <p>True when the other program is there to take it, whether or not it does anything with it; a
     * program that has stopped, or was never here, is not there, and one with too many calls already
     * waiting cannot take it.
     */
    public boolean send(final int from, final int to, final String text, final long tick) {
        final ProgramEntry<IMachineRuntime> target = this.byId(to);
        if (target == null || target.process().state() == ILanguageProcess.State.HALTED) {
            return false;
        }
        return target.process().deliverMessage(from, text, tick);
    }

    /**
     * Stops a program, letting it say goodbye first.
     *
     * <p>The farewell is paid for out of the machine's farewell budget for the tick rather than its programs'
     * credits, because a machine that is being taken apart cannot be asked to wait several ticks for it; a program
     * stopped once that budget is spent is stopped without one.
     */
    public boolean stop(final int id) {
        final ProgramEntry<IMachineRuntime> one = this.byId(id);
        if (one == null) {
            return false;
        }
        this.ticker.farewell(one.process());
        this.table.remove(id);
        this.focus.forget(id);
        return true;
    }

    /** Stops everything, as a machine being turned off or broken does, sharing a tick's farewell budget evenly. */
    public void stopAll() {
        final List<ProgramEntry<IMachineRuntime>> all = this.table.all();
        final int share = this.ticker.farewellShare(all.size());
        for (final ProgramEntry<IMachineRuntime> one : all) {
            this.ticker.farewell(one.process(), share);
            this.table.remove(one.id());
            this.focus.forget(one.id());
        }
    }

    /** Gives the machine's instructions out and runs them, with all the time in the world. */
    public void tick(final int credits) {
        this.tick(credits, Long.MAX_VALUE, null);
    }

    /** The same, with a way to look up what the network holds and still no deadline. */
    public void tick(final int credits, final ToLongFunction<String> stock) {
        this.tick(credits, Long.MAX_VALUE, stock);
    }

    /** The same, bounded by the clock, on a machine with no world to find other machines in. */
    public void tick(final int credits, final long deadline, final ToLongFunction<String> stock) {
        this.tick(credits, deadline, stock, NO_WORLD);
    }

    /**
     * The same, bounded by the clock, with a way to look up what the network holds and a way to ask
     * whether a program on another machine is still waiting on one started here.
     *
     * <p>Everything being watched is looked up once, however many programs are watching it, and the
     * answers are handed to each of them. A machine watching nothing pays nothing for the ability.
     *
     * <p>Every program that is still going gets the same share of the tick, dealt in turns so none
     * finishes its share before another has begun, and the tick ends early when the deadline passes,
     * with the program that went without first in line next time. A program that stays up and has
     * finished what it was asked to do is asked again, which is what makes it stay up.
     *
     * @param credits  how many instructions the machine's processors are worth this tick
     * @param deadline when the tick must end, on {@link System#nanoTime()}; one already passed runs
     *                 nothing, and one far in the future never interrupts
     * @param stock    what the network holds of an item, or null on a machine that cannot ask
     * @param waiting  whether the program on another machine that started one here is still there; only
     *                 ever asked about a program that has finished
     */
    public void tick(final int credits, final long deadline, final ToLongFunction<String> stock,
                     final Predicate<IProgramParent.Remote> waiting) {
        this.ticker.tick(credits, deadline, stock, waiting);
    }

    // across a reload

    private static final String PROGRAMS = "programs";
    private static final String NEXT = "next";
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String BINARY = "binary";
    private static final String HEAP = "heap";
    private static final String STATE = "state";
    private static final String PARENT = "parent";
    private static final String REMOTE_PARENT = "remoteParent";
    private static final String MACHINE = "machine";
    private static final String NODE = "node";
    private static final String ARGS = "args";
    private static final String PRIORITY = "priority";

    /** Writes every running program down. */
    public void save(final CompoundTag tag) {
        final ListTag written = new ListTag();
        for (final ProgramEntry<IMachineRuntime> one : this.table.running()) {
            final CompoundTag each = new CompoundTag();
            each.putInt(ID, one.id());
            each.putString(NAME, one.file());
            each.putString(BINARY, one.binary());
            each.putInt(HEAP, one.heapMb());
            each.putInt(PARENT, one.parent() instanceof IProgramParent.Local local ? local.program() : 0);
            if (one.parent() instanceof IProgramParent.Remote remote) {
                final CompoundTag from = new CompoundTag();
                from.putLong(MACHINE, remote.machine());
                from.putUUID(NODE, remote.node());
                from.putInt(ID, remote.program());
                each.put(REMOTE_PARENT, from);
            }
            final ListTag args = new ListTag();
            for (final String arg : one.args()) {
                args.add(StringTag.valueOf(arg));
            }
            each.put(ARGS, args);
            each.putString(PRIORITY, one.priority().serializedName());
            final CompoundTag state = new CompoundTag();
            one.process().save(state);
            each.put(STATE, state);
            written.add(each);
        }
        tag.put(PROGRAMS, written);
        tag.putInt(NEXT, this.table.nextId());
        this.focus.save(tag);
        this.ticker.save(tag);
    }

    /** Reads them back, each one carrying on from where it stopped. */
    public void load(final CompoundTag tag, final BlockEntity machine) {
        this.table.restart(tag.getInt(NEXT));
        this.focus.load(tag);
        this.ticker.load(tag);
        final ListTag written = tag.getList(PROGRAMS, Tag.TAG_COMPOUND);
        for (int i = 0; i < written.size(); i++) {
            final CompoundTag each = written.getCompound(i);
            final String name = each.getString(NAME);
            final int dot = name.lastIndexOf('.');
            final IProgrammingLanguage language = JsCore.languages()
                    .runnerOf(dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT));
            if (language == null) {
                /*
                 * The language that ran this is no longer installed. Dropping the program is better
                 * than refusing to load the machine it was on.
                 */
                continue;
            }
            final ILanguageProcess restored = language.restore(each.getString(BINARY),
                    each.getCompound(STATE), machine);
            if (restored != null) {
                final List<String> args = new ArrayList<>();
                final ListTag given = each.getList(ARGS, Tag.TAG_STRING);
                for (int j = 0; j < given.size(); j++) {
                    args.add(given.getString(j));
                }
                final IMachineRuntime process = IMachineRuntime.of(restored);
                process.identify(each.getInt(ID));
                this.table.add(new ProgramEntry<>(each.getInt(ID), name, each.getString(BINARY),
                        each.getInt(HEAP), process, parentOf(each), args,
                        ProgramPriority.named(each.getString(PRIORITY))));
            }
        }
        // The program the terminal held may not have come back, and the terminal cannot stay pointed at nothing.
        this.focus.letGoOfMissing();
    }

    /** The program that started this one: on another machine, on this one, or none. */
    private static IProgramParent parentOf(final CompoundTag each) {
        if (each.contains(REMOTE_PARENT, Tag.TAG_COMPOUND)) {
            final CompoundTag from = each.getCompound(REMOTE_PARENT);
            if (from.hasUUID(NODE)) {
                return new IProgramParent.Remote(from.getLong(MACHINE), from.getUUID(NODE), from.getInt(ID));
            }
        }
        final int local = each.getInt(PARENT);
        return local > 0 ? new IProgramParent.Local(local) : IProgramParent.NONE;
    }

    // what the machine is worth

    /** The fewest instructions a tick, so even the oldest processor that can run this gets somewhere. */
    public static final int LEAST_PER_TICK = 32;

    /**
     * What a machine's processors are worth in a tick, given their cores times their megahertz added up.
     *
     * <p>It follows the clock with no ceiling, so a faster machine really does get through more of a
     * program in the same second, however fast it is; an old machine still moves. What keeps a machine
     * from taking the server's tick with it is not a cap on these but the clock the tick is run against
     * (see {@link ServerTickDeadline}).
     */
    public static int creditsFor(final long coreMegahertz) {
        if (coreMegahertz <= 0) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.max(coreMegahertz / 8, LEAST_PER_TICK));
    }
}
