/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableNames;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

/**
 * The kinds of media-reader peripheral block. A drive has no tier of its own; its capability is
 * defined by which {@link MediaFormat media formats} it can read. Each drive is a distinct block
 * (its own texture) attached to a computer via the COMPUTING peripheral cable.
 *
 * <p>Pure enum (no Minecraft imports) so the compatibility rules are unit-testable.
 */
@TextHolder
public enum MediaDriveType implements IStableName {

    /** Reads 3.5" floppy disks. The earliest drive. */
    FLOPPY_DRIVE("floppy_drive", EnumSet.of(MediaFormat.FLOPPY)),

    /** Reads CDs (CD-ROM and CD-RW). */
    CD_DRIVE("cd_drive", EnumSet.of(MediaFormat.CD)),

    /** Reads DVDs and, for backward compatibility, CDs. */
    DVD_DRIVE("dvd_drive", EnumSet.of(MediaFormat.DVD, MediaFormat.CD)),

    /** Reads USB flash drives; also used for disk diagnostics and recovery. */
    DOCK_STATION("dock_station", EnumSet.of(MediaFormat.USB)),

    /** Reads Blu-ray discs and, as the DVD drive reads CDs, the older optical discs: DVDs and CDs. */
    BLU_RAY_DRIVE("blu_ray_drive", EnumSet.of(MediaFormat.BLU_RAY, MediaFormat.DVD, MediaFormat.CD));

    private static final StableNames<MediaDriveType> NAMES = StableNames.of(MediaDriveType.class);

    private static final TextKey FLOPPY_NAME = TextKey.of("jsc.media.media_drive_type.floppy_drive", "Floppy drive");
    private static final TextKey CD_NAME = TextKey.of("jsc.media.media_drive_type.cd_drive", "CD drive");
    private static final TextKey DVD_NAME = TextKey.of("jsc.media.media_drive_type.dvd_drive", "DVD drive");
    private static final TextKey DOCK_NAME = TextKey.of("jsc.media.media_drive_type.dock_station", "Dock Station");
    private static final TextKey BLU_RAY_NAME = TextKey.of("jsc.media.media_drive_type.blu_ray_drive",
            "Blu-ray drive");

    /** The same drives named for the middle of a sentence, so they read in lower case where a name would not. */
    private static final TextKey FLOPPY_IN_SENTENCE = TextKey.of("jsc.media.media_reader_block.floppy_drive",
            "floppy drive");
    private static final TextKey CD_IN_SENTENCE = TextKey.of("jsc.media.media_reader_block.cd_drive", "CD drive");
    private static final TextKey DVD_IN_SENTENCE = TextKey.of("jsc.media.media_reader_block.dvd_drive", "DVD drive");
    private static final TextKey BLU_RAY_IN_SENTENCE = TextKey.of("jsc.media.media_reader_block.blu_ray_drive",
            "Blu-ray drive");
    private static final TextKey DOCK_IN_SENTENCE = TextKey.of("jsc.media.media_reader_block.dock_station",
            "dock station");

    private static final EjectButton CD_BUTTON = new EjectButton(44, 20, 52, 23, 0);
    private static final EjectButton DVD_BUTTON = new EjectButton(53, 19, 59, 22, 0);
    private static final EjectButton BLU_RAY_BUTTON = new EjectButton(54, 20, 60, 23, 4);

    private final String serializedName;
    private final Set<MediaFormat> accepted;

    MediaDriveType(final String serializedName, final Set<MediaFormat> accepted) {
        this.serializedName = serializedName;
        this.accepted = accepted;
    }

    @Override
    public String serializedName() {
        return serializedName;
    }

    /**
     * What a firmware calls this drive when it lists it beside the disks.
     *
     * <p>A self-test names the hardware it found the way the hardware was sold, so these are written out
     * rather than taken from the constant's own name, which reads as a machine's spelling and not a drive's.
     */
    public Text driveName() {
        return switch (this) {
            case FLOPPY_DRIVE -> FLOPPY_NAME.text();
            case CD_DRIVE -> CD_NAME.text();
            case DVD_DRIVE -> DVD_NAME.text();
            case DOCK_STATION -> DOCK_NAME.text();
            case BLU_RAY_DRIVE -> BLU_RAY_NAME.text();
        };
    }

    /** What a message calls this drive in the middle of a sentence, such as "This floppy drive cannot read that". */
    public Text inSentence() {
        return switch (this) {
            case FLOPPY_DRIVE -> FLOPPY_IN_SENTENCE.text();
            case CD_DRIVE -> CD_IN_SENTENCE.text();
            case DVD_DRIVE -> DVD_IN_SENTENCE.text();
            case DOCK_STATION -> DOCK_IN_SENTENCE.text();
            case BLU_RAY_DRIVE -> BLU_RAY_IN_SENTENCE.text();
        };
    }

    /**
     * The era the drive is of, which decides the port on its back (save the Dock's, see {@link #portEra()}) and so the
     * peripheral cables it takes: the floppy
     * drive the Vintage's parallel port, the CD drive the Legacy's USB, the DVD drive and the Dock the Standard's
     * USB 3, the Blu-ray drive the Advanced's USB-C.
     */
    public HardwareEra era() {
        return switch (this) {
            case FLOPPY_DRIVE -> HardwareEra.VINTAGE;
            case CD_DRIVE -> HardwareEra.LEGACY;
            case DVD_DRIVE, DOCK_STATION -> HardwareEra.STANDARD;
            case BLU_RAY_DRIVE -> HardwareEra.ADVANCED;
        };
    }

    /**
     * The newest era of peripheral cable the drive's port takes, a port taking its own era's cable and every earlier
     * one. The Dock serves the disks of every era, so it takes every era's cable: an Advanced computer reaches it with
     * its own cable, not only with an older one.
     */
    public HardwareEra portEra() {
        return this == DOCK_STATION ? HardwareEra.ADVANCED : era();
    }

    /** Returns whether this drive can read media of the given format. */
    public boolean accepts(final MediaFormat format) {
        return accepted.contains(format);
    }

    /**
     * The button that opens and closes the drive's disc tray, where its model draws it beside the tray; null for the
     * floppy drive and the Dock, which have none. The Blu-ray drive's front is set a pixel back in its case.
     */
    @Nullable
    public EjectButton ejectButton() {
        return switch (this) {
            case CD_DRIVE -> CD_BUTTON;
            case DVD_DRIVE -> DVD_BUTTON;
            case BLU_RAY_DRIVE -> BLU_RAY_BUTTON;
            case FLOPPY_DRIVE, DOCK_STATION -> null;
        };
    }

    /** The drive a name stands for, or null for a name no drive declares. */
    @Nullable
    public static MediaDriveType find(@Nullable final String name) {
        return NAMES.find(name);
    }
}
