/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import dev.jstech.core.audio.media.MediaUploads;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * What the portables answer on the game's bus and in J's Core's store (computers/docs/design/wireless.md): the
 * server's tick, on which the network's watched items are looked at and the lost devices let go; a device lying in
 * the world that despawns, whose state goes with it; and the pictures the cameras bring.
 */
public final class PortableEvents {

    private PortableEvents() {
    }

    /** Says what the portables answer, once, from the mod's constructor. */
    public static void listen() {
        PortableAlerts.declare();
        MediaUploads.handle(PortableGallery.PURPOSE, new PortableGallery());
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> PortableAlerts.tick(event.getServer()));
        // Last of all, so a mod that gives the item longer to lie there is heard first.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, ItemExpireEvent.class, PortableEvents::expired);
    }

    /* A device that lay in the world until it despawned is lost, unless something gave it longer. */
    private static void expired(final ItemExpireEvent event) {
        if (event.getExtraLife() <= 0 && event.getEntity().getItem().getItem() instanceof PortableItem) {
            Portables.lost(event.getEntity());
        }
    }
}
