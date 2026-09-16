/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import com.mojang.logging.LogUtils;
import dev.jstech.computers.vm.program.IProgramParent;
import dev.jstech.computers.vm.program.ProgramEntry;
import dev.jstech.computers.vm.program.ProgramImage;
import dev.jstech.computers.vm.program.ProgramPriority;
import dev.jstech.computers.vm.program.ProgramTable;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.core.JsCore;
import dev.jstech.core.language.ExecutionBalance;
import dev.jstech.core.language.ILanguageProcess;
import dev.jstech.core.language.IProgrammingLanguage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * The programs one computer is running, in whatever languages are registered.
 *
 * <p>A program is called once when it starts, once per tick after that if it is the sort that stays up,
 * and once more when it is stopped. It is never waited on: each tick the machine hands out the
 * instructions its processor is worth and every program spends its share and stops where it stands, so
 * one that loops forever costs the same tick as one that does nothing.
 *
 * <p>Nothing here knows any language. The programs are kept in a {@link ProgramTable}, and what a program was started
 * from is kept beside it: a listing, which the machine runs itself, or what a language runs, handed back to the
 * language that runs its extension. So a program that came back after a reload is the program that was running even
 * if the file has been deleted or edited since.
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

    private static final Logger LOGGER = LogUtils.getLogger();

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
    private final ProgramTicker ticker = new ProgramTicker(this.table, this.focus, this::ended);
    /** How the machine tells a program on another machine that one it started here has ended. */
    private final ObjIntConsumer<IProgramParent.Remote> remoteEnded;
    /** What the machine has to tell its terminal the next time it is used, such as programs a save left out. */
    private final List<String> notices = new ArrayList<>();

    /** Programs with no machine around them, where nothing on another machine can be told anything. */
    public MachinePrograms() {
        this((parent, program) -> {
        });
    }

    /**
     * @param remoteEnded how the machine tells a program on another machine that one it started here has ended, given
     *                    that program and the number of the one that ended
     */
    public MachinePrograms(final ObjIntConsumer<IProgramParent.Remote> remoteEnded) {
        this.remoteEnded = remoteEnded;
    }

    /**
     * Every program the machine lists, in the order they started, as screens read them: a list of its own, built
     * when asked, whose programs cannot be reached through it.
     */
    public List<ProgramView> view() {
        final List<ProgramView> listed = new ArrayList<>(this.table.running().size());
        for (final ProgramEntry<IMachineRuntime> one : this.table.running()) {
            listed.add(new ProgramView(one));
        }
        return listed;
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

    /**
     * Whether a machine runs files with that extension: its own listings, the files a language runs, and the source
     * of a language that only compiles, which the machine compiles on the way in.
     */
    public static boolean runs(final String extension) {
        return MachineListing.claims(extension) || JsCore.languages().runnerOf(extension) != null
                || compilerOnly(extension) != null;
    }

    /** The language that only compiles and is written in files with that extension, or null. */
    @Nullable
    private static IProgrammingLanguage compilerOnly(final String extension) {
        final IProgrammingLanguage language = JsCore.languages().sourceOf(extension);
        return language != null && language.binaryExtensions().isEmpty() ? language : null;
    }

    /** What runs a program a language started or brought back, keeping what it writes in the view it was given. */
    private static IMachineRuntime runtimeOf(final ILanguageProcess process, final HostedView view,
                                             final IProgrammingLanguage language) {
        return process instanceof IMachineRuntime runtime ? runtime : new HostedRuntime(process, view, language);
    }

    /** What the machine has to tell its terminal, oldest first; each is handed over once. */
    public List<String> drainNotices() {
        if (this.notices.isEmpty()) {
            return List.of();
        }
        final List<String> told = List.copyOf(this.notices);
        this.notices.clear();
        return told;
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
        final boolean listing = MachineListing.claims(extension);
        final IProgrammingLanguage runner = listing ? null : JsCore.languages().runnerOf(extension);
        final IProgrammingLanguage compiler = listing || runner != null ? null : compilerOnly(extension);
        if (!listing && runner == null && compiler == null) {
            return Started.failed(name + ": nothing installed runs a ." + extension);
        }
        /*
         * A file of source is compiled here, on the way in, under the file's own name so the program's errors quote
         * it: a language that only compiles gives back a listing the machine runs itself, and one that runs its own
         * source gives back what it runs. What the machine keeps is what came out.
         */
        final IProgrammingLanguage writtenIn = compiler != null ? compiler
                : runner != null && runner.sourceExtensions().contains(extension) ? runner : null;
        String runnable = binary;
        if (writtenIn != null) {
            final List<IProgrammingLanguage.SourceText> sources = new ArrayList<>();
            sources.add(new IProgrammingLanguage.SourceText(name, binary));
            final IProgrammingLanguage.CompileResult built = writtenIn.compile(sources);
            if (!built.ok()) {
                final List<IProgrammingLanguage.Complaint> complaints = built.complaints();
                return Started.failed(complaints.isEmpty() ? name + " does not compile"
                        : complaints.getFirst().format() + (complaints.size() > 1
                        ? " (and " + (complaints.size() - 1) + " more)" : ""));
            }
            runnable = built.binary();
        }
        final int room = Math.clamp(heapMb <= 0 ? DEFAULT_HEAP_MB : heapMb, 1, MAX_HEAP_MB);
        final long heapBytes = (long) room * 1024 * 1024;
        final List<String> given = args == null ? List.of() : args;
        final IMachineRuntime process;
        if (runner == null) {
            process = MachineListing.start(runnable, heapBytes, machine, given);
            if (process == null) {
                // A listing that says what is wrong with it is worth more than being told it is not one.
                final var problem = MachineListing.firstProblem(runnable, machine);
                return Started.failed(name + ": "
                        + (problem == null ? "this is not a " + MachineListing.LABEL : problem.format()));
            }
        } else {
            final HostedView view = new HostedView(machine, heapBytes);
            final ILanguageProcess started = runner.start(runnable, view, given);
            if (started == null) {
                return Started.failed(name + ": this is not something " + runner.displayName() + " can run");
            }
            process = runtimeOf(started, view, runner);
        }
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

    /** Hands over every window the machine's programs have open, with the number of the program that owns it. */
    public void eachWindow(final ObjIntConsumer<Values.Obj> each) {
        // Reading a window changes nothing in the table, so the table is walked without a copy.
        for (final ProgramEntry<IMachineRuntime> one : this.table.running()) {
            for (final Values.Obj window : one.process().windows()) {
                each.accept(window, one.id());
            }
        }
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
     * program that has returned or stopped, or was never here, is not there even while it stays listed for its
     * terminal or its parent to read, and one with too many calls already waiting cannot take it.
     */
    public boolean send(final int from, final int to, final String text, final long tick) {
        final ProgramEntry<IMachineRuntime> target = this.byId(to);
        return target != null && running(target.process()) && target.process().deliverMessage(from, text, tick);
    }

    /**
     * Tells whoever may be waiting on a program that it has ended: every other program on this machine, and the program
     * on another machine that started it. A wait on a program of the same number elsewhere wakes too, and asks again.
     */
    void ended(final int id) {
        final ProgramEntry<IMachineRuntime> one = this.table.byId(id);
        // Being told only wakes a wait, so the table cannot change under this walk.
        for (final ProgramEntry<IMachineRuntime> other : this.table.running()) {
            if (other.id() != id) {
                other.process().programEnded(id);
            }
        }
        if (one != null && one.parent() instanceof IProgramParent.Remote remote) {
            this.remoteEnded.accept(remote, id);
        }
    }

    /** Tells one program here that a program it started on another machine has ended; nothing when it is gone. */
    public void tellEnded(final int program, final int ended) {
        final ProgramEntry<IMachineRuntime> one = this.byId(program);
        if (one != null) {
            one.process().programEnded(ended);
        }
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
        this.ended(id);
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
            // Everything here is going, so only a program on another machine that started one of these is told.
            if (one.parent() instanceof IProgramParent.Remote remote) {
                this.remoteEnded.accept(remote, one.id());
            }
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

    /**
     * The form a machine's programs are written in. A save in any other form is not read and never converted: the
     * programs in it are left out, and the machine says so.
     */
    private static final int FORMAT = 1;

    private static final String FORMAT_KEY = "format";
    private static final String LISTINGS = "listings";
    private static final String CHECKSUM = "checksum";
    private static final String TEXT = "text";
    private static final String LISTING = "listing";
    private static final String HOSTED = "hosted";
    private static final String LANGUAGE = "language";
    private static final String VERSION = "version";
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

    /**
     * Writes every running program down.
     *
     * <p>A listing is written once however many programs run it, and each of them names it by its checksum. A program
     * of another language is written inside an envelope naming that language and the version of what it wrote.
     */
    public void save(final CompoundTag tag) {
        final ListTag written = new ListTag();
        final Map<String, String> listings = new LinkedHashMap<>();
        for (final ProgramEntry<IMachineRuntime> one : this.table.running()) {
            final CompoundTag each = new CompoundTag();
            each.putInt(ID, one.id());
            each.putString(NAME, one.file());
            final IProgrammingLanguage language = one.process().language();
            if (language == null) {
                final String checksum = ProgramImage.checksumOf(one.binary());
                listings.putIfAbsent(checksum, one.binary());
                each.putString(LISTING, checksum);
            } else {
                final CompoundTag envelope = new CompoundTag();
                envelope.putString(LANGUAGE, language.id().toString());
                envelope.putInt(VERSION, language.stateVersion());
                envelope.putString(BINARY, one.binary());
                each.put(HOSTED, envelope);
            }
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
        final ListTag kept = new ListTag();
        for (final Map.Entry<String, String> listing : listings.entrySet()) {
            final CompoundTag each = new CompoundTag();
            each.putString(CHECKSUM, listing.getKey());
            each.putString(TEXT, listing.getValue());
            kept.add(each);
        }
        tag.putInt(FORMAT_KEY, FORMAT);
        tag.put(LISTINGS, kept);
        tag.put(PROGRAMS, written);
        tag.putInt(NEXT, this.table.nextId());
        this.focus.save(tag);
        this.ticker.save(tag);
    }

    /**
     * Reads them back, each one carrying on from where it stopped.
     *
     * <p>A save in another form, or one naming a listing it does not hold, brings nothing back: every program in it is
     * left out, with a line in the log and a notice for the machine's terminal. A program whose language is not
     * installed, or whose language does not take what it wrote, is left out alone.
     */
    public void load(final CompoundTag tag, final BlockEntity machine) {
        final ListTag written = tag.getList(PROGRAMS, Tag.TAG_COMPOUND);
        final Map<String, String> listings = tag.getInt(FORMAT_KEY) == FORMAT ? listingsOf(tag) : null;
        if (listings == null || !namesOnlyWhatItHolds(written, listings)) {
            this.discard(machine);
            return;
        }
        this.table.restart(tag.getInt(NEXT));
        this.focus.load(tag);
        this.ticker.load(tag);
        for (int i = 0; i < written.size(); i++) {
            final CompoundTag each = written.getCompound(i);
            final String name = each.getString(NAME);
            final String binary;
            final IMachineRuntime process;
            if (each.contains(HOSTED, Tag.TAG_COMPOUND)) {
                final CompoundTag envelope = each.getCompound(HOSTED);
                binary = envelope.getString(BINARY);
                process = restoreHosted(name, envelope, each.getCompound(STATE), each.getInt(HEAP), machine);
            } else {
                binary = listings.get(each.getString(LISTING));
                process = MachineListing.restore(binary, each.getCompound(STATE), machine);
            }
            if (process != null) {
                final List<String> args = new ArrayList<>();
                final ListTag given = each.getList(ARGS, Tag.TAG_STRING);
                for (int j = 0; j < given.size(); j++) {
                    args.add(given.getString(j));
                }
                process.identify(each.getInt(ID));
                this.table.add(new ProgramEntry<>(each.getInt(ID), name, binary,
                        each.getInt(HEAP), process, parentOf(each), args,
                        ProgramPriority.named(each.getString(PRIORITY))));
            }
        }
        // The program the terminal held may not have come back, and the terminal cannot stay pointed at nothing.
        this.focus.letGoOfMissing();
    }

    /** The listings a save holds, by checksum, or null when one of them is not the text its checksum names. */
    @Nullable
    private static Map<String, String> listingsOf(final CompoundTag tag) {
        final ListTag kept = tag.getList(LISTINGS, Tag.TAG_COMPOUND);
        final Map<String, String> listings = new LinkedHashMap<>();
        for (int i = 0; i < kept.size(); i++) {
            final CompoundTag each = kept.getCompound(i);
            final String text = each.getString(TEXT);
            if (!ProgramImage.checksumOf(text).equals(each.getString(CHECKSUM))) {
                return null;
            }
            listings.put(each.getString(CHECKSUM), text);
        }
        return listings;
    }

    /** Whether every program in a save names a listing the save holds, or sits in an envelope naming its language. */
    private static boolean namesOnlyWhatItHolds(final ListTag written, final Map<String, String> listings) {
        for (int i = 0; i < written.size(); i++) {
            final CompoundTag each = written.getCompound(i);
            final boolean hosted = each.contains(HOSTED, Tag.TAG_COMPOUND)
                    && !each.getCompound(HOSTED).getString(LANGUAGE).isBlank();
            if (!hosted && !listings.containsKey(each.getString(LISTING))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Brings a program of another language back out of its envelope, or null when that language is not installed or
     * does not take what the program wrote; either way the log says which program was left out.
     */
    @Nullable
    private static IMachineRuntime restoreHosted(final String name, final CompoundTag envelope, final CompoundTag state,
                                                 final int heapMb, final BlockEntity machine) {
        final String named = envelope.getString(LANGUAGE);
        final ResourceLocation id = ResourceLocation.tryParse(named);
        final IProgrammingLanguage language = id == null ? null : JsCore.languages().get(id);
        if (language == null) {
            LOGGER.warn("The program '{}' on the machine at {} was left out: no language called {} is installed", name,
                    where(machine), named);
            return null;
        }
        final HostedView view = new HostedView(machine, (long) Math.clamp(heapMb, 1, MAX_HEAP_MB) * 1024 * 1024);
        final ILanguageProcess restored =
                language.restore(envelope.getString(BINARY), state, envelope.getInt(VERSION), view);
        if (restored == null) {
            LOGGER.warn("The program '{}' on the machine at {} was left out: {} did not bring it back", name,
                    where(machine), named);
            return null;
        }
        return runtimeOf(restored, view, language);
    }

    /**
     * Forgets all a save held of the machine's programs, as a machine does with a save it cannot read: nothing written
     * in another form is guessed at. The log says so, and so does the machine's terminal the next time it is used.
     */
    private void discard(@Nullable final BlockEntity machine) {
        this.table.restart(0);
        this.focus.load(new CompoundTag());
        this.ticker.load(new CompoundTag());
        LOGGER.warn("The programs saved on the machine at {} were left out: the save is not in a form this version "
                + "reads", where(machine));
        this.notices.add("the programs that were running could not be brought back from the save");
    }

    /** Where a machine is, for the log. */
    private static Object where(@Nullable final BlockEntity machine) {
        return machine == null ? "no place" : machine.getBlockPos();
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
        return (int) Math.min(Integer.MAX_VALUE,
                Math.max(coreMegahertz / ExecutionBalance.MEGAHERTZ_PER_INSTRUCTION,
                        ExecutionBalance.LEAST_INSTRUCTIONS_PER_TICK));
    }
}
