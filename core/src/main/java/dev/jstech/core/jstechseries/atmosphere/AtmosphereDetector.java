/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.atmosphere;

import dev.jstech.core.JsCore;
import dev.jstech.core.call.MenuCall;
import dev.jstech.core.call.MenuCalls;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.jstechseries.JsTechTab;
import dev.jstech.core.jstechseries.SharedUses;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.Materials;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.room.RoomViews;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The Atmosphere Detector, a block of J's Core's shared content: it reads the air round it, a sealed room's or the
 * place's, and shows each gas and its share, the pressure and how fast it changes, and the room it stands in, its
 * leaks marked; a key shows the room in the world.
 */
public final class AtmosphereDetector {

    /* The elements whose gas is of pairs of atoms. */
    private static final Set<String> PAIRED = Set.of("H", "N", "O", "F", "Cl");

    /** The detector. */
    public static final BlockEntry<DeviceBlock> BLOCK = CoreItems.CONTENT.block("atmosphere_detector",
                    Device.of(() -> AtmosphereDetector.ENTITY.get()).ticks(AtmosphereDetectorBlockEntity::serverTick)
                            .opensMenu(AtmosphereDetectorMenu::new)::block).named("Atmosphere Detector")
            .properties(properties -> properties.strength(2.0F, 6.0F).sound(SoundType.METAL))
            .look(IBlockLook.facing(IBlockModel.orientable("atmosphere_detector", "block/atmosphere_detector/side",
                    "block/atmosphere_detector/front", "block/atmosphere_detector/top"))).item()
            .tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(JsTechTab.BLOCKS_AND_TOOLS).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AtmosphereDetectorBlockEntity>> ENTITY =
            CoreItems.CONTENT.blockEntity("atmosphere_detector", AtmosphereDetectorBlockEntity::new, BLOCK);
    /** Its screen's menu. */
    public static final DeferredHolder<MenuType<?>, MenuType<AtmosphereDetectorMenu>> MENU =
            CoreItems.CONTENT.menu("atmosphere_detector", AtmosphereDetectorMenu::new);
    /** Its key that shows its room in the world to the player who presses it, or stops showing it. */
    public static final MenuCall<AtmosphereDetectorMenu, Boolean> SHOW_ROOM = MenuCalls.declare(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "atmosphere_show"), AtmosphereDetectorMenu.class,
            ByteBufCodecs.BOOL, (menu, player, on) -> {
                if (player instanceof ServerPlayer server) {
                    RoomViews.show(server, menu.detector().getBlockPos(), on);
                }
            });

    static {
        // Any mod with rooms of air reads them with it, so the Core itself uses it.
        SharedUses.declare(JsCore.MODID, BLOCK::get);
    }

    private AtmosphereDetector() {
    }

    /** Declares the content, from the Core's constructor, while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares it.
    }

    /**
     * How a gas is written on the display: its formula (CO2), or its element's symbol, doubled for an element whose
     * gas is of pairs (N2, O2), or its id's name.
     */
    public static String gasLabel(final ResourceLocation gas) {
        final Optional<Material> material = Materials.byId(gas);
        if (material.isEmpty()) {
            return gas.getPath().toUpperCase(Locale.ROOT);
        }
        final Optional<String> formula = material.get().value(MaterialProperties.FORMULA);
        if (formula.isPresent()) {
            return formula.get();
        }
        return material.get().value(MaterialProperties.SYMBOL).map(symbol -> PAIRED.contains(symbol) ? symbol + "2"
                : symbol).orElse(gas.getPath().toUpperCase(Locale.ROOT));
    }
}
