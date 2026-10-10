/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import org.jetbrains.annotations.Nullable;

/**
 * A device block that may have a disc tray, opened and closed by the eject button on its front: an optical drive, or a
 * Pattern Encoder that writes discs.
 */
public interface ITrayBlock {

    /** The eject button on its front, or null when it has no tray. */
    @Nullable
    EjectButton ejectButton();
}
