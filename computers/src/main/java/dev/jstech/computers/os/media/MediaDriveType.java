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
    DOCK_STATION("dock_station", EnumSet.of(MediaFormat.USB));

    private static final StableNames<MediaDriveType> NAMES = StableNames.of(MediaDriveType.class);

    private static final TextKey FLOPPY_NAME = TextKey.of("jsc.media.media_drive_type.floppy_drive", "Floppy drive");
    private static final TextKey CD_NAME = TextKey.of("jsc.media.media_drive_type.cd_drive", "CD drive");
    private static final TextKey DVD_NAME = TextKey.of("jsc.media.media_drive_type.dvd_drive", "DVD drive");
    private static final TextKey DOCK_NAME = TextKey.of("jsc.media.media_drive_type.dock_station", "Dock Station");

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
        };
    }

    /** Returns whether this drive can read media of the given format. */
    public boolean accepts(final MediaFormat format) {
        return accepted.contains(format);
    }

    /** Returns an unmodifiable view of the formats this drive accepts. */
    public Set<MediaFormat> acceptedFormats() {
        return EnumSet.copyOf(accepted);
    }

    /** The drive a name stands for, or null for a name no drive declares. */
    @Nullable
    public static MediaDriveType find(@Nullable final String name) {
        return NAMES.find(name);
    }
}
