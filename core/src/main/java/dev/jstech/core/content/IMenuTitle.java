/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import net.minecraft.network.chat.Component;

/**
 * A device's block entity whose menu opens under a title other than its block's name: the terminal of a multiblock,
 * under the name of the machine it stands for, as a Computational Research Centre's terminal opens the Centre.
 */
public interface IMenuTitle {

    /** The title its menu opens under. */
    Component menuTitle();
}
