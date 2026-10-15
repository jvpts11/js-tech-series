/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.containment;

import dev.jstech.core.JsCore;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.energy.EnergyPorts;
import dev.jstech.core.jstechseries.JsTechTab;
import dev.jstech.core.jstechseries.SharedUses;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The containment of exotic matter, J's Core's shared blocks, so J's Industrial keeps in them what it makes and J's
 * Space what it gathers, with no other mod installed: the containment of exotic matter, for every exotic form but
 * antimatter, and the Antimatter Trap, a Penning trap, for antimatter alone. Each spends energy for as long as it holds
 * something; without energy it takes nothing more and lets nothing out. Each mod gives them its recipes.
 */
@TextHolder
public final class SharedContainment {

    public static final TextKey HOLDS = TextKey.of("jscore.containment.holds", "Holds %s mB of %s");
    public static final TextKey EMPTY = TextKey.of("jscore.containment.empty", "Empty");
    public static final TextKey UPKEEP_LINE = TextKey.of("jscore.containment.upkeep",
            "Spends %s while it holds something");
    public static final TextKey SEALED = TextKey.of("jscore.containment.sealed",
            "No energy: sealed, it takes nothing and lets nothing out");

    /** What each holds, in millibuckets. */
    public static final long VOLUME = 1_000L;
    /** What each spends a tick while it holds something, in joules: 1 kW, and 2 kW for the trap's colder vacuum. */
    public static final long UPKEEP = 50L;
    public static final long TRAP_UPKEEP = 100L;
    /** Their energy store, 100 kJ, and what it takes in a tick, at most what a basic cable carries. */
    public static final long STORE = 100_000L;
    public static final long TAKES = 500L;

    /** What a containment held, kept in the item it drops when broken. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MatterStack>> CONTAINED =
            CoreItems.CONTENT.component("contained_matter", MatterStack.CODEC,
                    ByteBufCodecs.fromCodecWithRegistries(MatterStack.CODEC));

    /** The containment of exotic matter. */
    public static final BlockEntry<ContainmentBlock> EXOTIC = CoreItems.CONTENT.block("exotic_matter_containment",
                    properties -> new ContainmentBlock(properties, false)).named("Exotic Matter Containment")
            .properties(properties -> properties.strength(5.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops())
            .look(IBlockLook.fixed(new IBlockModel.Column("exotic_matter_containment",
                    "block/containment/exotic_side", "block/containment/exotic_top")))
            .item((block, properties) -> new ContainmentItem(block, properties)).drops(Drops.SELF_WITH_CONTENTS)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(JsTechTab.BLOCKS_AND_TOOLS).register();
    /** The Antimatter Trap. */
    public static final BlockEntry<ContainmentBlock> ANTIMATTER_TRAP = CoreItems.CONTENT.block("antimatter_trap",
                    properties -> new ContainmentBlock(properties, true)).named("Antimatter Trap")
            .properties(properties -> properties.strength(5.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops().noOcclusion())
            .look(IBlockLook.fixed(new IBlockModel.Handmade("antimatter_trap")))
            .item((block, properties) -> new ContainmentItem(block, properties)).drops(Drops.SELF_WITH_CONTENTS)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(JsTechTab.BLOCKS_AND_TOOLS).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ContainmentBlockEntity>> CONTAINMENT_BE =
            CoreItems.CONTENT.blockEntity("exotic_matter_containment", ContainmentBlockEntity::new, EXOTIC,
                    ANTIMATTER_TRAP);

    static {
        // J's Industrial and J's Space declare their own use; the Core shows them too, so a world with the Core alone
        // can try them.
        SharedUses.declare(JsCore.MODID, EXOTIC::get);
        SharedUses.declare(JsCore.MODID, ANTIMATTER_TRAP::get);
    }

    private SharedContainment() {
    }

    /** Declares the content, from the Core's constructor, while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** Hands the mod's bus what this answers, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterCapabilitiesEvent.class, SharedContainment::onRegisterCapabilities);
    }

    public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(MatterCapabilities.EXOTIC, CONTAINMENT_BE.get(), (containment, side) ->
                containment.matter());
        event.registerBlockEntity(EnergyPorts.PORT, CONTAINMENT_BE.get(), (containment, side) -> containment.store());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CONTAINMENT_BE.get(),
                (containment, side) -> containment.fe());
    }

    /** The line saying what {@code held} is and how much of it there is. */
    public static Text holds(final MatterStack held) {
        return HOLDS.with(held.amount(), held.material().name());
    }
}
