/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.fire;

import dev.jstech.core.JsCore;
import dev.jstech.core.audio.SoundCue;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.fire.ExtinguishingAgent;
import dev.jstech.core.jstechseries.JsTechTab;
import dev.jstech.core.jstechseries.SharedUses;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.signal.Signals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The series' fire safety things: an extinguisher of each agent, its band in the colour of the European code (water
 * all red, foam cream, carbon dioxide black, dry powder blue); the sprinkler head fed by a water pipe; and the smoke
 * and heat detectors, sensors that speak redstone.
 */
public final class FireSafety {

    public static final ItemEntry<ExtinguisherItem> WATER_EXTINGUISHER = CoreItems.CONTENT.item(
                    "water_extinguisher", properties -> new ExtinguisherItem(ExtinguishingAgent.WATER,
                            properties.durability(ExtinguisherItem.holds(ExtinguishingAgent.WATER))))
            .named("Water Extinguisher").look(IItemLook.HELD).tab(JsTechTab.BLOCKS_AND_TOOLS).register();
    public static final ItemEntry<ExtinguisherItem> FOAM_EXTINGUISHER = CoreItems.CONTENT.item("foam_extinguisher",
                    properties -> new ExtinguisherItem(ExtinguishingAgent.FOAM,
                            properties.durability(ExtinguisherItem.holds(ExtinguishingAgent.FOAM))))
            .named("Foam Extinguisher").look(IItemLook.HELD).tab(JsTechTab.BLOCKS_AND_TOOLS).register();
    public static final ItemEntry<ExtinguisherItem> CARBON_DIOXIDE_EXTINGUISHER = CoreItems.CONTENT.item(
                    "carbon_dioxide_extinguisher", properties -> new ExtinguisherItem(ExtinguishingAgent.CARBON_DIOXIDE,
                            properties.durability(ExtinguisherItem.holds(ExtinguishingAgent.CARBON_DIOXIDE))))
            .named("Carbon Dioxide Extinguisher").look(IItemLook.HELD).tab(JsTechTab.BLOCKS_AND_TOOLS).register();
    public static final ItemEntry<ExtinguisherItem> DRY_POWDER_EXTINGUISHER = CoreItems.CONTENT.item(
                    "dry_powder_extinguisher", properties -> new ExtinguisherItem(ExtinguishingAgent.DRY_POWDER,
                            properties.durability(ExtinguisherItem.holds(ExtinguishingAgent.DRY_POWDER))))
            .named("Dry Powder Extinguisher").look(IItemLook.HELD).tab(JsTechTab.BLOCKS_AND_TOOLS).register();
    public static final BlockEntry<SprinklerBlock> SPRINKLER = CoreItems.CONTENT.block("sprinkler",
                    SprinklerBlock::new)
            .properties(properties -> properties.mapColor(MapColor.COLOR_RED).strength(1.5F)
                    .sound(SoundType.METAL).noOcclusion())
            .named("Sprinkler")
            .look(IBlockLook.byState(state -> new IBlockModel.Handmade(state.getValue(SprinklerBlock.OPEN)
                    ? "sprinkler_open" : "sprinkler")))
            .item()
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .tab(JsTechTab.BLOCKS_AND_TOOLS)
            .register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SprinklerBlockEntity>> SPRINKLER_ENTITY =
            CoreItems.CONTENT.blockEntity("sprinkler", SprinklerBlockEntity::new, SPRINKLER);
    public static final BlockEntry<FireDetectorBlock> SMOKE_DETECTOR = CoreItems.CONTENT.block("smoke_detector",
                    properties -> new FireDetectorBlock(FireDetectorBlock.Sense.SMOKE, properties))
            .properties(properties -> properties.mapColor(MapColor.SNOW).strength(0.5F).sound(SoundType.STONE)
                    .noOcclusion())
            .named("Smoke Detector")
            .look(IBlockLook.byState(state -> new IBlockModel.Handmade(state.getValue(FireDetectorBlock.ALARM)
                    ? "smoke_detector_alarm" : "smoke_detector")))
            .item()
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .tab(JsTechTab.BLOCKS_AND_TOOLS)
            .register();
    public static final BlockEntry<FireDetectorBlock> HEAT_DETECTOR = CoreItems.CONTENT.block("heat_detector",
                    properties -> new FireDetectorBlock(FireDetectorBlock.Sense.HEAT, properties))
            .properties(properties -> properties.mapColor(MapColor.SNOW).strength(0.5F).sound(SoundType.STONE)
                    .noOcclusion())
            .named("Heat Detector")
            .look(IBlockLook.byState(state -> new IBlockModel.Handmade(state.getValue(FireDetectorBlock.ALARM)
                    ? "heat_detector_alarm" : "heat_detector")))
            .item()
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .tab(JsTechTab.BLOCKS_AND_TOOLS)
            .register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FireDetectorBlockEntity>> DETECTOR =
            CoreItems.CONTENT.blockEntity("fire_detector", FireDetectorBlockEntity::new, SMOKE_DETECTOR,
                    HEAT_DETECTOR);
    /** The shower of a sprinkler. */
    public static final ResourceLocation SHOWER = id("sprinkler_shower");
    /** An extinguisher spraying, a sprinkler showering and a detector calling; silent until a pack sounds them. */
    public static final SoundCue SPRAYING = CoreItems.CONTENT.cue("fire/extinguisher").world().range(24).register();
    public static final SoundCue SHOWERING = CoreItems.CONTENT.cue("fire/sprinkler").world().range(24).register();
    public static final SoundCue ALARM_CALLS = CoreItems.CONTENT.cue("fire/alarm").world().range(48).register();

    static {
        // The Core's fire is put out with them and watched by them, so the Core itself uses them.
        SharedUses.declare(JsCore.MODID, WATER_EXTINGUISHER::get);
        SharedUses.declare(JsCore.MODID, FOAM_EXTINGUISHER::get);
        SharedUses.declare(JsCore.MODID, CARBON_DIOXIDE_EXTINGUISHER::get);
        SharedUses.declare(JsCore.MODID, DRY_POWDER_EXTINGUISHER::get);
        SharedUses.declare(JsCore.MODID, SPRINKLER::asItem);
        SharedUses.declare(JsCore.MODID, SMOKE_DETECTOR::asItem);
        SharedUses.declare(JsCore.MODID, HEAT_DETECTOR::asItem);
    }

    private FireSafety() {
    }

    /** Declares the things, from the Core's constructor, while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** The spray of an extinguisher of {@code agent}, in the agent's own colours. */
    public static ResourceLocation spray(final ExtinguishingAgent agent) {
        return id("extinguisher_spray_" + agent.getSerializedName());
    }

    /** Hands the mod's bus what the detectors offer, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterCapabilitiesEvent.class, event -> Signals.offer(event, DETECTOR.get()));
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }
}
