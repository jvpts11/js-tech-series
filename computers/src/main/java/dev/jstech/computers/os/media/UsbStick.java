/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;

/**
 * A model of USB stick, as its box describes it: the Vaultis Pocket 8G and 16G (USB 2.0) and 32G (USB 3.0) of the
 * Standard, and the 64G (USB-C) of the Advanced (computers/docs/design/storage-devices.md).
 *
 * @param capacityBytes what it holds, in the decimal gigabytes a stick's box counts
 * @param plug          its plug as the box prints it, a product's own name
 * @param era           the era it was sold in
 */
public record UsbStick(long capacityBytes, String plug, Era era) {

    public static final UsbStick POCKET_8G = new UsbStick(8_000_000_000L, "USB 2.0", HardwareEra.STANDARD);
    public static final UsbStick POCKET_16G = new UsbStick(16_000_000_000L, "USB 2.0", HardwareEra.STANDARD);
    public static final UsbStick POCKET_32G = new UsbStick(MediaFormat.USB.capacityBytes(), "USB 3.0",
            HardwareEra.STANDARD);
    public static final UsbStick POCKET_64G = new UsbStick(64_000_000_000L, "USB-C", HardwareEra.ADVANCED);
}
