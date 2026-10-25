/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.operation.payload.crash.MemoryShortPayload;
import dev.jstech.computers.os.GraphicsPrograms;
import dev.jstech.computers.os.KernelDef;
import dev.jstech.computers.os.MachineMemory;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.SchedulerKind;
import dev.jstech.computers.os.VramLedger;
import dev.jstech.computers.os.crash.OomKiller;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.text.GameText;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The machine's memory as its desktop sees it: the system, its desktop and its services hold their share (reserved,
 * from the server), and every open window holds its program's weight under the running system, by the same rule the
 * server applies. A window that does not fit brings a Frames machine down (the machine is told, and it crashes onto its
 * page and starts over), opens on a Unix one whose kernel then ends what holds the most, and is refused by the earliest
 * systems, which say so in a balloon.
 */
final class DesktopMemory {

    private final DesktopState desktop;
    private final ResourceLocation osId;
    /** The machine's era, which says what each window holds; null when it has none to tell. */
    @Nullable
    private final Era era;
    /** The desktop's windows, read to weigh what is open. */
    private final List<DesktopWindow> windows;
    private final int totalMb;
    private final int reservedMb;
    /** The installed programs the machine built from source, by id path, whose windows hold a little less. */
    private final Set<String> sourceBuilt = new HashSet<>();
    /** Whether the warning of a memory past nine tenths has been raised since it last dropped back under. */
    private boolean warned;

    DesktopMemory(final DesktopState desktop, final ResourceLocation osId, @Nullable final Era era,
                  final List<DesktopWindow> windows, final int totalMb, final int reservedMb) {
        this.desktop = desktop;
        this.osId = osId;
        this.era = era;
        this.windows = windows;
        this.totalMb = totalMb;
        this.reservedMb = reservedMb;
    }

    /** Takes the programs the machine built from source, as its listing brings them. */
    void takeSourceBuilt(final Collection<String> ids) {
        sourceBuilt.clear();
        sourceBuilt.addAll(ids);
    }

    /** The megabytes a window opened under {@code key} holds: its program's weight on a machine of this one's era. */
    int windowRamMb(final String key) {
        return MachineMemory.windowRamMb(key, era, spec -> sourceBuilt.contains(spec.id().getPath()));
    }

    /** Everything held right now: the system's share, its desktop and services, and the open windows. */
    int usedMb() {
        int sum = reservedMb;
        for (final DesktopWindow w : windows) {
            // A dialog is part of its program, not another copy of it.
            if (!w.dialog()) {
                sum += windowRamMb(w.appKey());
            }
        }
        return sum;
    }

    int totalMb() {
        return totalMb;
    }

    /** The memory meter's text, "used/total MB", shared by the panels so they can keep room for it. */
    String meterText() {
        return usedMb() + "/" + totalMb + " MB";
    }

    /**
     * Whether a window of {@code key} may open now, that is whether its program's weight still fits in the free
     * memory. One that does not brings every edition of Frames down, a Unix kernel lets it open and ends what holds the
     * most once the machine hears of it, and the earliest systems refuse it with the figures. A preemptive Frames that
     * a window takes past nine tenths of its memory says so first, naming what holds the most.
     */
    boolean allowOpen(final String key) {
        if (!videoMemoryAllows(key)) {
            return false;
        }
        final int need = windowRamMb(key);
        final int free = totalMb - usedMb();
        if (need <= free) {
            warnIfLow(key, need);
            return true;
        }
        if (killsForMemory()) {
            return true;
        }
        if (frames()) {
            // Frames does not refuse a program it has no room for: it goes down, and the machine puts its page up.
            PacketDistributor.sendToServer(new MemoryShortPayload(desktop.host(), desktop.nameOf(key)));
        } else {
            /*
             * A refusal the machine can simply report: the desktop is still there, so a balloon says it the way the
             * notification area always did, instead of taking the screen over with a dialog.
             */
            desktop.notices().showBalloon(GameText.resolve(DesktopTexts.LOW_MEMORY),
                    GameText.resolve(DesktopTexts.LOW_MEMORY_BODY.with(desktop.nameOf(key), need,
                            Math.max(0, free))));
        }
        return false;
    }

    /**
     * Whether a window of {@code key} fits in the machine's video memory: only a graphics program's window holds any,
     * a quarter of a monitor block of the machine's era, beside what the lit monitors and the other graphics windows
     * already hold. One that does not fit does not open, and the notification area says why.
     */
    boolean videoMemoryAllows(final String key) {
        if (!GraphicsPrograms.isGraphical(key)) {
            return true;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null
                || !(minecraft.level.getBlockEntity(desktop.host()) instanceof AbstractComputerBlockEntity machine)) {
            return true;
        }
        final Era era = machine.displayEra() == null ? HardwareEra.STANDARD : machine.displayEra();
        long held = machine.vramMonitorsKb();
        for (final DesktopWindow w : windows) {
            if (!w.dialog() && GraphicsPrograms.isGraphical(w.appKey())) {
                held += VramLedger.windowKb(era, w.maximized());
            }
        }
        final long need = VramLedger.windowKb(era, false);
        final long free = machine.vramTotalKb() - held;
        if (need <= free) {
            return true;
        }
        desktop.notices().showBalloon(GameText.resolve(DesktopTexts.LOW_VIDEO_MEMORY),
                GameText.resolve(DesktopTexts.LOW_VIDEO_MEMORY_BODY.with(desktop.nameOf(key), VramLedger.label(need),
                        VramLedger.label(Math.max(0L, free)))));
        return false;
    }

    /**
     * Past nine tenths of the memory, a preemptive Frames warns once, naming the three programs that hold the most and
     * what each holds, the warning the real ones raised before they ran out. Frames 95's cooperative kernel has no
     * such warning: it goes down without one. The warning comes again only after the memory has dropped back under.
     */
    private void warnIfLow(final String key, final int need) {
        final int after = usedMb() + need;
        if (after * 10L < totalMb * 9L) {
            this.warned = false;
            return;
        }
        if (this.warned || !frames() || cooperative()) {
            return;
        }
        this.warned = true;
        final Map<String, Integer> held = new HashMap<>();
        for (final DesktopWindow w : windows) {
            if (!w.dialog()) {
                held.merge(w.appKey(), windowRamMb(w.appKey()), Integer::sum);
            }
        }
        held.merge(key, need, Integer::sum);
        final List<Map.Entry<String, Integer>> byWeight = new ArrayList<>(held.entrySet());
        byWeight.sort(Map.Entry.<String, Integer>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey()));
        final List<String> heaviest = new ArrayList<>();
        for (final Map.Entry<String, Integer> one : byWeight.subList(0, Math.min(3, byWeight.size()))) {
            heaviest.add(GameText.resolve(DesktopTexts.LOW_MEMORY_HOLDS.with(desktop.nameOf(one.getKey()),
                    one.getValue())));
        }
        desktop.notices().showBalloon(GameText.resolve(DesktopTexts.LOW_MEMORY),
                GameText.resolve(DesktopTexts.LOW_MEMORY_WARNING.with(String.join(", ", heaviest))));
    }

    /** Whether the system is an edition of Frames, which goes down rather than refuse a program. */
    private boolean frames() {
        final OsDef os = OsRegistry.getOs(osId);
        return os != null && os.platform() == Platform.FRAMES;
    }

    /** Whether the system's kernel ends programs for their memory, and so lets every window open first. */
    private boolean killsForMemory() {
        final OsDef os = OsRegistry.getOs(osId);
        return os != null && OomKiller.runsOn(os.platform());
    }

    /** A cooperative kernel (Frames 95's) has no memory protection, and no warning before it goes down. */
    private boolean cooperative() {
        final OsDef os = OsRegistry.getOs(osId);
        final KernelDef kernel = os == null ? null : OsRegistry.getKernel(os.kernelId());
        return kernel != null && kernel.scheduler() == SchedulerKind.COOPERATIVE;
    }
}
