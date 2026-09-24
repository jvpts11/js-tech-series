/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install.voice;

import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.tty.TtyScript;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Locale;

/**
 * The two ways to a kernel, which sound nothing alike.
 *
 * <p>The kernel builder is quiet in life. It writes the compile to a log and shows only the phase it is in,
 * so a build of many minutes is a dozen lines, each appearing when its phase starts, and a closing remark
 * that a kernel that will not boot is not its fault. Building by hand is the one that scrolls: every object
 * the kernel is made of, named as it is compiled, then the link, then the image. Both are here because both
 * are real, and which one to watch is the player's to choose.
 *
 * <p>The kernel builder's remarks are the player's language. The build's own lines, a step and a file each, are
 * the build log, which reads the same in every language.
 */
@TextHolder
public final class KernelVoices {

    /** Each directory built into the kernel, then the objects compiled in it. */
    private static final String[][] TREE = {
        {"arch/x86/kernel", "apic/apic", "cpu/common", "cpu/intel", "cpu/amd", "e820", "head64", "irq", "process",
            "setup", "signal", "smpboot", "time", "traps", "tsc"},
        {"arch/x86/mm", "fault", "init", "ioremap", "pgtable", "tlb"},
        {"block", "bio", "blk-core", "blk-mq", "blk-settings", "elevator", "genhd"},
        {"crypto", "aes_generic", "api", "crc32c_generic", "sha256_generic"},
        {"drivers/ata", "ahci", "libata-core", "libata-scsi"},
        {"drivers/base", "bus", "class", "core", "dd", "driver", "platform"},
        {"drivers/nvme/host", "core", "pci"},
        {"drivers/pci", "access", "bus", "probe", "quirks"},
        {"drivers/scsi", "scsi", "scsi_lib", "scsi_scan", "sd"},
        {"fs", "dcache", "exec", "file", "inode", "namei", "namespace", "open", "read_write", "super"},
        {"fs/ext4", "balloc", "dir", "extents", "file", "ialloc", "inode", "mballoc", "namei", "super"},
        {"fs/proc", "array", "base", "generic", "meminfo", "root"},
        {"init", "do_mounts", "initramfs", "main", "version"},
        {"kernel", "exit", "fork", "kthread", "module", "panic", "signal", "sys", "workqueue"},
        {"kernel/sched", "core", "cputime", "deadline", "fair", "idle", "rt", "wait"},
        {"kernel/time", "clocksource", "hrtimer", "tick-sched", "timekeeping", "timer"},
        {"lib", "bitmap", "idr", "kobject", "radix-tree", "rbtree", "string", "vsprintf", "xarray"},
        {"mm", "filemap", "memory", "mmap", "oom_kill", "page_alloc", "slub", "swap", "vmalloc", "vmscan"},
        {"net/core", "dev", "filter", "neighbour", "skbuff", "sock"},
        {"net/ipv4", "af_inet", "arp", "icmp", "ip_input", "ip_output", "route", "tcp", "tcp_input", "tcp_output",
            "udp"},
        {"security", "commoncap", "security"},
    };

    /** The directories built as modules rather than into the kernel, which the build marks as it goes. */
    private static final String[][] MODULES = {
        {"drivers/net/ethernet/intel/e1000e", "ethtool", "ich8lan", "netdev", "phy"},
        {"drivers/usb/core", "hcd", "hub", "message", "urb", "usb"},
        {"fs/fat", "cache", "dir", "fatent", "file", "inode", "namei_vfat"},
    };

    /** What the build says before it compiles anything: its settings brought up to date and its own tools built. */
    private static final List<CliLine> PREPARING = List.of(step("SYNC", "include/config/auto.conf"),
            step("HOSTSCC", "scripts/basic/fixdep"), step("HOSTSCC", "scripts/kconfig/conf.asm"),
            step("UPD", "include/generated/compile.sg"));

    /**
     * What the build says once every object is made: the link, the symbol table, and the image.
     *
     * <p>The kernel of this world is written in Sigma, so what compiles it is the Sigma compiler, what goes in
     * is a {@code .sg} source and what comes out is the {@code .asm} listing a machine runs.
     */
    private static final List<CliLine> LINKING = List.of(step("AR", "built-in.a"), step("AR", "vmlinux.a"),
            step("LD", "vmlinux.asm"), step("OBJCOPY", "modules.builtin.modinfo"), step("GEN", "modules.builtin"),
            step("MODPOST", "Module.symvers"), step("SCC", ".vmlinux.export.asm"),
            step("UPD", "include/generated/utsversion.sg"), step("SCC", "init/version-timestamp.asm"),
            step("KSYMS", ".tmp_vmlinux0.kallsyms.asm"), step("LD", "vmlinux"), step("NM", "System.map"),
            step("SORTTAB", "vmlinux"), step("SCC", "arch/x86/boot/version.asm"),
            step("VOFFSET", "arch/x86/boot/compressed/../voffset.sg"),
            step("OBJCOPY", "arch/x86/boot/compressed/vmlinux.bin"),
            step("ZSTD22", "arch/x86/boot/compressed/vmlinux.bin.zst"),
            step("LD", "arch/x86/boot/compressed/vmlinux"), step("ZOFFSET", "arch/x86/boot/zoffset.sg"),
            step("OBJCOPY", "arch/x86/boot/vmlinux.bin"), step("AS", "arch/x86/boot/header.asm"),
            step("LD", "arch/x86/boot/setup.elf"), step("OBJCOPY", "arch/x86/boot/setup.bin"),
            step("BUILD", "arch/x86/boot/bzImage"));

    /** One object in so many is a directory's archive being closed rather than a file being compiled. */
    private static final int ARCHIVE_EVERY = 19;

    /** The kernel builder's name and version, which it opens with in every language. */
    private static final Text GENKERNEL = Text.literal("Gentoo Linux Genkernel");
    private static final Text GENKERNEL_VERSION = Text.literal("; Version 4.3.16");

    /** Where the kernel builder keeps its settings, and the kernel settings it builds with. */
    private static final String GENKERNEL_CONF = "/etc/genkernel.conf";
    private static final String KERNEL_CONFIG = "/usr/share/genkernel/arch/x86_64/generated-config";

    /** The machine the kernel is built for. */
    private static final String ARCH = "x86_64";

    private static final TextKey USING_CONFIGURATION = TextKey.of("jsc.install.kernel_voices.using_configuration",
            "Using genkernel configuration from '%s' ...");
    private static final TextKey RUNNING_WITH =
            TextKey.of("jsc.install.kernel_voices.running_with", "Running with options: %s");
    /* The kernel and the machine it is built for stand out in their own colour, so the line is split round them. */
    private static final TextKey WORKING_WITH =
            TextKey.of("jsc.install.kernel_voices.working_with", "Working with Linux kernel");
    private static final TextKey WORKING_FOR = TextKey.of("jsc.install.kernel_voices.working_for", "for");
    private static final TextKey USING_KERNEL_CONFIG = TextKey.of("jsc.install.kernel_voices.using_kernel_config",
            "Using kernel config file '%s' ...");
    private static final TextKey INITIALIZING =
            TextKey.of("jsc.install.kernel_voices.initializing", "Initializing ...");
    private static final TextKey RUNNING = TextKey.of("jsc.install.kernel_voices.running", "Running '%s' ...");
    private static final TextKey COMPILING_IMAGE =
            TextKey.of("jsc.install.kernel_voices.compiling_image", "Compiling %s bzImage ...");
    private static final TextKey COMPILING_MODULES =
            TextKey.of("jsc.install.kernel_voices.compiling_modules", "Compiling %s modules ...");
    private static final TextKey INSTALLING_MODULES = TextKey.of("jsc.install.kernel_voices.installing_modules",
            "Installing %s modules (and stripping) ...");
    private static final TextKey GENERATING_DEPENDENCIES = TextKey.of(
            "jsc.install.kernel_voices.generating_dependencies", "Generating module dependency data ...");
    private static final TextKey APPENDING =
            TextKey.of("jsc.install.kernel_voices.appending", "Appending %s cpio data ...");
    private static final TextKey DEDUPING = TextKey.of("jsc.install.kernel_voices.deduping", "Deduping cpio ...");
    private static final TextKey COMPRESSING =
            TextKey.of("jsc.install.kernel_voices.compressing", "Compressing cpio data (%s) ...");
    private static final TextKey COMPILED =
            TextKey.of("jsc.install.kernel_voices.compiled", "Kernel compiled successfully!");
    private static final TextKey SKIPPING_BOOTLOADER = TextKey.of("jsc.install.kernel_voices.skipping_bootloader",
            "%s set; Skipping bootloader update ...");
    private static final TextKey REQUIRED_PARAMETER =
            TextKey.of("jsc.install.kernel_voices.required_parameter", "Required kernel parameter:");
    private static final TextKey WARNING =
            TextKey.of("jsc.install.kernel_voices.warning", "WARNING... WARNING... WARNING...");
    /* The builder's closing remarks, a line to a key as they are broken over the terminal. */
    private static final TextKey ADDITIONAL_1 = TextKey.of("jsc.install.kernel_voices.additional_1",
            "Additional kernel parameters that *may* be required to boot");
    private static final TextKey ADDITIONAL_2 = TextKey.of("jsc.install.kernel_voices.additional_2", "properly:");
    private static final TextKey NOT_OUR_BUGS_1 = TextKey.of("jsc.install.kernel_voices.not_our_bugs_1",
            "Do NOT report kernel bugs as genkernel bugs unless your bug");
    private static final TextKey NOT_OUR_BUGS_2 = TextKey.of("jsc.install.kernel_voices.not_our_bugs_2",
            "is about the default genkernel configuration...");

    private KernelVoices() {
    }

    /**
     * The kernel builder: each phase named as it starts, the long ones taking the share of the time they take.
     *
     * @param release the kernel it is building, without the distribution's own suffix
     * @param ticks   how long this machine's processor takes over it
     * @param built   what having a kernel means to the machine, done at the end and not before
     */
    public static TtyScript genkernel(final String release, final int ticks, final Runnable built) {
        final String full = release + "-gentoo";
        /* Each phase is led by the part of the build it belongs to, or lined up under the first that named it. */
        final String under = "        >> ";
        final TtyScript.Builder script = TtyScript.script()
                .say(Tint.line(Tint.green("* "), Tint.bright(GENKERNEL), GENKERNEL_VERSION))
                .say(star(USING_CONFIGURATION.with(GENKERNEL_CONF)))
                .say(star(RUNNING_WITH.with(Text.literal("all"))))
                .say("")
                .say(Tint.line(Tint.green("* "), WORKING_WITH, " ", Tint.bright(full), " ", WORKING_FOR, " ",
                        Tint.bright(ARCH)))
                .pause(share(ticks, 1))
                .say(star(USING_KERNEL_CONFIG.with(KERNEL_CONFIG)))
                .say("");
        phase(script, "kernel: >> ", INITIALIZING.text(), share(ticks, 1));
        phase(script, under, RUNNING.with(Text.literal("make mrproper")), share(ticks, 4));
        phase(script, under, RUNNING.with(Text.literal("make oldconfig")), share(ticks, 5));
        phase(script, under, COMPILING_IMAGE.with(full), share(ticks, 38));
        phase(script, under, COMPILING_MODULES.with(full), share(ticks, 27));
        phase(script, under, INSTALLING_MODULES.with(full), share(ticks, 5));
        phase(script, under, GENERATING_DEPENDENCIES.text(), share(ticks, 2));
        script.say("");
        phase(script, "initramfs: >> ", INITIALIZING.text(), share(ticks, 1));
        phase(script, under, APPENDING.with("devices"), share(ticks, 1));
        phase(script, under, APPENDING.with("base_layout"), share(ticks, 1));
        phase(script, under, APPENDING.with("busybox"), share(ticks, 5));
        phase(script, under, APPENDING.with("modules"), share(ticks, 3));
        phase(script, under, DEDUPING.text(), share(ticks, 1));
        phase(script, under, COMPRESSING.with(".xz"), share(ticks, 5));
        return script.say("")
                .effect(built)
                .say(star(COMPILED.text()))
                .say(star(Text.EMPTY))
                .say(star(SKIPPING_BOOTLOADER.with(Text.literal("--no-bootloader"))))
                .say(star(Text.EMPTY))
                .say(star(REQUIRED_PARAMETER.text()))
                .say(star(Text.EMPTY))
                .say(star(Text.literal("    root=/dev/$ROOT")))
                .say(star(Text.EMPTY))
                .say(Tint.line(Tint.yellow("* "), WARNING))
                .say(Tint.line(Tint.yellow("* "), ADDITIONAL_1))
                .say(Tint.line(Tint.yellow("* "), ADDITIONAL_2))
                .say(star(Text.EMPTY))
                .say(star(NOT_OUR_BUGS_1.text()))
                .say(star(NOT_OUR_BUGS_2.text()))
                .done();
    }

    /**
     * Building by hand: every object named as it is compiled, the link, the image, then the modules and the
     * install.
     *
     * @param jobs  how many are compiled at once, which the build options decide and the cores cap
     * @param ticks how long this machine's processor takes over it
     */
    public static TtyScript make(final String release, final int jobs, final int ticks, final Runnable built) {
        final int compiling = share(ticks, 86);
        final int objects = Math.min(compiling * TtyScript.MAX_LINES_PER_TICK, Math.max(120, compiling * 2));
        final String modules = "/lib/modules/" + release + "-gentoo";
        return TtyScript.script()
                .sayAll(PREPARING)
                .flood(compiling, objects, KernelVoices::object)
                .flood(share(ticks, 9), LINKING.size(), LINKING::get)
                .say(Text.literal("Kernel: arch/x86/boot/bzImage is ready  (#1)"))
                .flood(share(ticks, 5), MODULES.length + 2, index -> index < MODULES.length
                        ? step("INSTALL", modules + "/kernel/" + MODULES[index][0] + "/" + lastOf(MODULES[index][0])
                                + ".ko")
                        : index == MODULES.length ? step("DEPMOD", modules) : step("INSTALL", "/boot"))
                .effect(built)
                .done();
    }

    /**
     * The object compiled at a place in the build.
     *
     * <p>Worked out from the place alone, so the build reads the same every time it is played and a line is
     * only ever made when somebody is there to read it.
     */
    private static CliLine object(final int index) {
        final int mixed = mix(index);
        if (index % ARCHIVE_EVERY == ARCHIVE_EVERY - 1) {
            return step("AR", TREE[Math.floorMod(mixed, TREE.length)][0] + "/built-in.a");
        }
        final boolean module = Math.floorMod(mixed, 7) == 0;
        final String[][] from = module ? MODULES : TREE;
        final String[] dir = from[Math.floorMod(mixed >>> 3, from.length)];
        final String file = dir[1 + Math.floorMod(mixed >>> 9, dir.length - 1)];
        return step(module ? "SCC [M]" : "SCC", dir[0] + "/" + file + ".asm");
    }

    /**
     * One line of the build log: the step, set in a column of its own, and the file it is done to. The log reads
     * the same in every language.
     */
    private static CliLine step(final String step, final String file) {
        return CliLine.plain(Text.literal(String.format(Locale.ROOT, "  %-7s %s", step, file)));
    }

    private static int mix(final int index) {
        int hash = index * 0x9E3779B1;
        hash ^= hash >>> 15;
        hash *= 0x85EBCA6B;
        return hash ^ (hash >>> 13);
    }

    private static String lastOf(final String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /** A phase named as it starts, after its lead, and the time it takes before the next one is named. */
    private static void phase(final TtyScript.Builder script, final String lead, final Text text, final int ticks) {
        script.say(Tint.line(Tint.green("* "), lead, text)).pause(ticks);
    }

    private static CliLine star(final Text text) {
        return Tint.line(Tint.green("* "), text);
    }

    private static int share(final int ticks, final int hundredths) {
        return Math.max(1, ticks * hundredths / 100);
    }
}
