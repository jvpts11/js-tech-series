/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

/**
 * A block entity whose block is a space of parts: the Core's cable block, where a cable's bundle runs through the
 * middle, and the space of parts alone. A microblock is placed into either the same way.
 */
public interface ISpaceHolder {

    /** The parts standing in its space. */
    SpaceParts parts();
}
