/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.SystemProcesses;
import dev.jstech.computers.os.install.Installers;
import dev.jstech.computers.os.crash.CrashPage;
import dev.jstech.computers.os.crash.CrashPages;
import dev.jstech.computers.registry.HardwareRegistry;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The crash a machine's system went down on: the page on its glass, until when it stands, and what follows it.
 *
 * <p>Its clock is its own, beside the closing-down's: a system that crashed while its machine still makes a computer
 * goes down and starts over on the same clock, but one whose part was taken out from under it has no computer left to
 * close down, and its red page still stands its time before the monitor goes dark. Transient, all of it: a world
 * saved during a crash comes back with the machine starting over or dark, as the crash left it.
 */
final class MachineCrash {

    @Nullable
    private CrashPage page;
    private long endsAt;
    private boolean endsDark;
    private boolean held;

    /** The system goes down on {@code page} now; the monitor goes dark after it when {@code endsDark}. */
    void begin(final CrashPage page, final long now, final boolean endsDark) {
        this.page = page;
        this.endsAt = now + page.totalTicks();
        this.endsDark = endsDark;
        this.held = false;
    }

    /** The page standing on the glass at {@code now}, or null when there is none or its time is up. */
    @Nullable
    CrashPage page(final long now) {
        return this.page != null && (this.held || now < this.endsAt) ? this.page : null;
    }

    /** The ticks the page still stands, none while a key holds it. */
    int remaining(final long now) {
        return page(now) == null || this.held ? 0 : (int) Math.max(0L, this.endsAt - now);
    }

    boolean held() {
        return this.held;
    }

    boolean endsDark() {
        return this.endsDark;
    }

    /**
     * A key was pressed on the console: the first holds a page that offers to stop its reboot, the next lets it go.
     * Answers whether anything changed.
     */
    boolean key(final long now) {
        final CrashPage shown = page(now);
        if (shown == null || !shown.holdsOnKey()) {
            return false;
        }
        if (!this.held) {
            this.held = true;
            return true;
        }
        this.held = false;
        this.endsAt = now;
        return true;
    }

    /**
     * What the machine knows of itself at the moment it goes down, for the kernels to print.
     *
     * @param system the system that came up, which a pulled disk no longer shows but which is still the one going down
     */
    static CrashPages.Facts facts(final IOsHost machine, final OsDef system, final long upFor,
                                  final CrashPages.Moment moment) {
        ItemStack board = ItemStack.EMPTY;
        ItemStack cpu = ItemStack.EMPTY;
        for (final ItemStack part : machine.hardwareStacks()) {
            if (board.isEmpty() && HardwareRegistry.board(part) != null) {
                board = part;
            } else if (cpu.isEmpty() && HardwareRegistry.cpu(part) != null) {
                cpu = part;
            }
        }
        final String cpuName = cpu.isEmpty() ? "" : cpu.getHoverName().getString();
        // The processor's maker is the first word of its name, which is how this world names every one of them.
        final String vendor = cpuName.isEmpty() ? "Velocion" : cpuName.split(" ", 2)[0];
        return CrashPages.Facts.of(Installers.hostName(machine), system.displayName(),
                board.isEmpty() ? "" : board.getHoverName().getString(), vendor,
                SystemProcesses.initName(system.platform(), system.packageManager()), upFor,
                machine.ramTotalMb() * 1_048_576L, moment);
    }
}
