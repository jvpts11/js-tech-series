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
import java.util.List;

/**
 * The two ways to a kernel, which sound nothing alike.
 *
 * <p>The kernel builder is quiet in life. It writes the compile to a log and shows only the phase it is in,
 * so a build of many minutes is a dozen lines, each appearing when its phase starts, and a closing remark
 * that a kernel that will not boot is not its fault. Building by hand is the one that scrolls: every object
 * the kernel is made of, named as it is compiled, then the link, then the image. Both are here because both
 * are real, and which one to watch is the player's to choose.
 */
public final class KernelVoices {

    private static final String[][] TREE = {
        {"arch/x86/kernel", "apic/apic cpu/common cpu/intel cpu/amd e820 head64 irq process setup signal smpboot "
                + "time traps tsc"},
        {"arch/x86/mm", "fault init ioremap pgtable tlb"},
        {"block", "bio blk-core blk-mq blk-settings elevator genhd"},
        {"crypto", "aes_generic api crc32c_generic sha256_generic"},
        {"drivers/ata", "ahci libata-core libata-scsi"},
        {"drivers/base", "bus class core dd driver platform"},
        {"drivers/nvme/host", "core pci"},
        {"drivers/pci", "access bus probe quirks"},
        {"drivers/scsi", "scsi scsi_lib scsi_scan sd"},
        {"fs", "dcache exec file inode namei namespace open read_write super"},
        {"fs/ext4", "balloc dir extents file ialloc inode mballoc namei super"},
        {"fs/proc", "array base generic meminfo root"},
        {"init", "do_mounts initramfs main version"},
        {"kernel", "exit fork kthread module panic signal sys workqueue"},
        {"kernel/sched", "core cputime deadline fair idle rt wait"},
        {"kernel/time", "clocksource hrtimer tick-sched timekeeping timer"},
        {"lib", "bitmap idr kobject radix-tree rbtree string vsprintf xarray"},
        {"mm", "filemap memory mmap oom_kill page_alloc slub swap vmalloc vmscan"},
        {"net/core", "dev filter neighbour skbuff sock"},
        {"net/ipv4", "af_inet arp icmp ip_input ip_output route tcp tcp_input tcp_output udp"},
        {"security", "commoncap security"},
    };

    /** The directories built as modules rather than into the kernel, which the build marks as it goes. */
    private static final String[][] MODULES = {
        {"drivers/net/ethernet/intel/e1000e", "ethtool ich8lan netdev phy"},
        {"drivers/usb/core", "hcd hub message urb usb"},
        {"fs/fat", "cache dir fatent file inode namei_vfat"},
    };

    /** What the build says once every object is made: the link, the symbol table, and the image. */
    private static final List<String> LINKING = List.of("  AR      built-in.a", "  AR      vmlinux.a",
            "  LD      vmlinux.o", "  OBJCOPY modules.builtin.modinfo", "  GEN     modules.builtin",
            "  MODPOST Module.symvers", "  CC      .vmlinux.export.o", "  UPD     include/generated/utsversion.h",
            "  CC      init/version-timestamp.o", "  KSYMS   .tmp_vmlinux0.kallsyms.S", "  LD      vmlinux",
            "  NM      System.map", "  SORTTAB vmlinux", "  CC      arch/x86/boot/version.o",
            "  VOFFSET arch/x86/boot/compressed/../voffset.h", "  OBJCOPY arch/x86/boot/compressed/vmlinux.bin",
            "  ZSTD22  arch/x86/boot/compressed/vmlinux.bin.zst", "  LD      arch/x86/boot/compressed/vmlinux",
            "  ZOFFSET arch/x86/boot/zoffset.h", "  OBJCOPY arch/x86/boot/vmlinux.bin",
            "  AS      arch/x86/boot/header.o", "  LD      arch/x86/boot/setup.elf",
            "  OBJCOPY arch/x86/boot/setup.bin", "  BUILD   arch/x86/boot/bzImage");

    /** One object in so many is a directory's archive being closed rather than a file being compiled. */
    private static final int ARCHIVE_EVERY = 19;

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
        final TtyScript.Builder script = TtyScript.script()
                .say(Tint.line(Tint.green("* "), Tint.bright("Gentoo Linux Genkernel"), "; Version 4.3.16"))
                .say(star("Using genkernel configuration from '/etc/genkernel.conf' ..."))
                .say(star("Running with options: all"))
                .say("")
                .say(Tint.line(Tint.green("* "), "Working with Linux kernel ", Tint.bright(full), " for ",
                        Tint.bright("x86_64")))
                .pause(share(ticks, 1))
                .say(star("Using kernel config file '/usr/share/genkernel/arch/x86_64/generated-config' ..."))
                .say("");
        phase(script, "kernel: >> Initializing ...", share(ticks, 1));
        phase(script, "        >> Running 'make mrproper' ...", share(ticks, 4));
        phase(script, "        >> Running 'make oldconfig' ...", share(ticks, 5));
        phase(script, "        >> Compiling " + full + " bzImage ...", share(ticks, 38));
        phase(script, "        >> Compiling " + full + " modules ...", share(ticks, 27));
        phase(script, "        >> Installing " + full + " modules (and stripping) ...", share(ticks, 5));
        phase(script, "        >> Generating module dependency data ...", share(ticks, 2));
        script.say("");
        phase(script, "initramfs: >> Initializing ...", share(ticks, 1));
        phase(script, "        >> Appending devices cpio data ...", share(ticks, 1));
        phase(script, "        >> Appending base_layout cpio data ...", share(ticks, 1));
        phase(script, "        >> Appending busybox cpio data ...", share(ticks, 5));
        phase(script, "        >> Appending modules cpio data ...", share(ticks, 3));
        phase(script, "        >> Deduping cpio ...", share(ticks, 1));
        phase(script, "        >> Compressing cpio data (.xz) ...", share(ticks, 5));
        return script.say("")
                .effect(built)
                .say(star("Kernel compiled successfully!"))
                .say(star(""))
                .say(star("--no-bootloader set; Skipping bootloader update ..."))
                .say(star(""))
                .say(star("Required kernel parameter:"))
                .say(star(""))
                .say(star("    root=/dev/$ROOT"))
                .say(star(""))
                .say(Tint.line(Tint.yellow("* "), "WARNING... WARNING... WARNING..."))
                .say(Tint.line(Tint.yellow("* "), "Additional kernel parameters that *may* be required to boot"))
                .say(Tint.line(Tint.yellow("* "), "properly:"))
                .say(star(""))
                .say(star("Do NOT report kernel bugs as genkernel bugs unless your bug"))
                .say(star("is about the default genkernel configuration..."))
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
        return TtyScript.script()
                .say("  SYNC    include/config/auto.conf")
                .say("  HOSTCC  scripts/basic/fixdep")
                .say("  HOSTCC  scripts/kconfig/conf.o")
                .say("  UPD     include/generated/compile.h")
                .flood(compiling, objects, KernelVoices::object)
                .flood(share(ticks, 9), LINKING.size(), index -> CliLine.plain(LINKING.get(index)))
                .say("Kernel: arch/x86/boot/bzImage is ready  (#1)")
                .flood(share(ticks, 5), MODULES.length + 2, index -> CliLine.plain(index < MODULES.length
                        ? "  INSTALL /lib/modules/" + release + "-gentoo/kernel/" + MODULES[index][0] + "/"
                                + lastOf(MODULES[index][0]) + ".ko"
                        : index == MODULES.length ? "  DEPMOD  /lib/modules/" + release + "-gentoo"
                                : "  INSTALL /boot"))
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
            return CliLine.plain("  AR      " + TREE[Math.floorMod(mixed, TREE.length)][0] + "/built-in.a");
        }
        final boolean module = Math.floorMod(mixed, 7) == 0;
        final String[][] from = module ? MODULES : TREE;
        final String[] dir = from[Math.floorMod(mixed >>> 3, from.length)];
        final String[] files = dir[1].split(" ");
        return CliLine.plain((module ? "  CC [M]  " : "  CC      ") + dir[0] + "/"
                + files[Math.floorMod(mixed >>> 9, files.length)] + ".o");
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

    private static void phase(final TtyScript.Builder script, final String text, final int ticks) {
        script.say(star(text)).pause(ticks);
    }

    private static CliLine star(final String text) {
        return Tint.line(Tint.green("* "), text);
    }

    private static int share(final int ticks, final int hundredths) {
        return Math.max(1, ticks * hundredths / 100);
    }
}
