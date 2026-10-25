/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.tools;

import dev.jstech.core.JsCore;
import dev.jstech.core.cable.FlowSight;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.jstechseries.JsTechTab;
import dev.jstech.core.jstechseries.SharedUses;
import dev.jstech.core.machine.MachineSetup;
import dev.jstech.core.multipart.CoreSpaces;
import dev.jstech.core.multipart.MicroShape;
import dev.jstech.core.multipart.Microblock;
import dev.jstech.core.registry.CoreItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The tools the series' mods share, so a mod used alone has them: the Basic Wrench, which sets the ends of a line and
 * the sides of a machine and takes a machine away whole, in the common wrench tag every mod's machines know; the
 * Configuration Card, which carries a machine's setup to others; and the saw, which cuts microblocks.
 */
public final class SharedTools {

    /** The common tag of wrenches, by which other mods' blocks know one. */
    public static final TagKey<Item> WRENCHES = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));

    /** How hard a blow of the Basic Wrench harms, and how many it gives a second: a bar of iron, swung slowly. */
    public static final double WRENCH_DAMAGE = 5;
    public static final double WRENCH_SPEED = 0.8;
    /** The Basic Wrench. */
    public static final ItemEntry<BasicWrenchItem> WRENCH = CoreItems.CONTENT.item("basic_wrench",
                    properties -> new BasicWrenchItem(properties.stacksTo(1))).named("Basic Wrench")
            .look(IItemLook.HELD).strikes(WRENCH_DAMAGE, WRENCH_SPEED).tag(WRENCHES).tag(FlowSight.SEES_FLOW)
            .tab(JsTechTab.BLOCKS_AND_TOOLS).register();
    /** The setup a Configuration Card holds, copied from a machine. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MachineSetup.Copied>> SETUP =
            CoreItems.CONTENT.component("machine_setup", MachineSetup.CODEC, MachineSetup.STREAM_CODEC);
    /** The first corner of a box a Configuration Card pastes over. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> CORNER =
            CoreItems.CONTENT.component("card_corner", BlockPos.CODEC, BlockPos.STREAM_CODEC);
    /** The Configuration Card. */
    public static final ItemEntry<ConfigurationCardItem> CARD = CoreItems.CONTENT.item("configuration_card",
                    properties -> new ConfigurationCardItem(properties.stacksTo(1))).named("Configuration Card")
            .tab(JsTechTab.BLOCKS_AND_TOOLS).register();
    /** How many cuts a saw makes before it wears out. */
    public static final int SAW_CUTS = 512;
    /** How hard a blow of the saw harms, and how many it gives a second: a light blade, swung quickly. */
    public static final double SAW_DAMAGE = 4;
    public static final double SAW_SPEED = 1.2;
    /** The saw, which cuts microblocks in the crafting grid and is worn by each cut. */
    public static final ItemEntry<Item> SAW = CoreItems.CONTENT.item("saw",
                    properties -> new Item(properties.durability(SAW_CUTS))).named("Saw")
            .look(IItemLook.HELD).strikes(SAW_DAMAGE, SAW_SPEED).tag(CoreSpaces.SAWS)
            .tab(JsTechTab.BLOCKS_AND_TOOLS).register();

    static {
        // Every mod of the series sets its lines' ends with it and its machines up with the card, so the Core itself
        // uses them; and the saw and the microblocks it cuts.
        SharedUses.declare(JsCore.MODID, WRENCH::get);
        SharedUses.declare(JsCore.MODID, CARD::get);
        SharedUses.declare(JsCore.MODID, SAW::get);
        SharedUses.declare(JsCore.MODID, CoreSpaces.MICROBLOCK::get);
        JsTechTab.BLOCKS_AND_TOOLS.with(CoreSpaces.MICROBLOCK::get).alsoShowing(output -> {
            for (final MicroShape shape : MicroShape.values()) {
                output.accept(CoreSpaces.stack(new Microblock.MicroblockPiece(shape, shape == MicroShape.FACADE
                        ? MicroShape.COVER : MicroShape.PANEL, Blocks.OAK_PLANKS)));
            }
        });
    }

    private SharedTools() {
    }

    /** Declares the tools, from the Core's constructor, while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares them.
    }
}
