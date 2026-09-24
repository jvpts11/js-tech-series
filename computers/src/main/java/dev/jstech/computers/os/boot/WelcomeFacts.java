/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.install.Installers;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * What a machine says about itself the first time one of its systems comes up.
 *
 * <p>Every line of it is read off the machine at the moment it is asked for: the name it goes by, the processor
 * that is really in it, the memory that is really counted, which disk carries which system, and whether a cable
 * reaches a Mainframe with a Mirror on it. A welcome that told a player something untrue about their own computer
 * would be worse than no welcome, which is why none of this is written in advance.
 *
 * <p>The tips are the same promise in smaller print: each one is a thing a player can go and do on this machine
 * as it stands, so the ones about the network only appear when there is a network, and the one about installing
 * software says where software comes from when no Mirror is answering.
 */
@TextHolder
public final class WelcomeFacts {

    /** The most tips a welcome walks through, which is as many as there are true things worth saying. */
    public static final int MOST_TIPS = 8;

    /** What the machine calls the welcome's window when it puts one up, and what closes it again. */
    public static final String WINDOW_KEY = "Welcome";

    private static final TextKey TIP_NO_MIRROR = TextKey.of("jsc.boot.welcome_facts.tip_no_mirror",
            "pckmgr at the Command Prompt installs programs, once a Mainframe on this network runs the Mirror.");
    private static final TextKey TIP_MIRROR = TextKey.of("jsc.boot.welcome_facts.tip_mirror",
            "pckmgr at the Command Prompt installs programs from the Mirror on %s. Try pckmgr search.");
    private static final TextKey TIP_THIS_PC = TextKey.of("jsc.boot.welcome_facts.tip_this_pc",
            "This PC shows the disks in this computer and what each one holds.");
    private static final TextKey TIP_HOSTNAME = TextKey.of("jsc.boot.welcome_facts.tip_hostname",
            "This computer's name, %s, is its hostname: other machines on the network reach it by that name.");
    private static final TextKey TIP_NETWORK = TextKey.of("jsc.boot.welcome_facts.tip_network",
            "Network shows the machines and the storage on the network this computer is cabled to.");
    private static final TextKey TIP_TASK_MANAGER = TextKey.of("jsc.boot.welcome_facts.tip_task_manager",
            "Right-click the taskbar to open the Task Manager.");
    private static final TextKey TIP_ESC = TextKey.of("jsc.boot.welcome_facts.tip_esc",
            "ESC closes the monitor; the computer keeps running.");

    private WelcomeFacts() {
    }

    /** Whether the system this machine boots greets anybody at all; the terminal systems do not. */
    public static boolean greeter(final IOsHost machine) {
        final OsDef system = machine.installedOs();
        return system != null && system.platform() == Platform.FRAMES;
    }

    /** The other systems installed on this machine, on the disks they sit on, without the one that booted. */
    public static List<Other> others(final IOsHost machine) {
        final List<Other> others = new ArrayList<>();
        final ResourceLocation booted = machine.installedOsId();
        for (int slot = 0; slot < machine.diskSlots(); slot++) {
            final ItemStack disk = machine.diskInSlot(slot);
            if (!(disk.getItem() instanceof DiskItem)) {
                continue;
            }
            final ResourceLocation osId = OsDisks.systemOn(disk);
            if (osId == null || osId.equals(booted)) {
                continue;
            }
            final OsDef other = OsRegistry.getOs(osId);
            if (other != null) {
                others.add(new Other(other.displayName(), slot));
            }
        }
        return others;
    }

    /** The slot the system that booted is on, or -1 when nothing carries one. */
    public static int bootedSlot(final IOsHost machine) {
        return OsDisks.systemDiskSlot(machine.diskSlots(), machine::diskInSlot, machine.bootDiskSlot());
    }

    /** The drive the system that booted is on, as it is written on it. */
    public static String bootedDisk(final IOsHost machine) {
        final int slot = bootedSlot(machine);
        return slot < 0 ? "" : machine.diskInSlot(slot).getHoverName().getString();
    }

    /**
     * The things a player can go and do on this machine right now, each one true of it as it stands.
     *
     * @param mirrorHost the Mirror answering this machine, empty when none does
     * @param networked  whether a data cable reaches a network at all
     */
    public static List<Text> tips(final IOsHost machine, final String mirrorHost, final boolean networked) {
        final List<Text> tips = new ArrayList<>();
        tips.add(mirrorHost.isEmpty() ? TIP_NO_MIRROR.text() : TIP_MIRROR.with(mirrorHost));
        tips.add(TIP_THIS_PC.text());
        final String name = machine.console() == null ? "" : machine.console().computerName();
        if (!name.isBlank()) {
            tips.add(TIP_HOSTNAME.with(name));
        }
        if (networked) {
            tips.add(TIP_NETWORK.text());
        }
        tips.add(TIP_TASK_MANAGER.text());
        tips.add(TIP_ESC.text());
        return tips.size() <= MOST_TIPS ? tips : tips.subList(0, MOST_TIPS);
    }

    /** The Mirror answering this machine, by the name its Mainframe goes by; empty when none answers. */
    public static String mirrorHost(final IOsHost machine, final ServerLevel level) {
        return Installers.mirrorHost(machine, level);
    }

    /**
     * Another system on another disk of the same machine.
     *
     * @param system the system by the name a person reads
     * @param slot   the disk it is on
     */
    public record Other(String system, int slot) {
    }
}
