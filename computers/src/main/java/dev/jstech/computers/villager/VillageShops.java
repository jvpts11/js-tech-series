/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.villager;

import com.mojang.datafixers.util.Pair;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.abandoned.AbandonedBuilding;
import dev.jstech.computers.abandoned.AbandonedMachines;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.worldgen.modify.WorldgenModifiers;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/**
 * The villages' computer shop (computers/docs/design/survival.md, "Villagers"): a house of each kind of village, in
 * its own materials, with both job sites and a Vintage demo computer running MC-DOS, so the two professions appear
 * without the player placing anything. It joins the houses its village may build as the server starts, as often as a
 * library does there, while the server's settings let it.
 */
public final class VillageShops {

    /** The kinds of village, each with its own shop. */
    public static final List<String> KINDS = List.of("plains", "desert", "savanna", "snowy", "taiga");
    /* What a library's houses are named by, among a village's houses: the shop turns up as often. */
    private static final String LIBRARY = "library";
    private static final String SHOP = "computer_shop";

    private VillageShops() {
    }

    /** Adds the shop to the villages' houses as every server starts, after the world's settings are read. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ServerAboutToStartEvent.class,
                event -> addTo(event.getServer().registryAccess()));
    }

    /** The template of the shop of the village of {@code kind}. */
    public static ResourceLocation template(final String kind) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "village/" + kind + "/" + SHOP);
    }

    /** The pool of houses of the village of {@code kind}, which the shop joins. */
    public static ResourceKey<StructureTemplatePool> houses(final String kind) {
        return ResourceKey.create(Registries.TEMPLATE_POOL, ResourceLocation.withDefaultNamespace(
                "village/" + kind + "/houses"));
    }

    /** Puts the shop among the houses of every kind of village of {@code registries}, unless the server says not. */
    public static void addTo(final RegistryAccess registries) {
        if (!VillagerSettings.computerShop()) {
            return;
        }
        final Registry<StructureTemplatePool> pools = registries.registryOrThrow(Registries.TEMPLATE_POOL);
        for (final String kind : KINDS) {
            pools.getHolder(houses(kind)).ifPresent(holder -> {
                if (!holder.value().getShuffledTemplates(RandomSource.create(0)).stream()
                        .anyMatch(element -> element.toString().contains(template(kind).toString()))) {
                    WorldgenModifiers.rebind(holder, withShop(holder.value(), kind));
                }
            });
        }
    }

    /*
     * The pool with the shop among its houses: each of its own houses as often as before, and the shop as often as
     * its libraries together (a house's element names its template).
     */
    private static StructureTemplatePool withShop(final StructureTemplatePool pool, final String kind) {
        final Map<StructurePoolElement, Integer> weights = new LinkedHashMap<>();
        for (final StructurePoolElement element : pool.getShuffledTemplates(RandomSource.create(0))) {
            weights.merge(element, 1, Integer::sum);
        }
        int library = 0;
        final List<Pair<StructurePoolElement, Integer>> houses = new ArrayList<>();
        for (final Map.Entry<StructurePoolElement, Integer> house : weights.entrySet()) {
            houses.add(Pair.of(house.getKey(), house.getValue()));
            if (house.getKey().toString().contains(LIBRARY)) {
                library += house.getValue();
            }
        }
        houses.add(Pair.of(shop(kind), Math.max(1, library)));
        return new StructureTemplatePool(pool.getFallback(), houses);
    }

    /* The shop's piece: its template, its demo computer and its frames filled as an abandoned shop's are. */
    private static StructurePoolElement shop(final String kind) {
        return StructurePoolElement.single(template(kind).toString(), Holder.direct(new StructureProcessorList(
                List.of(new AbandonedMachines(AbandonedBuilding.COMPUTER_SHOP, HardwareEra.VINTAGE, true)))))
                .apply(StructureTemplatePool.Projection.RIGID);
    }
}
