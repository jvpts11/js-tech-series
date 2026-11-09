/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import dev.jstech.computers.wireless.WirelessStandard;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableNames;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.text.Text;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * The twelve portable devices (computers/docs/design/wireless.md), each with the hardware of its era: its system on a
 * chip (how many cores at what clock), its memory and its storage, the Hightide Pocket Pro's memory card beside it, and
 * its screen, in the GUI's pixels, as it shows held up. Its name is its item's.
 *
 * <p>Pure: no part of the game.
 */
public enum PortableModel implements IStableName {

    HIGHTIDE_POCKET("hightide_pocket", PortableKind.PDA, HardwareEra.LEGACY, MobileSystem.FRAMES_POCKET_2002, 1, 400,
            64, 64, 0, 144, 192),
    HIGHTIDE_POCKET_PRO("hightide_pocket_pro", PortableKind.PDA, HardwareEra.TRANSITION,
            MobileSystem.FRAMES_POCKET_2007, 1, 624, 128, 256, 2_048, 144, 192),
    QPHONE("qphone", PortableKind.SMARTPHONE, HardwareEra.TRANSITION, MobileSystem.QOS_2007, 1, 412, 128, 8_192, 0,
            128, 192),
    MOTOROLLER_LANDMARK("motoroller_landmark", PortableKind.SMARTPHONE, HardwareEra.TRANSITION,
            MobileSystem.DROIDIX_2009, 1, 550, 256, 16_384, 0, 108, 192),
    QPHONE_5("qphone_5", PortableKind.SMARTPHONE, HardwareEra.STANDARD, MobileSystem.QOS_2013, 2, 1_300, 1_024, 32_768,
            0, 108, 192),
    HIGHTIDE_WAVE("hightide_wave", PortableKind.SMARTPHONE, HardwareEra.STANDARD, MobileSystem.DROIDIX_2014, 4, 2_300,
            2_048, 32_768, 0, 117, 208),
    QPAD_AIR("qpad_air", PortableKind.TABLET, HardwareEra.STANDARD, MobileSystem.QOS_2013, 3, 1_500, 2_048, 65_536, 0,
            180, 240),
    SUNSUNG_NEBULA_TAB("sunsung_nebula_tab", PortableKind.TABLET, HardwareEra.STANDARD, MobileSystem.DROIDIX_2014, 2,
            1_000, 1_024, 16_384, 0, 320, 200),
    QPHONE_X("qphone_x", PortableKind.SMARTPHONE, HardwareEra.ADVANCED, MobileSystem.QOS_2017, 6, 2_400, 3_072,
            65_536, 0, 112, 243),
    SUNSUNG_NEBULA_S10("sunsung_nebula_s10", PortableKind.SMARTPHONE, HardwareEra.ADVANCED, MobileSystem.DROIDIX_2019,
            8, 2_700, 8_192, 131_072, 0, 112, 236),
    QPAD_PRO("qpad_pro", PortableKind.TABLET, HardwareEra.ADVANCED, MobileSystem.QOS_2017, 8, 3_200, 8_192, 262_144, 0,
            196, 280),
    SUNSUNG_NEBULA_TAB_S7("sunsung_nebula_tab_s7", PortableKind.TABLET, HardwareEra.ADVANCED,
            MobileSystem.DROIDIX_2019, 8, 3_100, 6_144, 131_072, 0, 352, 220);

    private final String serializedName;
    private final PortableKind kind;
    private final Era era;
    private final MobileSystem system;
    private final int cores;
    private final int clockMhz;
    private final int memoryMb;
    private final int storageMb;
    private final int cardMb;
    private final int screenWidth;
    private final int screenHeight;

    private static final StableNames<PortableModel> NAMES = StableNames.of(PortableModel.class);
    /* A megabyte count this large or larger is written in gigabytes. */
    private static final int MB_A_GB = 1_024;
    private static final double MHZ_A_GHZ = 1_000.0;

    PortableModel(final String serializedName, final PortableKind kind, final Era era, final MobileSystem system,
                  final int cores, final int clockMhz, final int memoryMb, final int storageMb, final int cardMb,
                  final int screenWidth, final int screenHeight) {
        this.serializedName = serializedName;
        this.kind = kind;
        this.era = era;
        this.system = system;
        this.cores = cores;
        this.clockMhz = clockMhz;
        this.memoryMb = memoryMb;
        this.storageMb = storageMb;
        this.cardMb = cardMb;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    /** The device of that serialized name, or null when none is. */
    @Nullable
    public static PortableModel byName(@Nullable final String name) {
        return NAMES.find(name);
    }

    /** Megabytes as a tooltip writes them: in megabytes below a gigabyte, in gigabytes from one up. */
    public static Text size(final int megabytes) {
        return megabytes >= MB_A_GB && megabytes % MB_A_GB == 0 ? PortableTexts.GIGABYTES.with(megabytes / MB_A_GB)
                : PortableTexts.MEGABYTES.with(megabytes);
    }

    public PortableKind kind() {
        return kind;
    }

    public Era era() {
        return era;
    }

    public MobileSystem system() {
        return system;
    }

    public int cores() {
        return cores;
    }

    public int clockMhz() {
        return clockMhz;
    }

    public int memoryMb() {
        return memoryMb;
    }

    /** Its own storage, in megabytes, without a memory card. */
    public int storageMb() {
        return storageMb;
    }

    /** The memory card it comes with, in megabytes; 0 for none. */
    public int cardMb() {
        return cardMb;
    }

    /** How wide its screen shows held up, in the GUI's pixels. */
    public int screenWidth() {
        return screenWidth;
    }

    /** How tall its screen shows held up, in the GUI's pixels. */
    public int screenHeight() {
        return screenHeight;
    }

    /** The Wi-Fi it speaks: its era's. */
    public WirelessStandard wifi() {
        return Objects.requireNonNull(WirelessStandard.of(era), "a portable is of an era with Wi-Fi");
    }

    /**
     * Its chip as its settings and its tooltip show it: the clock of its one core, or how many cores at what clock,
     * after the chip's own name on the qPad Pro, which has Quince's Q1.
     */
    public Text chip() {
        final Text clocks;
        if (cores == 1) {
            clocks = PortableTexts.CLOCK_MHZ.with(clockMhz);
        } else {
            final double ghz = clockMhz / MHZ_A_GHZ;
            clocks = PortableTexts.CLOCK_CORES.with(cores, ghz == Math.rint(ghz) ? Integer.toString((int) ghz)
                    : Double.toString(ghz));
        }
        return this == QPAD_PRO ? PortableTexts.CHIP_NAMED.with(PortableTexts.CHIP_Q1.text(), clocks) : clocks;
    }

    /** Its storage and its memory card together, in megabytes. */
    public int totalStorageMb() {
        return storageMb + cardMb;
    }

    /**
     * How wide the pictures its camera takes are, at its era's resolution: 640 in the Transition, 1,280 in the
     * Standard, 4,032 in the Advanced; 0 for a device with no camera.
     */
    public int cameraWidth() {
        if (!kind.hasCameraAndGps()) {
            return 0;
        }
        return era == HardwareEra.TRANSITION ? 640 : era == HardwareEra.STANDARD ? 1_280 : 4_032;
    }

    /** How tall: 480, 960 or 3,024, four to every three across; 0 for a device with no camera. */
    public int cameraHeight() {
        return cameraWidth() * 3 / 4;
    }

    @Override
    public String serializedName() {
        return serializedName;
    }
}
