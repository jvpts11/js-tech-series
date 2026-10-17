/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.atmosphere;

import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuOpening;
import dev.jstech.core.menu.MenuValidity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Atmosphere Detector's menu: no slots, since a detector holds nothing; its screen reads the detector, whose
 * readings are sent to the players who see it, and asks it to show its room through
 * {@link AtmosphereDetector#SHOW_ROOM}.
 */
public final class AtmosphereDetectorMenu extends CoreMenu {

    private final AtmosphereDetectorBlockEntity detector;

    public AtmosphereDetectorMenu(final int containerId, final Inventory inventory,
                                  final AtmosphereDetectorBlockEntity detector) {
        super(AtmosphereDetector.MENU.get(), containerId, inventory, MenuValidity.blockEntity(detector));
        this.detector = detector;
    }

    /** The menu on the player's side, for the detector whose place the server wrote. */
    public AtmosphereDetectorMenu(final int containerId, final Inventory inventory, final RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, MenuOpening.blockEntity(inventory, buf, AtmosphereDetectorBlockEntity.class));
    }

    /** The detector. */
    public AtmosphereDetectorBlockEntity detector() {
        return this.detector;
    }
}
