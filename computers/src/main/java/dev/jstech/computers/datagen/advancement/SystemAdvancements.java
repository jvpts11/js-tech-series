/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datagen.advancement;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.advancement.JscEvents;
import net.minecraft.advancements.Criterion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The operating systems: each one's first install, the things done at their prompts and desktops, and the
 * challenges of the hard ways in. A first install is the first time an installed system comes up in front of
 * somebody, which is marked on the disk, so erasing it and installing again counts again.
 */
public final class SystemAdvancements extends AdvancementTab {

    private static final List<String> DISTRIBUTIONS = List.of("ubuntu", "debian", "fedora", "arch", "gentoo");
    /* Where screenfetch installs: every distribution and FreeBSD. UNIX has no package for it. */
    private static final List<String> SCREENFETCH_SYSTEMS =
            List.of("ubuntu", "debian", "fedora", "arch", "gentoo", "freebsd");

    public SystemAdvancements() {
        super("systems", ResourceLocation.withDefaultNamespace("textures/block/deepslate_tiles.png"));
        this.root(ComputingModule.CD_ROM.get(), "Operating Systems", "Boot a computer into an operating system",
                bootedAny());

        this.task("abort_retry_fail", "root", ComputingModule.FLOPPY_DISK.get(), "Abort, Retry, Fail?",
                "Install MC-DOS and boot it", booted("mc_dos"));
        this.task("feels_like_the_old_days", "abort_retry_fail", ComputingModule.ETHERNET_CABLE_ITEM.get(),
                "Feels like the old days", "Install MC-NET and boot it", booted("mc_net"));

        this.task("start_me_up", "root", ComputingModule.CD_ROM.get(), "Start Me Up",
                "Install Frames 95 and boot it", booted("frames_95"));
        this.task("the_goat", "start_me_up", ComputingModule.DVD_ROM.get(), "The goat",
                "Install Frames XP and boot it", booted("frames_xp"));
        this.task("bloat_11", "the_goat", ComputingModule.USB_FLASH_DRIVE.get(), "More like, Bloat 11",
                "Install Frames 11 and boot it", booted("frames_11"));

        this.task("where_it_all_started", "root", ComputingModule.VINTAGE_PERSONAL_COMPUTER_ITEM.get(),
                "Where it all started long ago...", "Install UNIX and boot it", booted("unix"));
        this.task("not_linux", "where_it_all_started", ComputingModule.LEGACY_PERSONAL_COMPUTER_ITEM.get(),
                "It's Not Linux, Stop Asking", "Install FreeBSD and boot it", booted("freebsd"));
        this.challenge("clockwork", "where_it_all_started", Items.CLOCK, "Clockwork",
                "Have cron run a job on UNIX on Vintage hardware", on(JscEvents.CLOCKWORK));

        this.task("bloat_everywhere", "root", ComputingModule.DVD_ROM.get(), "Bloat everywhere, but linux",
                "Install Ubuntu and boot it", booted("ubuntu"));
        this.task("universal", "bloat_everywhere", ComputingModule.DVD_ROM.get(),
                "The Universal Operating System", "Install Debian and boot it", booted("debian"));
        this.task("freedom", "universal", ComputingModule.DVD_ROM.get(), "Freedom, Friends, Features, First",
                "Install Fedora and boot it", booted("fedora"));
        this.challenge("i_use_arch_btw", "freedom", ComputingModule.USB_FLASH_DRIVE.get(), "I Use Arch BTW",
                "Install Arch Linux by hand from the live medium and boot it", booted("arch"));
        this.challenge("recompile", "i_use_arch_btw", ComputingModule.CD_ROM.get(), "Didn't Like It? Recompile!",
                "Build Gentoo from source and boot it", booted("gentoo"));
        final Map<String, Supplier<Criterion<?>>> everyDistribution = new LinkedHashMap<>();
        for (final String distribution : DISTRIBUTIONS) {
            everyDistribution.put(distribution, booted(distribution));
        }
        this.challengeOfAll("distro_hopper", "recompile", Items.RABBIT_FOOT, "Distro Hopper",
                "Install and boot all five distributions", everyDistribution);
        final Map<String, Supplier<Criterion<?>>> everyScreenfetch = new LinkedHashMap<>();
        for (final String system : SCREENFETCH_SYSTEMS) {
            everyScreenfetch.put(system, on(JscEvents.SCREENFETCH, system));
        }
        this.challengeOfAll("professional_larper", "distro_hopper", Items.PAINTING, "Professional Larper",
                "Run screenfetch on every system it installs on", everyScreenfetch);

        this.task("press_del", "root", ComputingModule.MOTHERBOARD_ATX_P.get(), "Press DEL to Enter Setup",
                "Open a computer's firmware setup", on(JscEvents.FIRMWARE_SETUP));
        this.goal("best_of_both_worlds", "press_del", HardwareItems.DISK_TRENCH_20M.get(),
                "The Best of Both Worlds", "Boot one of two systems sharing a disk from the boot menu",
                on(JscEvents.DUAL_BOOT));
        this.secret("format_c", "press_del", Items.LAVA_BUCKET, "Format C:",
                "Erase a disk that held an installed system", on(JscEvents.SYSTEM_ERASED));

        this.task("rtfm", "root", Items.BOOK, "RTFM", "Open a manual page", on(JscEvents.MAN_PAGE));
        this.task("sudo_make_me_a_sandwich", "rtfm", Items.BREAD, "sudo make me a sandwich",
                "Install a package from the Mirror", on(JscEvents.MIRROR_INSTALL));
        this.task("not_responding", "root", Items.BARRIER, "Not Responding",
                "End a program from the Task Manager", on(JscEvents.TASK_ENDED));
        this.challenge("professional_procrastinator", "not_responding", Items.TNT, "Professional Procrastinator",
                "Win Minesweeper on the Expert board", on(JscEvents.MINESWEEPER_EXPERT));
        this.task("im_in", "root", Items.ENDER_EYE, "I'm In",
                "Take control of another computer through Remote Control", on(JscEvents.REMOTE_CONTROL));
    }
}
