/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import net.minecraft.world.item.ItemStack;

/**
 * Reads and writes the user-chosen label of a disk or media volume (the {@code VOLUME_LABEL} data
 * component). The label rides on the {@link ItemStack} so it travels with the disk/medium. When no
 * label is set, callers fall back to the volume's default name.
 */
public final class VolumeLabel {

    /** The longest label the user may set, to keep the address bar and drive tree readable. */
    public static final int MAX_LENGTH = 32;

    private VolumeLabel() {
    }

    /** The volume's label, or {@code fallback} when none is set or the stack is empty. */
    public static String of(final ItemStack stack, final String fallback) {
        if (stack.isEmpty()) {
            return fallback;
        }
        final String label = stack.get(ComputingComponents.VOLUME_LABEL.get());
        return label != null && !label.isBlank() ? label : fallback;
    }

    /** The volume's label, which is the player's own words, or {@code fallback} read in the player's language. */
    public static Text of(final ItemStack stack, final Text fallback) {
        final String label = stack.isEmpty() ? null : stack.get(ComputingComponents.VOLUME_LABEL.get());
        return label != null && !label.isBlank() ? Text.literal(label) : fallback;
    }

    /**
     * The volume's label as text: what the player called it, which is theirs, or else the medium's own name, which
     * each player reads in their language.
     */
    public static Text text(final ItemStack stack) {
        final String label = stack.isEmpty() ? null : stack.get(ComputingComponents.VOLUME_LABEL.get());
        return label != null && !label.isBlank() ? Text.literal(label) : GameText.of(stack.getHoverName());
    }

    /** Sets the volume's label, or clears it (restoring the default name) when {@code label} is blank. */
    public static void set(final ItemStack stack, final String label) {
        if (stack.isEmpty()) {
            return;
        }
        final String trimmed = label == null ? "" : label.trim();
        if (trimmed.isEmpty()) {
            stack.remove(ComputingComponents.VOLUME_LABEL.get());
        } else {
            stack.set(ComputingComponents.VOLUME_LABEL.get(),
                    trimmed.length() > MAX_LENGTH ? trimmed.substring(0, MAX_LENGTH) : trimmed);
        }
    }
}
