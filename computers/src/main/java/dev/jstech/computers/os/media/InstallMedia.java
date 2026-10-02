/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import dev.jstech.computers.os.ProgramKind;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;

/**
 * Which physical medium a piece of software ships on. One axis, the generation the software belongs
 * to: Vintage on a floppy, Legacy on a CD, Transition on a DVD, and the Standard era split the way that decade
 * did it, a bootable flash drive for the system and the daemons that run it, a DVD for the applications a
 * player opens; the Advanced era keeps the stick for those and puts the applications on a Blu-ray disc. Size never
 * decides: a small server daemon is not a floppy program because it is small.
 *
 * <p>Pure, so the rule is unit-tested and the creative tab, the tooltip and the installer projection
 * all read the same answer.
 */
@TextHolder
public final class InstallMedia {

    private static final TextKey FLOPPY_DRIVE = TextKey.of("jsc.media.install_media.floppy_drive", "Floppy Drive");
    private static final TextKey CD_DRIVE = TextKey.of("jsc.media.install_media.cd_drive", "CD Drive");
    private static final TextKey DVD_DRIVE = TextKey.of("jsc.media.install_media.dvd_drive", "DVD Drive");
    private static final TextKey DOCK_STATION = TextKey.of("jsc.media.install_media.dock_station", "Dock Station");
    private static final TextKey BLU_RAY_DRIVE = TextKey.of("jsc.media.install_media.blu_ray_drive", "Blu-ray Drive");

    private InstallMedia() {
    }

    /** The medium an operating system of {@code era} ships on. */
    public static MediaFormat forSystem(final HardwareEra era) {
        return switch (era) {
            case VINTAGE -> MediaFormat.FLOPPY;
            case LEGACY -> MediaFormat.CD;
            // The systems of the late 2000s came on a DVD, before a stick could boot one.
            case TRANSITION -> MediaFormat.DVD;
            case STANDARD, ADVANCED, EXA, SINGULARITY -> MediaFormat.USB;
        };
    }

    /** The medium a program written in {@code era} ships on; a headless service goes on the stick. */
    public static MediaFormat forProgram(final HardwareEra era, final ProgramKind kind) {
        return switch (era) {
            case VINTAGE -> MediaFormat.FLOPPY;
            case LEGACY -> MediaFormat.CD;
            case TRANSITION -> MediaFormat.DVD;
            case STANDARD -> kind.runsInBackground() ? MediaFormat.USB : MediaFormat.DVD;
            case ADVANCED, EXA, SINGULARITY -> kind.runsInBackground() ? MediaFormat.USB : MediaFormat.BLU_RAY;
        };
    }

    /** The drive a medium of {@code format} is read in, for the words on a tooltip. */
    public static Text readerName(final MediaFormat format) {
        return switch (format) {
            case FLOPPY -> FLOPPY_DRIVE.text();
            case CD -> CD_DRIVE.text();
            case DVD -> DVD_DRIVE.text();
            case USB -> DOCK_STATION.text();
            case BLU_RAY -> BLU_RAY_DRIVE.text();
        };
    }
}
