/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.desktop;

import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.ExperienceIndex;
import dev.jstech.computers.os.DesktopEffects;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.program.ComputerSettings;
import dev.jstech.core.tier.HardwareEra;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * A machine's visual effects as its desktop runs them: the ones its owner switched off, and whether its graphics can
 * run any at all. Only the desktops that drew their effects on the graphics card ask: Frames 7 and later, Cinnamon,
 * KDE from its fourth version and GNOME from its third. A desktop that drew everything on the processor shows the
 * same on any card.
 */
public final class MachineEffects {

    /** The desktops whose effects need the graphics card on every machine they run on. */
    private static final Set<String> ALWAYS = Set.of("frames_7", "frames_10", "frames_11", "cinnamon");
    /** The desktops whose effects need it from an era on: KDE 4 at Transition, GNOME 3 at Standard. */
    private static final String KDE = "kde_plasma";
    private static final String GNOME = "gnome";

    private MachineEffects() {
    }

    /** The effects that machine's desktop runs, from the settings it keeps and the graphics it has. */
    public static DesktopEffects of(final IOsHost computer, final ComputerSettings settings) {
        final DesktopEffects chosen = new DesktopEffects(List.copyOf(settings.effectsOff()), settings.effectSpeed());
        return chosen.withBasic(basic(computer));
    }

    /** How the machine rates, from the parts it is built of; the lowest scores when it is not built of parts. */
    public static ExperienceIndex indexOf(final IOsHost computer) {
        final ComputerBuild build = computer.currentBuild();
        return build == null ? ExperienceIndex.NONE : ExperienceIndex.of(build, computer.ramTotalMb());
    }

    /*
     * Whether the desktop runs its basic look: one whose effects need the card, on a machine built of parts whose
     * graphics score under what they need. A machine not built of parts keeps its effects, since nothing says it is
     * weak.
     */
    private static boolean basic(final IOsHost computer) {
        final ResourceLocation desktop = computer.installedDesktopId();
        if (desktop == null || computer.currentBuild() == null || !needsGraphics(desktop, computer.displayEra())) {
            return false;
        }
        return !indexOf(computer).runsEffects();
    }

    private static boolean needsGraphics(final ResourceLocation desktop, final HardwareEra era) {
        final String name = desktop.getPath();
        if (ALWAYS.contains(name)) {
            return true;
        }
        if (era == null) {
            return false;
        }
        return name.equals(KDE) && era.isAtLeast(HardwareEra.TRANSITION)
                || name.equals(GNOME) && era.isAtLeast(HardwareEra.STANDARD);
    }
}
