/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

import dev.jstech.core.JsCore;
import dev.jstech.core.cable.CableEntry;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.Lane;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.energy.EnergyBlockItem;
import dev.jstech.core.energy.EnergyPorts;
import dev.jstech.core.jstechseries.JsTechTab;
import dev.jstech.core.jstechseries.SharedUses;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The energy content the series' mods share, so a mod used alone can carry and keep energy: the basic cable, the
 * Insulated Copper Cable, a low-voltage line of J's Energy; and the basic battery, the Lead-Acid Battery Bank. With J's
 * Industrial installed they are its own first cable and first energy bank, which it gives its industrial recipes.
 */
@TextHolder
public final class SharedEnergy {

    public static final TextKey CABLE_JOB = TextKey.of("jscore.cable.insulated_copper.job",
            "Low voltage line of J's Energy, the basic cable");

    /** The cable's ceiling, 10 kW, in joules a tick, and what each block of it loses: a tenth over a hundred blocks. */
    public static final long CABLE_CEILING = 500L;
    public static final int CABLE_LOSS = 1_000;
    /** The battery's store, 50 MJ, and what it takes and gives in a tick, 10 kW. */
    public static final long BATTERY_CAPACITY = 50_000_000L;
    public static final long BATTERY_RATE = 500L;

    /** The low-voltage line the basic cable carries. */
    public static final Connection LOW_VOLTAGE_LINE = Connection.of(rl("low_voltage"));

    /** The basic cable. */
    public static final CableEntry INSULATED_COPPER_CABLE = CoreItems.CONTENT.cable("insulated_copper_cable",
                    CableType.builder(LOW_VOLTAGE_LINE).describe(CABLE_JOB, null)
                            .grid(JsEnergy.ENERGY.grid(JsEnergy.LOW)).lane(Lane.BOTTOM).thickness(4)
                            .carries(CABLE_CEILING, 0).loses(CABLE_LOSS)
                            .jacket(rl("block/cable/insulated_copper"))
                            .plug(rl("block/cable/plug/insulated_copper")))
            .named("Insulated Copper Cable").tab(JsTechTab.BLOCKS_AND_TOOLS).register();

    /** The basic battery. */
    public static final BlockEntry<BatteryBankBlock> BATTERY = CoreItems.CONTENT.block("lead_acid_battery_bank",
                    BatteryBankBlock::new).named("Lead-Acid Battery Bank")
            .properties(properties -> properties.strength(2.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .look(IBlockLook.facingByState(state -> new IBlockModel.Column("lead_acid_battery_bank_"
                    + state.getValue(BatteryBankBlock.CHARGE), "block/battery/lead_acid_side_"
                    + state.getValue(BatteryBankBlock.CHARGE), "block/battery/lead_acid_top")))
            .item((block, properties) -> new EnergyBlockItem(block, properties, JsEnergy.ENERGY, JsEnergy.LOW))
            .itemLook(IItemLook.parent("block/lead_acid_battery_bank_0")).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(JsTechTab.BLOCKS_AND_TOOLS).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryBankBlockEntity>> BATTERY_BE =
            CoreItems.CONTENT.blockEntity("lead_acid_battery_bank", BatteryBankBlockEntity::new, BATTERY);

    static {
        // A mod used alone carries and keeps energy with these, so the Core itself uses them.
        SharedUses.declare(JsCore.MODID, () -> INSULATED_COPPER_CABLE);
        SharedUses.declare(JsCore.MODID, BATTERY::get);
    }

    private SharedEnergy() {
    }

    /** Declares the content, from the Core's constructor, while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** Hands the mod's bus what this answers, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterCapabilitiesEvent.class, SharedEnergy::onRegisterCapabilities);
    }

    public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(EnergyPorts.PORT, BATTERY_BE.get(), (battery, side) -> battery.store());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BATTERY_BE.get(), (battery, side) -> battery.fe());
    }

    private static ResourceLocation rl(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }
}
