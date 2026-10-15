/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.machine;

import java.util.List;
import net.minecraft.network.chat.Component;

/**
 * What one tab of the machine screen draws on the steel face, and how it answers the mouse. The Main tab's is the
 * machine's own: a mod gives a machine of its own a face of its own ({@link MachineFaces#register}), drawn with
 * {@link MachineArt} so it matches every other, its slots where its machine's {@code mainSlots} puts them; every other
 * machine shows the {@link StandardFace}.
 *
 * <p>A point it is given is on the face, from the face's top left, as {@link MachineView#x} and {@link MachineView#y}
 * draw them.
 */
public interface IMachineFace {

    /** Draws it, under the items in the slots. */
    void draw(MachineView view);

    /** Draws what lies over the items in the slots, such as the thing a slot is locked to; nothing unless it says. */
    default void drawOver(final MachineView view) {
    }

    /** Answers a click of mouse {@code button} at ({@code x}, {@code y}); whether it took it. */
    default boolean clicked(final MachineView view, final double x, final double y, final int button) {
        return false;
    }

    /** Answers the wheel turned by {@code amount} at ({@code x}, {@code y}); whether it took it. */
    default boolean scrolled(final MachineView view, final double x, final double y, final double amount) {
        return false;
    }

    /** What the mouse at ({@code x}, {@code y}) is told; nothing unless it says. */
    default List<Component> tooltip(final MachineView view, final double x, final double y) {
        return List.of();
    }
}
