/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import dev.jstech.computers.block.DeviceFront;
import dev.jstech.computers.hardware.FrontArea;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

/**
 * Where a device's disc tray stands once it is open, as its model draws it: the part of the front it slides out of,
 * in sixty-fourths from the front's top left as it is seen, with the disc lying on it taken in, and how many
 * sixty-fourths it stands out.
 *
 * @param area the part of the front it slides out of
 * @param out  how far out of the front it stands
 */
public record StandingTray(FrontArea area, int out) {

    /** The open tray of a device facing {@code front}, as a box in its block reaching out of its face. */
    public AABB box(final Direction front) {
        return DeviceFront.box(area, -out, 0, front);
    }
}
