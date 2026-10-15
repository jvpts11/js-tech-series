/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.data;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/** A screen built from a file: built again when the file is read again, and found by its view in a test. */
public interface IDataScreen {

    /** The file it is built from. */
    ResourceLocation file();

    /** What it built from the file, laid out and drawn. */
    DataScreenView view();

    /** Builds it again from what its file says now. */
    void rebuild();

    /** The component of that id in what it built, or none. */
    default Optional<BuiltNode> find(final String id) {
        return view().find(id);
    }
}
