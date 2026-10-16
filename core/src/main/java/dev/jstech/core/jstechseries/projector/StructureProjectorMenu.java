/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.projector;

import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuOpening;
import dev.jstech.core.menu.MenuValidity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Structure Projector's menu: no slots, since a projector holds nothing; its screen sets what it shows through
 * {@link StructureProjector#SETTINGS}, and reads it from the projector, which is sent to the players who see it.
 */
public final class StructureProjectorMenu extends CoreMenu {

    private final StructureProjectorBlockEntity projector;

    public StructureProjectorMenu(final int containerId, final Inventory inventory,
                                  final StructureProjectorBlockEntity projector) {
        super(StructureProjector.MENU.get(), containerId, inventory, MenuValidity.blockEntity(projector));
        this.projector = projector;
    }

    /** The menu on the player's side, for the projector whose place the server wrote. */
    public StructureProjectorMenu(final int containerId, final Inventory inventory, final RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, MenuOpening.blockEntity(inventory, buf, StructureProjectorBlockEntity.class));
    }

    /** The projector. */
    public StructureProjectorBlockEntity projector() {
        return this.projector;
    }
}
