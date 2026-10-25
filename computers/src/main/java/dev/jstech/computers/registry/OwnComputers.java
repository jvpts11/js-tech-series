/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.registry;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.api.ComputersRegisterEvent;
import dev.jstech.computers.hardware.ComputerDef;
import dev.jstech.computers.rack.RackChassis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * This mod's own computers and servers, kept as each is declared onto the mod's content, and declared again through
 * the register event the way an add-on declares its own: a block made here is a computer only because the event says
 * so.
 */
public final class OwnComputers {

    private static final List<Kept<ComputerDef>> COMPUTERS = new ArrayList<>();
    private static final List<Kept<RackChassis>> SERVERS = new ArrayList<>();

    private OwnComputers() {
    }

    /** Keeps the computer this mod declares as the block {@code id}, to be declared through the event. */
    public static synchronized void computer(final String id, final ComputerDef computer) {
        COMPUTERS.add(new Kept<>(own(id), computer));
    }

    /** Keeps the server this mod declares as the item {@code id}, built in {@code chassis}. */
    public static synchronized void server(final String id, final RackChassis chassis) {
        SERVERS.add(new Kept<>(own(id), chassis));
    }

    /** Declares every computer and server kept, through {@code event}; the mod's listener on the event. */
    public static synchronized void declare(final ComputersRegisterEvent event) {
        for (final Kept<ComputerDef> kept : COMPUTERS) {
            final Block block = BuiltInRegistries.BLOCK.get(kept.id());
            if (block == Blocks.AIR) {
                throw new IllegalStateException("the computer " + kept.id() + " was kept but never registered");
            }
            event.computer(block, kept.entry());
        }
        for (final Kept<RackChassis> kept : SERVERS) {
            final Item item = BuiltInRegistries.ITEM.get(kept.id());
            if (item == Items.AIR) {
                throw new IllegalStateException("the server " + kept.id() + " was kept but never registered");
            }
            event.server(item, kept.entry());
        }
    }

    private static ResourceLocation own(final String id) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, id);
    }

    private record Kept<T>(ResourceLocation id, T entry) {
    }
}
