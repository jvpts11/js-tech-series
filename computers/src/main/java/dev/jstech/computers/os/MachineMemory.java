/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.program.ComputerConsoleState;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * What a machine's memory holds: the running system's own share, the desktop package it booted, the services it is
 * running, the programs it is running and the windows it has open, each weighed under the installed system.
 *
 * <p>Worked out from the machine each time it is asked, since every one of those can change between two questions,
 * and a ledger kept up to date by hand is a ledger that drifts.
 */
public final class MachineMemory {

    private MachineMemory() {
    }

    /** That machine's ledger. A machine that is off or has no system holds nothing. */
    public static RamLedger ledgerOf(final IOsHost host) {
        final RamLedger ledger = new RamLedger(host.ramTotalMb());
        final OsDef os = host.installedOs();
        if (os == null || !host.isRunning()) {
            return ledger;
        }
        ledger.add(os.displayName(), os.ramMb(), RamLedger.Kind.SYSTEM);
        final ComputerConsoleState console = host.console();
        final ResourceLocation desktopId = host.bootedDesktopId();
        final DesktopEnvironmentDef desktop = desktopId != null ? OsRegistry.getDesktop(desktopId) : null;
        if (desktopId != null && !desktopId.equals(os.id())) {
            // A desktop package on a Linux system; a Frames desktop is the system itself and is counted above.
            final ProgramSpec pack = OsRegistry.getProgram(desktopId);
            if (pack != null) {
                ledger.add(desktop != null ? desktop.displayName() : pack.displayName(),
                        pack.ramMbOn(os, builtHere(console, pack)), RamLedger.Kind.DESKTOP);
            }
        }
        if (console != null) {
            for (final ProgramSpec spec : OsRegistry.programs()) {
                /*
                 * By the whole id, which is how every install path writes it down. Asking by the path alone
                 * matched nothing, so no service ever weighed anything here and none of them was listed as
                 * running, whatever the player had installed.
                 */
                if (spec.kind() == ProgramKind.SERVICE && console.isInstalled(spec.id().toString())
                        && host.serviceRunning(spec)) {
                    ledger.add(spec.displayName(), spec.ramMbOn(os, builtHere(console, spec)),
                            RamLedger.Kind.SERVICE);
                }
            }
        }
        final MachinePrograms scripts = host.programs();
        if (scripts != null) {
            for (final var one : scripts.view()) {
                ledger.add(one.name(), one.heapMb(), one.heldBytes(), RamLedger.Kind.PROCESS, one.id());
            }
        }
        for (final OpenWindow window : host.openWindows()) {
            ledger.add(window.key(), windowRamMb(window.key(), os, desktop, spec -> builtHere(console, spec)),
                    RamLedger.Kind.WINDOW);
        }
        return ledger;
    }

    /** Everything the session holds before the player opens a window: the system, its desktop, its services. */
    public static int reservedMb(final IOsHost host) {
        final RamLedger ledger = host.ramLedger();
        return ledger.usedMb() - ledger.usedMb(RamLedger.Kind.WINDOW);
    }

    /**
     * The leading windows of {@code windows} that fit beside everything else that machine holds, in order; the
     * first past the budget and everything after it are dropped, so the oldest windows survive.
     */
    public static List<OpenWindow> withinBudget(final IOsHost host, final List<OpenWindow> windows) {
        final OsDef os = host.installedOs();
        if (os == null) {
            return windows;
        }
        final ResourceLocation desktopId = host.bootedDesktopId();
        final DesktopEnvironmentDef desktop = desktopId != null ? OsRegistry.getDesktop(desktopId) : null;
        final ComputerConsoleState console = host.console();
        return RamLedger.withinBudget(windows,
                window -> windowRamMb(window.key(), os, desktop, spec -> builtHere(console, spec)),
                host.ramTotalMb() - host.ramReservedMb());
    }

    /**
     * The megabytes a window opened under {@code key} holds: its program's weight under {@code os}, found by the
     * label the desktop gives the program, else by the program's own name; a window no program answers to weighs
     * what a bundled program of that system does.
     *
     * @param builtHere which programs the machine built from source, which hold a little less
     */
    public static int windowRamMb(final String key, final OsDef os, @Nullable final DesktopEnvironmentDef desktop,
                                  final Predicate<ProgramSpec> builtHere) {
        ProgramSpec spec = desktop != null ? desktop.programFor(key) : null;
        if (spec == null) {
            for (final ProgramSpec candidate : OsRegistry.programs()) {
                if (candidate.displayName().equals(key)) {
                    spec = candidate;
                    break;
                }
            }
        }
        return spec != null ? spec.ramMbOn(os, builtHere.test(spec)) : RamLedger.bundledWeightMb(os.ramMb());
    }

    /** Whether the machine whose console that is built that program from source; nothing is, with no console. */
    private static boolean builtHere(@Nullable final ComputerConsoleState console, final ProgramSpec spec) {
        return console != null && console.builtFromSource(spec.id().toString());
    }
}
