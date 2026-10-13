/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.api.client;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.ApiStatus;

/**
 * One opening of a machine's operating space, as the screen that draws it is handed it: which machine and monitor it
 * belongs to, which space it is, the era of the machine's hardware, and the menu that is already wired to the machine.
 *
 * <p>The menu holds the player's inventory as real slots, which a space builds its container screen on; everything a
 * player sees and types is the space's. What the network holds and does, the space asks the machine for through its
 * own payloads, as any screen of its own mod does.
 */
@ApiStatus.Experimental
public interface IOperatingSpace {

    /** The machine's menu, holding the player's inventory as real slots, for the screen to be built on. */
    AbstractContainerMenu menu();

    /** The machine this opening shows. */
    BlockPos hostPos();

    /** The monitor it is shown on. */
    BlockPos monitorPos();

    /** Which space the machine runs. */
    ResourceLocation spaceId();

    /** The era of the machine's hardware by its name, {@code vintage}, {@code legacy} and so on, for the look. */
    String era();
}
