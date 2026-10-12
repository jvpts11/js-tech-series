/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.crash;

import dev.jstech.computers.os.KernelNames;
import dev.jstech.computers.os.Platform;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/**
 * Works out the page a system crashes onto, from the system and what the machine knows about the moment.
 *
 * <p>The numbers are the real systems' own: the STOP code and its parameters each crash of that kind printed, the
 * fatal exception's number (0E is a page fault, 12 the processor's machine check), the kernels' messages word for
 * word. What changes from one crash to the next is what the machine was doing: which program asked, which ones the
 * kernel killed first, how long it had been up, what it is called and what it is built on.
 */
public final class CrashPages {

    /* How long each page stands before the machine starts over, in ticks. */
    private static final int FRAMES_95_TICKS = 84;
    private static final int FRAMES_XP_TICKS = 120;
    private static final int FRAMES_7_TICKS = 160;
    private static final int FRAMES_MODERN_TICKS = 200;
    /* The Linux page says it reboots in 10 seconds and FreeBSD's in 15, and they do; System V does it at once. */
    private static final int LINUX_TICKS = 200;
    private static final int FREEBSD_TICKS = 300;
    private static final int UNIX_TICKS = 100;
    /** The moment the kernels' clocks count from, so the time a panic prints is a plausible one. */
    private static final long EPOCH_BASE = 1_728_741_600L;
    private static final String LINUX_REBOOT = "Rebooting in 10 seconds..";
    private static final String FREEBSD_REBOOT = "Automatic reboot in 15 seconds - press a key on the console to abort";

    private CrashPages() {
    }

    /** What the machine knows when it goes down, which the kernels print. */
    public record Facts(String hostName, String distro, String board, String cpuVendor, String initName,
                        long uptimeTicks, long ramBytes, String asking, int askingPid, List<Victim> killed,
                        String typed, int signal, String part) {

        public Facts {
            killed = List.copyOf(killed);
        }

        /** The machine's own facts, with what it was doing at {@code moment}. */
        public static Facts of(final String hostName, final String distro, final String board, final String cpuVendor,
                               final String initName, final long uptimeTicks, final long ramBytes,
                               final Moment moment) {
            return new Facts(hostName, distro, board, cpuVendor, initName, uptimeTicks, ramBytes, moment.asking(),
                    moment.askingPid(), moment.killed(), moment.typed(), moment.signal(), moment.part());
        }
    }

    /**
     * What the machine was doing when it went down: the program that asked for memory and the ones the kernel killed
     * first, the line that killed its first process and with which signal, or the part that was taken out.
     */
    public record Moment(String asking, int askingPid, List<Victim> killed, String typed, int signal, String part) {

        public Moment {
            killed = List.copyOf(killed);
        }

        /** A program asked for memory that was not there, after the kernel had killed {@code killed} for theirs. */
        public static Moment memory(final String asking, final int askingPid, final List<Victim> killed) {
            return new Moment(asking, askingPid, killed, "", 0, "");
        }

        /** The part called {@code part} was taken out from under the running system. */
        public static Moment hardware(final String part) {
            return new Moment("", 0, List.of(), "", 0, part);
        }

        /** {@code typed} sent {@code signal} to the first process, which died of it. */
        public static Moment initKilled(final String typed, final int signal) {
            return new Moment("", 0, List.of(), typed, signal, "");
        }
    }

    /** A process the kernel killed for its memory before it gave up: its number, its name and what it held. */
    public record Victim(int pid, String name, int mb) {
    }

    /** The page a system of that platform crashes onto, or null for one that has none (MC-DOS, MC-NET). */
    @Nullable
    public static CrashStyle styleFor(final Platform platform, final int familyRank) {
        return switch (platform) {
            case FRAMES -> switch (familyRank) {
                case 1 -> CrashStyle.FRAMES_95;
                case 2 -> CrashStyle.FRAMES_XP;
                case 3 -> CrashStyle.FRAMES_7;
                case 4 -> CrashStyle.FRAMES_10;
                default -> CrashStyle.FRAMES_11;
            };
            case LINUX, FREEBSD, UNIX -> CrashStyle.CONSOLE;
            case MC_DOS, MC_NET -> null;
        };
    }

    /** The page of a {@code platform} system of that style crashing for {@code cause}. */
    public static CrashPage of(final CrashStyle style, final Platform platform, final CrashCause cause,
                               final Facts facts) {
        return switch (style) {
            case FRAMES_95 -> frames95(cause);
            case FRAMES_XP -> framesNt(style, cause, FRAMES_XP_TICKS);
            case FRAMES_7 -> framesNt(style, cause, FRAMES_7_TICKS);
            case FRAMES_10, FRAMES_11 -> framesModern(style, cause, facts);
            case CONSOLE -> switch (platform) {
                case FREEBSD -> new CrashPage(style, cause, "", "", facts.part(), freeBsd(cause, facts),
                        FREEBSD_TICKS, true);
                case UNIX -> new CrashPage(style, cause, "", "", facts.part(), systemV(cause, facts), UNIX_TICKS,
                        false);
                default -> new CrashPage(style, cause, "", "", facts.part(), linux(cause, facts), LINUX_TICKS,
                        false);
            };
        };
    }

    /** The line the Linux kernel logs for a process its out-of-memory killer took, as {@code dmesg} keeps it. */
    public static String linuxKill(final Victim victim) {
        final long rss = victim.mb() * 1024L;
        return "Out of memory: Killed process " + victim.pid() + " (" + victim.name() + ") total-vm:"
                + rss * 31 / 20 + "kB, anon-rss:" + rss + "kB, UID:0 pgtables:" + (victim.mb() * 2 + 180)
                + "kB oom_score_adj:0";
    }

    /** The line FreeBSD prints for a process it killed for want of memory. */
    public static String freeBsdKill(final Victim victim) {
        return "pid " + victim.pid() + " (" + victim.name() + "), jid 0, uid 0, was killed: out of swap space";
    }

    /** The line System V prints for a process it killed for want of memory. */
    public static String systemVKill(final Victim victim) {
        return "NOTICE: out of memory: process " + victim.pid() + " (" + victim.name() + ") killed";
    }

    /**
     * The line a kernel of {@code platform} logs for a process its out-of-memory killer took, {@code uptimeTicks} after
     * it came up: Linux stamps it with the time, FreeBSD and System V print it as it is.
     */
    public static String killLine(final Platform platform, final long uptimeTicks, final Victim victim) {
        return switch (platform) {
            case FREEBSD -> freeBsdKill(victim);
            case UNIX -> systemVKill(victim);
            default -> new Clock(uptimeTicks).stamp(linuxKill(victim));
        };
    }

    private static CrashPage frames95(final CrashCause cause) {
        return switch (cause) {
            case HARDWARE -> new CrashPage(CrashStyle.FRAMES_95, cause, "12", "0028:C0001F12 in VXD VMM(01) + 00001F12",
                    "", List.of(), FRAMES_95_TICKS, false);
            case INIT_KILLED -> new CrashPage(CrashStyle.FRAMES_95, cause, "0D",
                    "0028:C000A2F4 in VXD VMM(01) + 0000A2F4", "", List.of(), FRAMES_95_TICKS, false);
            case MEMORY -> new CrashPage(CrashStyle.FRAMES_95, cause, "0E", "0028:C0011E36 in VXD VMM(01) + 00010E36",
                    "", List.of(), FRAMES_95_TICKS, false);
        };
    }

    private static CrashPage framesNt(final CrashStyle style, final CrashCause cause, final int ticks) {
        return switch (cause) {
            case HARDWARE -> new CrashPage(style, cause, "MACHINE_CHECK_EXCEPTION",
                    "0x0000009C (0x00000004,0x80549C00,0xB2000000,0x00070F0F)", "", List.of(), ticks, false);
            case INIT_KILLED -> new CrashPage(style, cause, "CRITICAL_PROCESS_DIED",
                    "0x000000EF (0x89D2B1A8,0x00000000,0x00000000,0x00000000)", "", List.of(), ticks, false);
            case MEMORY -> new CrashPage(style, cause, "MEMORY_MANAGEMENT",
                    "0x0000001A (0x00041790,0xC0A00000,0x0000FFFF,0x00000000)", "", List.of(), ticks, false);
        };
    }

    private static CrashPage framesModern(final CrashStyle style, final CrashCause cause, final Facts facts) {
        final String code = switch (cause) {
            case HARDWARE -> "WHEA_UNCORRECTABLE_ERROR";
            case INIT_KILLED -> "CRITICAL_PROCESS_DIED";
            case MEMORY -> "MEMORY_MANAGEMENT";
        };
        return new CrashPage(style, cause, code, "", cause == CrashCause.HARDWARE ? facts.part() : "", List.of(),
                FRAMES_MODERN_TICKS, false);
    }

    private static List<String> linux(final CrashCause cause, final Facts facts) {
        final Clock clock = new Clock(facts.uptimeTicks());
        final List<String> out = new ArrayList<>();
        final String kernel = KernelNames.LINUX_VERSION + " #1 " + facts.distro();
        final String hardware = "Hardware name: JSC Technologies " + facts.board();
        final String reason;
        final List<String> trace;
        switch (cause) {
            case HARDWARE -> {
                out.add(clock.stamp("mce: [Hardware Error]: CPU 0: Machine Check Exception: 5 Bank 0:"
                        + " b200000000070005"));
                out.add(clock.stamp("mce: [Hardware Error]: TSC 0"));
                out.add(clock.stamp("mce: [Hardware Error]: PROCESSOR 0:6fd TIME " + clock.epoch()
                        + " SOCKET 0 APIC 0"));
                out.add(clock.stamp("mce: [Hardware Error]: Run the above through 'mcelog --ascii'"));
                out.add(clock.stamp("mce: [Hardware Error]: Machine check: Processor context corrupt"));
                reason = "Fatal machine check";
                out.add(clock.stamp("Kernel panic - not syncing: " + reason));
                trace = List.of();
            }
            case INIT_KILLED -> {
                out.add("root@" + facts.hostName() + ":~# " + facts.typed());
                out.add("");
                reason = "Attempted to kill init! exitcode=0x" + String.format(Locale.ROOT, "%08x", facts.signal());
                out.add(clock.stamp("Kernel panic - not syncing: " + reason));
                out.add(clock.stamp("CPU: 0 PID: 1 Comm: " + facts.initName() + " Not tainted " + kernel));
                out.add(clock.stamp(hardware));
                trace = List.of(" dump_stack_lvl+0x44/0x5c", " panic+0x118/0x2ed", " do_exit.cold+0x15/0xa0",
                        " do_group_exit+0x2d/0x80", " get_signal+0x8b4/0x8c0");
            }
            case MEMORY -> {
                for (final Victim victim : facts.killed()) {
                    out.add(clock.stamp(linuxKill(victim)));
                }
                out.add(clock.stamp("Out of memory and no killable processes..."));
                reason = "System is deadlocked on memory";
                out.add(clock.stamp("Kernel panic - not syncing: " + reason));
                out.add(clock.stamp("CPU: 0 PID: " + facts.askingPid() + " Comm: " + facts.asking() + " Not tainted "
                        + kernel));
                out.add(clock.stamp(hardware));
                trace = List.of(" dump_stack_lvl+0x44/0x5c", " panic+0x118/0x2ed", " out_of_memory.cold+0x67/0x7e",
                        " __alloc_pages_slowpath.constprop.0+0xc8e/0xd60");
            }
            default -> throw new IllegalStateException();
        }
        if (!trace.isEmpty()) {
            out.add(clock.stamp("Call Trace:"));
            out.add(clock.stamp(" <TASK>"));
            for (final String frame : trace) {
                out.add(clock.stamp(frame));
            }
            out.add(clock.stamp(" </TASK>"));
        }
        out.add(clock.stamp("Kernel Offset: disabled"));
        out.add(clock.stamp(LINUX_REBOOT));
        out.add(clock.stamp("---[ end Kernel panic - not syncing: " + reason + " ]---"));
        return out;
    }

    private static List<String> freeBsd(final CrashCause cause, final Facts facts) {
        final Clock clock = new Clock(facts.uptimeTicks());
        final List<String> out = new ArrayList<>();
        final List<String> frames;
        switch (cause) {
            case HARDWARE -> {
                out.add("MCA: Bank 4, Status 0xbe00000000800400");
                out.add("MCA: Global Cap 0x0000000000000106, Status 0x0000000000000005");
                out.add("MCA: Vendor \"" + facts.cpuVendor() + "\", ID 0x6fd, APIC ID 0");
                out.add("MCA: CPU 0 UNCOR PCC internal timer error");
                out.add("panic: Unrecoverable machine check exception");
                frames = List.of("0xffffffff810a4c71 at mca_intr+0x81", "0xffffffff8109c2f3 at trap+0x93");
            }
            case INIT_KILLED -> {
                out.add("root@" + facts.hostName() + ":~ # " + facts.typed());
                out.add("init died (signal " + facts.signal() + ", exit 0)");
                out.add("panic: Going nowhere without my init!");
                frames = List.of("0xffffffff80b04e3a at exit1+0x5ba", "0xffffffff80b0b2c1 at sigexit+0x6a1");
            }
            case MEMORY -> {
                for (final Victim victim : facts.killed()) {
                    out.add(freeBsdKill(victim));
                }
                out.add("panic: kmem_malloc(16384): kmem_map too small: " + facts.ramBytes() + " total allocated");
                frames = List.of("0xffffffff80ed2d55 at kmem_malloc_domainset+0x95",
                        "0xffffffff80b0e8c3 at malloc+0x63");
            }
            default -> throw new IllegalStateException();
        }
        out.add("cpuid = 0");
        out.add("time = " + clock.epoch());
        out.add("KDB: stack backtrace:");
        out.add("#0 0xffffffff80b9002d at kdb_backtrace+0x5d");
        out.add("#1 0xffffffff80b43132 at vpanic+0x132");
        out.add("#2 0xffffffff80b42fb3 at panic+0x43");
        for (int i = 0; i < frames.size(); i++) {
            out.add("#" + (i + 3) + " " + frames.get(i));
        }
        out.add("Uptime: " + clock.freeBsdUptime());
        out.add(FREEBSD_REBOOT);
        return out;
    }

    private static List<String> systemV(final CrashCause cause, final Facts facts) {
        final List<String> out = new ArrayList<>();
        final String panic;
        final String trap;
        switch (cause) {
            case HARDWARE -> {
                panic = "PANIC: memory parity error";
                trap = "Trap Type 00000002";
            }
            case INIT_KILLED -> {
                out.add("# " + facts.typed());
                out.add("");
                panic = "PANIC: init died";
                trap = "";
            }
            case MEMORY -> {
                for (final Victim victim : facts.killed()) {
                    out.add(systemVKill(victim));
                }
                if (!facts.killed().isEmpty()) {
                    out.add("");
                }
                panic = "PANIC: kmem_alloc: out of kernel memory";
                trap = "Trap Type 0000000e";
            }
            default -> throw new IllegalStateException();
        }
        out.add(panic);
        out.add("");
        if (!trap.isEmpty()) {
            out.add(trap);
            out.add("");
        }
        out.add("Syncing disks... done");
        out.add("");
        out.add("Automatic reboot in progress...");
        return out;
    }

    /** The kernel's clock at the moment of the panic, ticking a few microseconds between the lines it prints. */
    private static final class Clock {

        private final long uptimeTicks;
        private int line;

        Clock(final long uptimeTicks) {
            this.uptimeTicks = Math.max(0L, uptimeTicks);
        }

        /** A line as the Linux kernel's log stamps it: seconds since boot, to the microsecond, in brackets. */
        String stamp(final String message) {
            final long micros = this.uptimeTicks * 50_000L + 338_190L + this.line++ * 7L;
            return String.format(Locale.ROOT, "[%5d.%06d] %s", micros / 1_000_000L, micros % 1_000_000L, message);
        }

        long epoch() {
            return EPOCH_BASE + this.uptimeTicks / 20L;
        }

        /** How long the machine was up, the way FreeBSD writes it: hours, minutes and seconds run together. */
        String freeBsdUptime() {
            final long seconds = this.uptimeTicks / 20L;
            final long h = seconds / 3600L;
            final long m = seconds / 60L % 60L;
            final long s = seconds % 60L;
            if (h > 0) {
                return h + "h" + m + "m" + s + "s";
            }
            return m > 0 ? m + "m" + s + "s" : s + "s";
        }
    }
}
