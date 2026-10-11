/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.program.install.LiveInstallState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Whether a machine can still see the medium it was started from.
 *
 * <p>A machine running from a live medium is running out of that medium, so taking it out ends the session:
 * that is how a real one behaves and it is what makes the drive worth anything. What matters is the
 * difference between the medium being gone and the machine being unable to tell, and that difference is the
 * whole reason this is a question with three answers instead of two.
 *
 * <p>A drive in a chunk that is not loaded yet answers nothing. A world reloading brings the computer back
 * before the drive cabled to it, and for those few ticks a two-answer check reads the drive as empty. Acting
 * on that reading throws away an installation somebody may have spent an hour on, and throws it away
 * silently, which is worse: the terminal stays on the screen and simply stops answering. So a machine that
 * cannot tell is told to wait and ask again.
 */
public final class LiveMedium {

    /** What a machine can say about the medium it was started from. */
    public enum Answer {

        /** A drive it reaches holds that medium. */
        PRESENT,

        /** Every drive it reaches was looked at, and none of them holds it. */
        GONE,

        /** At least one drive could not be looked at, so nothing can be concluded yet. */
        UNKNOWN
    }

    private LiveMedium() {
    }

    /**
     * Whether any drive the machine reaches still holds the live medium of that distribution.
     *
     * @param level   the world the machine is in, or null before it has one
     * @param machine the machine
     * @param distro  the distribution whose medium the machine was started from
     */
    public static Answer holding(@Nullable final Level level, final IOsHost machine,
                                 final LiveInstallState.Distro distro) {
        if (level == null) {
            return Answer.UNKNOWN;
        }
        final MachineMedia media = MachineMedia.of(machine, level);
        for (final IMediaDrive drive : media.drives()) {
            if (holds(drive, distro)) {
                return Answer.PRESENT;
            }
        }
        /* Cabled to something nobody can see right now, which is not the same as cabled to nothing. */
        return media.complete() ? Answer.GONE : Answer.UNKNOWN;
    }

    /** The distribution a live medium is of, or nothing when it is not a live medium at all. */
    public static LiveInstallState.Distro distroOf(final ResourceLocation os) {
        if (os == null) {
            return null;
        }
        for (final LiveInstallState.Distro distro : LiveInstallState.Distro.values()) {
            if (mediumName(distro).equals(os.getPath())) {
                return distro;
            }
        }
        return null;
    }

    /** Whether that drive holds the live medium of that distribution. */
    private static boolean holds(final IMediaDrive drive, final LiveInstallState.Distro distro) {
        return drive.installs(MediaKind.OS_INSTALL) && mediumName(distro).equals(drive.payload().getPath());
    }

    /** What that distribution's medium is called, which is the distribution's own name. */
    private static String mediumName(final LiveInstallState.Distro distro) {
        return distro.serializedName();
    }
}
