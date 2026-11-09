/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import dev.jstech.computers.hardware.EraStorage;
import dev.jstech.computers.os.fs.FsPaths;

/**
 * How much a portable's own storage holds (computers/docs/design/wireless.md): its storage, the Hightide Pocket Pro's
 * card with it, over the size an item takes on a disk of its era, less its system: 4 items on the Hightide Pocket, 8
 * on the Pocket Pro, 112 on the qPhone 5, 480 on the Nebula S10, 1,000 on the qPad Pro. Its room is counted in
 * thousandths of an item, as a disk counts it, so the pictures its camera takes fill it by their bytes as a computer's
 * files fill a disk.
 *
 * <p>Pure: no part of the game.
 */
public final class PortableStorage {

    /** What one item weighs in a device's room: a thousand thousandths, as on every disk. */
    public static final long ITEM_WEIGHT = 1_000L;

    private PortableStorage() {
    }

    /** How many items a device of {@code model} holds with nothing else on it. */
    public static long items(final PortableModel model) {
        final long all = model.totalStorageMb() / EraStorage.mbPerItem(model.era());
        return Math.max(0L, all - EraStorage.itemsFor(model.era(), model.system().footprintMb()));
    }

    /** Its whole room, in thousandths of an item. */
    public static long capacity(final PortableModel model) {
        return items(model) * ITEM_WEIGHT;
    }

    /** What a picture of {@code bytes} weighs in its room, rounded up as a file on a disk of its era is. */
    public static long pictureWeight(final PortableModel model, final long bytes) {
        return FsPaths.sizeMbEq(bytes, model.era());
    }
}
