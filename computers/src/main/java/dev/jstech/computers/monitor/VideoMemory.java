/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.monitor;

import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.block.MonitorPanel;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.hardware.FormFactor;
import dev.jstech.computers.os.GraphicsPrograms;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OpenWindow;
import dev.jstech.computers.os.VramLedger;
import dev.jstech.core.gui.Tube;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.util.Loaded;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * What a computer's video memory goes to: its graphics windows, then its monitors in the order they were linked,
 * each lit while it fits beside what is already held. Worked out from the machine at most once a tick, since every
 * monitor of it asks.
 *
 * <p>The memory is its cards', each cut by a slot older than itself; a machine with no card whose processor carries
 * graphics on the die shares the system's own memory instead, up to a quarter of it, as such graphics did. A monitor
 * that does not fit is told how much it needs and how much is free, and stays dark until there is room; a window is
 * counted first because a graphics program open on the desktop is already holding its memory.
 */
public final class VideoMemory {

    /* At most this much of the system's memory goes to graphics on a processor's die, as a quarter of it at most. */
    private static final int SHARED_MOST_MB = 1792;
    private static final Map<IOsHost, Worked> WORKED = new WeakHashMap<>();

    private VideoMemory() {
    }

    /** One monitor's share: the monitor, what it needs, whether it is lit, and how much was free for it. */
    public record Screen(long pos, long needKb, boolean fits, long freeKb) {
    }

    /** The ledger of what is held, and every linked monitor's share in link order. */
    public record State(VramLedger ledger, List<Screen> screens, boolean shared) {

        /** That monitor's share, or null when it is not linked to this machine. */
        public Screen screen(final long pos) {
            for (final Screen screen : screens) {
                if (screen.pos() == pos) {
                    return screen;
                }
            }
            return null;
        }
    }

    /** What that machine's video memory goes to this tick. */
    public static State of(final ServerLevel level, final IOsHost host) {
        final long tick = level.getGameTime();
        final Worked worked = WORKED.get(host);
        if (worked != null && worked.tick() == tick) {
            return worked.state();
        }
        final State state = work(level, host);
        WORKED.put(host, new Worked(tick, state));
        return state;
    }

    /**
     * The memory a machine built as {@code build} has for graphics, in kilobytes, with {@code ramMb} of RAM. A server
     * board's console output has memory of its own, enough for one monitor of the board's era, as its management
     * controller's graphics had.
     */
    public static long totalKb(final ComputerBuild build, final int ramMb) {
        final long console = build.motherboard().formFactor() == FormFactor.EEB
                ? VramLedger.monitorKbPerBlock(build.motherboard().era(), true) : 0L;
        if (shares(build)) {
            return console + Math.min(ramMb / 4, SHARED_MOST_MB) * VramLedger.KB_PER_MB;
        }
        return console + build.effectiveVramMb() * VramLedger.KB_PER_MB;
    }

    /** Whether a machine so built draws its screens with its processor's graphics, from the system's memory. */
    public static boolean shares(final ComputerBuild build) {
        if (!build.gpus().isEmpty()) {
            return false;
        }
        for (final CpuSpec cpu : build.cpus()) {
            if (cpu.hasIntegratedGraphics()) {
                return true;
            }
        }
        return false;
    }

    /** What a lit monitor holds: its kind's block, times the monitors of its big screen. */
    public static long monitorKb(final MonitorBlockEntity monitor) {
        if (!(monitor.getBlockState().getBlock() instanceof MonitorBlock block)) {
            return 0L;
        }
        final MonitorPanel panel = monitor.panel();
        final int blocks = panel == null ? 1 : panel.width() * panel.height();
        return blocks * VramLedger.monitorKbPerBlock(block.era(), block.kind().tube() == Tube.SIXTEEN
                || !block.kind().tube().monochrome());
    }

    /** The megabytes of the system's RAM a machine sharing it for graphics is holding for them right now. */
    public static int sharedHeldMb(final ServerLevel level, final IOsHost host) {
        final State state = of(level, host);
        return state.shared() ? (int) ((state.ledger().usedKb() + VramLedger.KB_PER_MB - 1) / VramLedger.KB_PER_MB)
                : 0;
    }

    private static State work(final ServerLevel level, final IOsHost host) {
        final ComputerBuild build = host.currentBuild();
        if (build == null || !host.isRunning()) {
            return new State(new VramLedger(0L), List.of(), false);
        }
        final VramLedger ledger = new VramLedger(totalKb(build, host.ramTotalMb()));
        final HardwareEra era = host.displayEra() == null ? HardwareEra.STANDARD : host.displayEra();
        for (final OpenWindow window : host.openWindows()) {
            if (GraphicsPrograms.isGraphical(window.key()) && !window.minimized()) {
                ledger.add(window.key(), VramLedger.windowKb(era, window.maximized()), VramLedger.Kind.WINDOW);
            }
        }
        final List<Screen> screens = new ArrayList<>();
        final Map<Long, Boolean> seen = new HashMap<>();
        for (final long endpoint : host.linkedEndpoints()) {
            if (seen.put(endpoint, Boolean.TRUE) != null
                    || !(Loaded.blockEntity(level, BlockPos.of(endpoint)) instanceof MonitorBlockEntity monitor)
                    || host.isDisabled(endpoint)) {
                continue;
            }
            final long need = monitorKb(monitor);
            final long free = ledger.freeKb();
            final boolean fits = ledger.fits(need);
            if (fits) {
                ledger.add("monitor:" + endpoint, need, VramLedger.Kind.MONITOR);
            }
            screens.add(new Screen(endpoint, need, fits, free));
        }
        return new State(ledger, List.copyOf(screens), shares(build));
    }

    private record Worked(long tick, State state) {
    }
}
