/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import dev.jstech.computers.os.ProgramKind;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class InstallMediaTest {

    @Test
    public void forSystem_followsTheEra() {
        assertEquals(MediaFormat.FLOPPY, InstallMedia.forSystem(HardwareEra.VINTAGE));
        assertEquals(MediaFormat.CD, InstallMedia.forSystem(HardwareEra.LEGACY));
        assertEquals(MediaFormat.DVD, InstallMedia.forSystem(HardwareEra.TRANSITION));
        assertEquals(MediaFormat.USB, InstallMedia.forSystem(HardwareEra.STANDARD));
        assertEquals(MediaFormat.USB, InstallMedia.forSystem(HardwareEra.ADVANCED));
    }

    @Test
    public void forSystem_laterErasStayOnTheStick() {
        for (final Era era : HardwareEra.SERIES) {
            if (era.isAtLeast(HardwareEra.STANDARD)) {
                assertEquals(MediaFormat.USB, InstallMedia.forSystem(era), era.id());
            }
        }
    }

    @Test
    public void forProgram_vintageAndLegacyIgnoreTheKind() {
        for (final ProgramKind kind : ProgramKind.values()) {
            assertEquals(MediaFormat.FLOPPY, InstallMedia.forProgram(HardwareEra.VINTAGE, kind), kind.name());
            assertEquals(MediaFormat.CD, InstallMedia.forProgram(HardwareEra.LEGACY, kind), kind.name());
            assertEquals(MediaFormat.DVD, InstallMedia.forProgram(HardwareEra.TRANSITION, kind), kind.name());
        }
    }

    @Test
    public void forProgram_advancedPutsApplicationsOnBluRayAndServicesOnTheStick() {
        assertEquals(MediaFormat.USB, InstallMedia.forProgram(HardwareEra.ADVANCED, ProgramKind.SERVICE));
        assertEquals(MediaFormat.BLU_RAY, InstallMedia.forProgram(HardwareEra.ADVANCED, ProgramKind.APP));
        assertEquals(MediaFormat.BLU_RAY,
                InstallMedia.forProgram(HardwareEra.ADVANCED, ProgramKind.DESKTOP_ENVIRONMENT));
    }

    @Test
    public void forProgram_standardSplitsServicesFromApplications() {
        assertEquals(MediaFormat.USB, InstallMedia.forProgram(HardwareEra.STANDARD, ProgramKind.SERVICE));
        assertEquals(MediaFormat.DVD, InstallMedia.forProgram(HardwareEra.STANDARD, ProgramKind.APP));
        assertEquals(MediaFormat.DVD, InstallMedia.forProgram(HardwareEra.STANDARD, ProgramKind.DESKTOP_ENVIRONMENT));
    }

    @Test
    public void readerName_namesEveryFormat() {
        for (final MediaFormat format : MediaFormat.values()) {
            assertEquals(false, InstallMedia.readerName(format).english().isBlank(), format.name());
        }
        assertEquals("Dock Station", InstallMedia.readerName(MediaFormat.USB).english());
        assertEquals("Blu-ray Drive", InstallMedia.readerName(MediaFormat.BLU_RAY).english());
    }

    @Test
    public void bluRay_isTheAdvancedDiscOfTwentyFiveGigabytes() {
        assertEquals(HardwareEra.ADVANCED, MediaFormat.BLU_RAY.era());
        assertEquals(25_025_314_816L, MediaFormat.BLU_RAY.capacityBytes());
        assertEquals("Blu-ray", MediaFormat.BLU_RAY.text().english());
    }

    @Test
    public void capacityBytes_isWhatEachMediumWasSoldWith() {
        assertEquals(1_474_560L, MediaFormat.FLOPPY.capacityBytes());
        assertEquals(737_280_000L, MediaFormat.CD.capacityBytes());
        assertEquals(4_700_372_992L, MediaFormat.DVD.capacityBytes());
        assertEquals(32_000_000_000L, MediaFormat.USB.capacityBytes());
    }

    @Test
    public void capacityBytes_growsFromTheFloppyDiskThroughEachDisc() {
        final MediaFormat[] line = {MediaFormat.FLOPPY, MediaFormat.CD, MediaFormat.DVD, MediaFormat.BLU_RAY};
        for (int i = 1; i < line.length; i++) {
            assertEquals(true, line[i].capacityBytes() > line[i - 1].capacityBytes(), line[i].name());
        }
    }
}
