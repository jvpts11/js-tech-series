/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.launch;

import dev.jstech.core.call.MenuCall;
import dev.jstech.core.call.MenuCalls;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.space.JsSpace;
import dev.jstech.space.SpaceContent;
import dev.jstech.space.pad.PadBlocks;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The Launch Panel of J's Space: the LCC-1 console by the pad, where a launch is counted down and given its go. Used,
 * it opens its console, the approved one, its switches, key and buttons pressed with the mouse.
 */
public final class LaunchPanel {

    public static final BlockEntry<DeviceBlock> BLOCK = SpaceContent.CONTENT.block("launch_panel", properties ->
                    new DeviceBlock(properties, Device.of(() -> LaunchPanel.ENTITY.get())
                            .ticks((level, pos, state, panel) -> panel.tick()).opensMenu(LaunchPanelMenu::new)))
            .properties(properties -> properties.mapColor(MapColor.METAL).strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL))
            .look(IBlockLook.facing(new IBlockModel.Orientable("launch_panel", "block/launch/launch_panel_side",
                    "block/launch/launch_panel_front", "block/launch/launch_panel_top")))
            .item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(PadBlocks.PAD).named("Launch Panel")
            .register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LaunchPanelBlockEntity>> ENTITY =
            SpaceContent.CONTENT.blockEntity("launch_panel", LaunchPanelBlockEntity::new, BLOCK);
    /** Its console's menu. */
    public static final DeferredHolder<MenuType<?>, MenuType<LaunchPanelMenu>> MENU =
            SpaceContent.CONTENT.menu("launch_panel", LaunchPanelMenu::new);
    /** A control of its console pressed, by its number ({@link LaunchPanelBlockEntity#press}). */
    public static final MenuCall<LaunchPanelMenu, Integer> PRESS = MenuCalls.declare(id("launch_panel_press"),
            LaunchPanelMenu.class, ByteBufCodecs.VAR_INT, (menu, player, control) -> menu.panel().press(control,
                    player.getUUID()));

    private LaunchPanel() {
    }

    /** Declares its block, its block entity, its menu and its console's call, once, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, path);
    }
}
