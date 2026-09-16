/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.machine.NetworkReadService;
import dev.jstech.computers.os.Branding;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * What each system has to say while it comes up, read off the machine it is coming up on.
 *
 * <p>Nothing here is written in advance. A drive letter is a drive that is in the machine, the memory is the
 * memory that is seated, and a network step reports what answered, or that nothing did. A system that says it
 * found something it did not find is worse than a system that says nothing, so a step that cannot be answered
 * says what actually happened instead.
 */
public final class BootLines {

    /** What a machine of the earliest age reserves below the line, in kilobytes, as those machines did. */
    private static final int BASE_MEMORY_KB = 640;

    private BootLines() {
    }

    /** The sequence this machine's system shows while it comes up. */
    public static BootSequence forMachine(final AbstractComputerBlockEntity machine) {
        final OsDef system = machine.installedOs();
        if (system == null) {
            return BootSequence.NONE;
        }
        final HardwareEra era = machine.installedEra();
        final String copyright = Branding.systemCopyright(system.displayName(),
                era != null ? era : HardwareEra.STANDARD);
        return switch (system.platform()) {
            case MC_DOS -> dos(machine, system, copyright);
            case MC_NET -> net(machine, system, copyright);
            default -> new BootSequence.Builder().title(system.displayName()).subtitle(copyright).build();
        };
    }

    /**
     * The earliest machines: memory counted above the line, then a letter for every drive that is in, then the
     * network only when a cable actually reaches one.
     */
    private static BootSequence dos(final AbstractComputerBlockEntity machine, final OsDef system,
                                    final String copyright) {
        final BootSequence.Builder out = new BootSequence.Builder()
                .title("Starting " + system.displayName() + "...")
                .subtitle(copyright);
        final int extendedKb = Math.max(0, machine.ramTotalMb() * 1024 - BASE_MEMORY_KB);
        out.line("HIMEM", String.format(Locale.ROOT, "%,d KB extended memory", extendedKb));
        char letter = 'C';
        for (int slot = 0; slot < machine.diskSlots(); slot++) {
            final ItemStack disk = machine.diskInSlot(slot);
            if (!(disk.getItem() instanceof DiskItem)) {
                continue;
            }
            out.line(letter + ":", disk.getHoverName().getString());
            letter++;
        }
        final String network = networkName(machine);
        if (!network.isEmpty()) {
            out.line("NET", network);
        }
        return out.build();
    }

    /**
     * The network machines: every step is a question put to the network, and an unanswered one says so rather
     * than opening on a list with nothing in it and no reason given.
     */
    private static BootSequence net(final AbstractComputerBlockEntity machine, final OsDef system,
                                    final String copyright) {
        final BootSequence.Builder out = new BootSequence.Builder()
                .title(system.displayName() + " 1.0")
                .subtitle(copyright);
        final NetworkReadService network = machine.services().network();
        final ICliComputer.NetSummary summary = network == null ? null : network.summary();
        if (summary == null || !summary.linked()) {
            out.line("network link", "down");
            out.line("mainframe", "skipped");
            out.line("index", "skipped");
            out.line("storage", "skipped");
            return out.build();
        }
        out.line("network link", "done");
        if (!summary.mainframePresent()) {
            out.line("mainframe", "none answering");
            out.line("index", "not available");
            out.line("storage", "not available");
            return out.build();
        }
        out.line("mainframe", network.current());
        out.line("index", String.format(Locale.ROOT, "%,d item types", summary.indexedTypes()));
        final long capacity = network.capacity();
        final long used = network.used();
        final int percent = capacity > 0 ? (int) (used * 100L / capacity) : 0;
        out.line("storage", summary.servers() + (summary.servers() == 1 ? " server, " : " servers, ")
                + percent + "% full");
        return out.build();
    }

    /** The network this machine is on, by the name it answers to, or nothing when no cable reaches one. */
    private static String networkName(final AbstractComputerBlockEntity machine) {
        final NetworkReadService network = machine.services().network();
        if (network == null || !network.online()) {
            return "";
        }
        final String name = network.current();
        return name == null ? "" : name;
    }
}
