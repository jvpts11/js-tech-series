/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.tool;

import com.mojang.serialization.Codec;
import dev.jstech.core.machine.Tally;
import dev.jstech.core.material.MaterialThing;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.industrial.JsIndustrial;
import dev.jstech.industrial.material.IndustrialMaterials;
import dev.jstech.industrial.metallurgy.FormRoute;
import dev.jstech.industrial.metallurgy.FormRoutes;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The scraps drawing by hand leaves, as a machine's does: the player counts, a share of a scrap each wire pulled
 * through the Draw Plate ({@link FormRoutes#SCRAP_SHARE}), so one comes every so many, always, and never by chance.
 * What a player's count holds short of a scrap is kept with them, through death as well.
 */
public final class HandScrap {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, JsIndustrial.MODID);
    /** What each player's count of scraps holds short of a whole one, by scrap. */
    public static final Supplier<AttachmentType<Map<String, Double>>> COUNTED = ATTACHMENTS.register("hand_scrap",
            () -> AttachmentType.<Map<String, Double>>builder(() -> Map.of())
                    .serialize(Codec.unboundedMap(Codec.STRING, Codec.DOUBLE), map -> !map.isEmpty())
                    .copyOnDeath().build());

    private HandScrap() {
    }

    /** Registers the count and hands the game's bus what it answers, once, from the mod's constructor. */
    public static void register(final IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.ItemCraftedEvent.class, HandScrap::onCrafted);
    }

    /**
     * The scraps {@code player} gets for making {@code made} by hand with {@code tool}: a share of one counted, and
     * the whole ones given. Nothing for work that leaves no scrap.
     */
    public static int count(final ServerPlayer player, final HandTool tool, final ItemStack made) {
        final Optional<MaterialThing> thing = MaterialUses.thingOf(made.getItem());
        if (thing.isEmpty() || !thing.get().material().fits(IndustrialMaterials.SCRAP)) {
            return 0;
        }
        final Optional<FormRoute> route = FormRoutes.of(thing.get().form().id().getPath());
        if (route.isEmpty() || !route.get().leavesScrap() || route.get().hand() != tool.work()) {
            return 0;
        }
        final ItemStack scrap = new ItemStack(MaterialUses.item(thing.get().material(), IndustrialMaterials.SCRAP)
                .get());
        final Tally tally = new Tally();
        tally.restore(player.getData(COUNTED));
        final int due = tally.add(scrap.getItemHolder().getRegisteredName(), FormRoutes.SCRAP_SHARE);
        player.setData(COUNTED, tally.snapshot());
        if (due > 0) {
            final ItemStack given = scrap.copyWithCount(due);
            if (!player.getInventory().add(given)) {
                player.drop(given, false);
            }
        }
        return due;
    }

    private static void onCrafted(final PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        final Container grid = event.getInventory();
        for (int slot = 0; slot < grid.getContainerSize(); slot++) {
            if (grid.getItem(slot).getItem() instanceof HandTool tool) {
                count(player, tool, event.getCrafting());
                return;
            }
        }
    }
}
