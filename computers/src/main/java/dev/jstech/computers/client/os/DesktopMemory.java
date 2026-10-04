/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.os.GraphicsPrograms;
import dev.jstech.computers.os.KernelDef;
import dev.jstech.computers.os.MachineMemory;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.SchedulerKind;
import dev.jstech.computers.os.VramLedger;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.text.GameText;
import dev.jstech.core.tier.HardwareEra;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The machine's memory as its desktop sees it: the system, its desktop and its services hold their share (reserved,
 * from the server), and every open window holds its program's weight under the running system, by the same rule the
 * server applies. A window that does not fit is refused by a preemptive kernel, which says so in a balloon, and
 * brings a cooperative one (Frames 95's) down: the crash screen, then a reboot to an empty session.
 */
final class DesktopMemory {

    private final DesktopState desktop;
    private final ResourceLocation osId;
    /** The desktop's windows, read to weigh what is open. */
    private final List<DesktopWindow> windows;
    private final int totalMb;
    private final int reservedMb;
    /** The installed programs the machine built from source, by id path, whose windows hold a little less. */
    private final Set<String> sourceBuilt = new HashSet<>();
    /** True while a cooperative kernel shows its crash screen, until the reboot. */
    private boolean crashing;
    private long crashUntil;

    /** How long the crash screen stays up before the machine reboots, in milliseconds. */
    private static final long CRASH_MS = 4200L;

    DesktopMemory(final DesktopState desktop, final ResourceLocation osId, final List<DesktopWindow> windows,
                  final int totalMb, final int reservedMb) {
        this.desktop = desktop;
        this.osId = osId;
        this.windows = windows;
        this.totalMb = totalMb;
        this.reservedMb = reservedMb;
    }

    /** Takes the programs the machine built from source, as its listing brings them. */
    void takeSourceBuilt(final Collection<String> ids) {
        sourceBuilt.clear();
        sourceBuilt.addAll(ids);
    }

    /** The megabytes a window opened under {@code key} holds: its program's weight under the running system. */
    int windowRamMb(final String key) {
        final OsDef os = OsRegistry.getOs(osId);
        return os == null ? 0
                : MachineMemory.windowRamMb(key, os, spec -> sourceBuilt.contains(spec.id().getPath()));
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
     * memory; otherwise a preemptive system refuses with the figures and a cooperative one crashes.
     */
    boolean allowOpen(final String key) {
        if (crashing || !videoMemoryAllows(key)) {
            return false;
        }
        final int need = windowRamMb(key);
        final int free = totalMb - usedMb();
        if (need <= free) {
            return true;
        }
        if (cooperative()) {
            crashing = true;
            crashUntil = System.currentTimeMillis() + CRASH_MS;
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
        final HardwareEra era = machine.displayEra() == null ? HardwareEra.STANDARD : machine.displayEra();
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

    /** Whether the crash screen is up. */
    boolean crashing() {
        return crashing;
    }

    /** Whether the crash screen has been up long enough for the machine to reboot. */
    boolean crashOver() {
        return crashing && System.currentTimeMillis() >= crashUntil;
    }

    /** Clears the crash once the machine has rebooted. */
    void recover() {
        crashing = false;
    }

    /** The cooperative kernel's crash screen: a classic blue fatal-error page, drawn in desktop-local coordinates. */
    void renderCrash(final GuiGraphics g, final int sw, final int sh) {
        final Font font = desktop.textFont();
        final DesktopShellPalette.Colours c = DesktopShellPalette.get();
        g.fill(0, 0, sw, sh, c.crashGround());
        final int cy = sh / 3;
        final String head = " Frames ";
        final int hw = font.width(head) + 6;
        g.fill((sw - hw) / 2, cy - 2, (sw + hw) / 2, cy + 10, c.crashBand());
        Draw.text(g, font, head, (sw - font.width(head)) / 2, cy, c.crashGround());
        final List<String> lines = new ArrayList<>();
        lines.add(GameText.resolve(DesktopTexts.CRASH_FATAL));
        // The cause is written over two lines of the screen, where the language breaks it.
        lines.addAll(List.of(GameText.resolve(DesktopTexts.CRASH_CAUSE).split("\n")));
        lines.add("");
        lines.add(GameText.resolve(DesktopTexts.CRASH_NO_RECOVERY));
        lines.add(GameText.resolve(DesktopTexts.REBOOTING));
        int ly = cy + 20;
        for (final String s : lines) {
            Draw.text(g, font, s, (sw - font.width(s)) / 2, ly, c.crashInk());
            ly += 11;
        }
    }

    /** A cooperative kernel (Frames 95's) has no memory protection: overloading it crashes the whole desktop. */
    private boolean cooperative() {
        final OsDef os = OsRegistry.getOs(osId);
        final KernelDef kernel = os == null ? null : OsRegistry.getKernel(os.kernelId());
        return kernel != null && kernel.scheduler() == SchedulerKind.COOPERATIVE;
    }
}
