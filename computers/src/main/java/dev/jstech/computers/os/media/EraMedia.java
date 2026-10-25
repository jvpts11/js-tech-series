/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import java.util.Objects;
import org.jetbrains.annotations.ApiStatus;

/**
 * What the software of an era ships on: its operating systems, the programs a player opens, and the services that run
 * in the background, which from the Standard on go on the bootable stick the systems do.
 *
 * @param systems  the medium an operating system of the era ships on
 * @param programs the medium a program a player opens ships on
 * @param services the medium a program that runs in the background ships on
 */
@ApiStatus.Experimental
public record EraMedia(MediaFormat systems, MediaFormat programs, MediaFormat services) {

    public EraMedia {
        Objects.requireNonNull(systems, "systems");
        Objects.requireNonNull(programs, "programs");
        Objects.requireNonNull(services, "services");
    }

    /** Everything of the era on one medium, as before the stick could boot a system. */
    public static EraMedia all(final MediaFormat format) {
        return new EraMedia(format, format, format);
    }
}
