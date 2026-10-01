/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import dev.jstech.core.JsCore;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.registry.CoreItems;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * The Core's cable block and the registry of the cables laid in it. Every mod's cables share the one block, so a run of
 * one mod's cable and a run of another's lie side by side in it without either mod knowing the other. A mod declares
 * its cables with its content:
 *
 * <pre>{@code
 * CableEntry ETHERNET = CONTENT.cable("ethernet_cable", CableType.builder(ACCESS)
 *         .lane(Lane.TOP_LEFT).carries(500, 64)
 *         .jacket(texture("block/cable/ethernet")).plug(model("block/cable/plug/rj45")))
 *         .named("Ethernet Cable").tab(NETWORK).register();
 * }</pre>
 *
 * <p>The registry is synced, so a player's game knows the same cables as the server and reads a block's wires by the
 * cables' ids.
 */
@EventBusSubscriber(modid = JsCore.MODID)
public final class CoreCables {

    /** The registry's key. */
    public static final ResourceKey<Registry<CableType>> KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "cable_type"));
    /** Every cable. */
    public static final Registry<CableType> REGISTRY = new RegistryBuilder<>(KEY).sync(true).create();

    /** The block every cable is laid in. */
    public static final BlockEntry<CableBlock> BLOCK = CoreItems.CONTENT.block("cable", CableBlock::new)
            .properties(properties -> properties.mapColor(MapColor.COLOR_GRAY).strength(0.3F)
                    .sound(SoundType.WOOL).noOcclusion().dynamicShape())
            .named("Cable")
            .look(IBlockLook.fixed(new IBlockModel.ParticleOnly("cable", "block/cable/housing")))
            .drops(Drops.NONE)
            .register();
    /** The block entity of the cable block, which keeps its wires and its parts. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CableBlockEntity>> BLOCK_ENTITY =
            CoreItems.CONTENT.blockEntity("cable", CableBlockEntity::new, BLOCK);

    /* The line each lane was found to belong to, checked as cables are first looked at. */
    private static final Map<Lane, ResourceLocation> LANE_LINES = new HashMap<>();
    private static volatile boolean checked;

    private CoreCables() {
    }

    /** Declares the cable block, before the Core's content is registered. */
    public static void declare() {
        // Loading the class declares the block and its entity.
    }

    /**
     * Makes sure no two lines were given one lane: a lane belongs to a line in every block, so two lines in one lane
     * would take each other's place. Run once, the first time a cable is laid or read.
     *
     * @throws IllegalStateException when two lines share a lane
     */
    public static void checkLanes() {
        if (checked) {
            return;
        }
        synchronized (LANE_LINES) {
            if (checked) {
                return;
            }
            for (final CableType type : REGISTRY) {
                type.lane().ifPresent(lane -> {
                    final ResourceLocation line = type.line().line();
                    final ResourceLocation before = LANE_LINES.putIfAbsent(lane, line);
                    if (before != null && !before.equals(line)) {
                        throw new IllegalStateException("the lines " + before + " and " + line
                                + " were both given the lane " + lane);
                    }
                });
            }
            checked = true;
        }
    }

    @SubscribeEvent
    public static void onNewRegistry(final NewRegistryEvent event) {
        event.register(REGISTRY);
    }
}
