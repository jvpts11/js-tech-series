/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.os.OpenWindow;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.RamLedger;
import dev.jstech.computers.os.WindowKeys;
import dev.jstech.computers.os.crash.CrashCause;
import dev.jstech.computers.os.crash.CrashPages;
import dev.jstech.computers.os.crash.OomKiller;
import dev.jstech.core.JsCore;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/**
 * A kernel with an out-of-memory killer on one machine: what it ends when the memory runs out, and what it remembers
 * of the run it is on.
 *
 * <p>The services it ends stay down until the machine starts over, as one the real killer took does until it is
 * started again; the lines it logs are kept for {@code dmesg}, the last of them only, as a kernel's ring of messages
 * keeps. Both belong to the run: a machine that starts over begins with every service up and an empty log.
 */
final class MachineOom {

    private final Set<ResourceLocation> stopped = new LinkedHashSet<>();
    private final List<String> logged = new ArrayList<>();
    /** The number the next thing ended is given when it has none of its own (a window, a service). */
    private int nextPid = FIRST_PID;

    /** Where the numbers of the things a session started begin, past the system's own processes. */
    private static final int FIRST_PID = 1200;
    /** The most lines kept; older ones fall off, as from a kernel's ring of messages. */
    private static final int MOST_LINES = 64;
    /** A kernel keeps fifteen characters of a process's name. */
    private static final int COMM_LENGTH = 15;
    private static final String KEY = "OutOfMemory";
    private static final String STOPPED = "Stopped";
    private static final String LOGGED = "Logged";
    private static final String NEXT_PID = "NextPid";

    /** Whether the killer ended that service in this run. */
    boolean stopped(final ResourceLocation service) {
        return stopped.contains(service);
    }

    /** What the kernel logged in this run about the memory, oldest first. */
    List<String> logged() {
        return List.copyOf(logged);
    }

    /** The machine starts over: every service comes back and the log begins again. */
    void reset() {
        stopped.clear();
        logged.clear();
        nextPid = FIRST_PID;
    }

    /**
     * Makes room on {@code machine}, running {@code system}, for {@code needMb} more: the heaviest window, program or
     * service is ended, then the next, until it fits, and each is logged as the kernel logs it. When everything that
     * could be ended has been and it still does not fit, the kernel panics, naming {@code asking} as the program that
     * asked last.
     *
     * @param upFor how long the system has been up, in ticks, which the Linux kernel stamps on its lines
     * @return whether the room was made; false when the kernel panicked instead
     */
    boolean makeRoom(final AbstractComputerBlockEntity machine, final ServerLevel level, final OsDef system,
                     final String asking, final int needMb, final long upFor) {
        final OomKiller.Plan plan = OomKiller.plan(machine.ramLedger(), needMb);
        if (plan.victims().isEmpty() && plan.fits()) {
            return true;
        }
        final List<CrashPages.Victim> victims = new ArrayList<>();
        final List<String> lines = new ArrayList<>();
        final List<String> windows = new ArrayList<>();
        final List<OpenWindow> open = new ArrayList<>(machine.openWindows());
        boolean front = false;
        for (final RamLedger.Entry entry : plan.victims()) {
            final int pid = entry.kind() == RamLedger.Kind.PROCESS ? entry.id() : nextPid++;
            final CrashPages.Victim victim = new CrashPages.Victim(pid, comm(entry), entry.mb());
            victims.add(victim);
            lines.add(CrashPages.killLine(system.platform(), upFor, victim));
            switch (entry.kind()) {
                case WINDOW -> {
                    if (removeLatest(open, entry.key())) {
                        windows.add(entry.key());
                    }
                }
                case PROCESS -> {
                    front |= machine.programs().held() == entry.id();
                    machine.programs().stop(entry.id());
                }
                case SERVICE -> {
                    final ResourceLocation id = ResourceLocation.tryParse(entry.key());
                    if (id != null && JsCore.languages().get(id) != null) {
                        // A language's runtime is in memory for the programs it runs, so ending it ends them all.
                        front |= machine.programs().held() != 0;
                        machine.programs().stopAll();
                    } else if (id != null) {
                        stopped.add(id);
                    }
                }
                default -> {
                }
            }
        }
        if (!windows.isEmpty()) {
            machine.setOpenWindows(open);
        }
        log(lines);
        machine.setChanged();
        if (!plan.fits()) {
            machine.crash(CrashCause.MEMORY, CrashPages.Moment.memory(asking, nextPid, victims));
            return false;
        }
        machine.memoryFreed(level, lines, windows, front);
        return true;
    }

    /** Writes the run's memory down under a key of its own in {@code tag}. */
    void save(final CompoundTag tag) {
        final CompoundTag own = new CompoundTag();
        final ListTag services = new ListTag();
        for (final ResourceLocation id : stopped) {
            services.add(StringTag.valueOf(id.toString()));
        }
        own.put(STOPPED, services);
        final ListTag lines = new ListTag();
        for (final String line : logged) {
            lines.add(StringTag.valueOf(line));
        }
        own.put(LOGGED, lines);
        own.putInt(NEXT_PID, nextPid);
        tag.put(KEY, own);
    }

    void load(final CompoundTag tag) {
        reset();
        final CompoundTag own = tag.getCompound(KEY);
        for (final Tag one : own.getList(STOPPED, Tag.TAG_STRING)) {
            final ResourceLocation id = ResourceLocation.tryParse(one.getAsString());
            if (id != null) {
                stopped.add(id);
            }
        }
        for (final Tag one : own.getList(LOGGED, Tag.TAG_STRING)) {
            logged.add(one.getAsString());
        }
        if (own.contains(NEXT_PID)) {
            nextPid = Math.max(FIRST_PID, own.getInt(NEXT_PID));
        }
    }

    private void log(final List<String> lines) {
        logged.addAll(lines);
        while (logged.size() > MOST_LINES) {
            logged.removeFirst();
        }
    }

    /*
     * The name a kernel gives the process: the command a window's or a service's program is run by, and a script's own
     * name, cut where the kernel cuts it.
     */
    private static String comm(final RamLedger.Entry entry) {
        final String name = switch (entry.kind()) {
            case WINDOW -> {
                final ProgramSpec spec = WindowKeys.program(entry.key());
                yield spec != null ? spec.commandName() : afterColon(entry.key());
            }
            case SERVICE -> {
                final ResourceLocation id = ResourceLocation.tryParse(entry.key());
                final ProgramSpec spec = id == null ? null : OsRegistry.getProgram(id);
                yield spec != null ? spec.commandName() : entry.name();
            }
            default -> entry.name();
        };
        return name.length() > COMM_LENGTH ? name.substring(0, COMM_LENGTH) : name;
    }

    private static String afterColon(final String key) {
        final int colon = key.indexOf(':');
        return colon < 0 ? key : key.substring(colon + 1);
    }

    /* Takes the latest window of that key out of the list, which is the one a desktop opened last. */
    private static boolean removeLatest(final List<OpenWindow> open, final String key) {
        for (int i = open.size() - 1; i >= 0; i--) {
            if (open.get(i).key().equals(key)) {
                open.remove(i);
                return true;
            }
        }
        return false;
    }
}
