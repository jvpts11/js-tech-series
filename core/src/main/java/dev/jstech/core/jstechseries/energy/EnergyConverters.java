/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

import dev.jstech.core.JsCore;
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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The FE converters, the Core's own blocks, so any mod alone can bridge the game's FE and J's Energy: one of each class,
 * from 100 kW at low voltage to 12 GW at ultra-high, and the Configurable Energy Converter, whose class the player sets.
 * Every one works both ways and takes a limit under its class's ceiling.
 */
@TextHolder
public final class EnergyConverters {

    public static final TextKey RUN = TextKey.of("jscore.converter.run", "RUN");
    public static final TextKey LIMIT = TextKey.of("jscore.converter.limit", "LIMIT");
    public static final TextKey FAULT = TextKey.of("jscore.converter.fault", "FAULT");
    public static final TextKey ON = TextKey.of("jscore.converter.on", "ON");
    public static final TextKey OFF = TextKey.of("jscore.converter.off", "OFF");
    public static final TextKey CLASS = TextKey.of("jscore.converter.class", "CLASS");
    public static final TextKey CEILING = TextKey.of("jscore.converter.ceiling", "CEILING");
    public static final TextKey AT_LIMIT = TextKey.of("jscore.converter.at_limit", "AT LIMIT");
    public static final TextKey FE = TextKey.of("jscore.converter.fe", "FE");
    public static final TextKey JE = TextKey.of("jscore.converter.je", "JE");
    public static final TextKey FE_PER_TICK = TextKey.of("jscore.converter.fe_per_tick", "%sFE/t");
    public static final TextKey CLASS_PLATE = TextKey.of("jscore.converter.class_plate", "%s %s");
    public static final TextKey FIXED_PLATE = TextKey.of("jscore.converter.fixed_plate", "FE CONVERTER %s");
    public static final TextKey CONFIGURABLE_PLATE = TextKey.of("jscore.converter.configurable_plate",
            "ENERGY CONVERTER");
    public static final TextKey LOW_KEY = TextKey.of("jscore.converter.key.low", "LV");
    public static final TextKey MEDIUM_KEY = TextKey.of("jscore.converter.key.medium", "MV");
    public static final TextKey HIGH_KEY = TextKey.of("jscore.converter.key.high", "HV");
    public static final TextKey EXTRA_HIGH_KEY = TextKey.of("jscore.converter.key.extra_high", "EHV");
    public static final TextKey ULTRA_HIGH_KEY = TextKey.of("jscore.converter.key.ultra_high", "UHV");
    public static final TextKey LOW_WORD = TextKey.of("jscore.converter.word.low", "LOW");
    public static final TextKey MEDIUM_WORD = TextKey.of("jscore.converter.word.medium", "MEDIUM");
    public static final TextKey HIGH_WORD = TextKey.of("jscore.converter.word.high", "HIGH");
    public static final TextKey EXTRA_HIGH_WORD = TextKey.of("jscore.converter.word.extra_high", "EXTRA-HIGH");
    public static final TextKey ULTRA_HIGH_WORD = TextKey.of("jscore.converter.word.ultra_high", "ULTRA-HIGH");
    public static final TextKey TO_JE_LINE = TextKey.of("jscore.converter.way.to_je", "Works from FE to J's Energy");
    public static final TextKey TO_FE_LINE = TextKey.of("jscore.converter.way.to_fe", "Works from J's Energy to FE");
    public static final TextKey LIMIT_LINE = TextKey.of("jscore.converter.limit_line", "Limit: %s");
    public static final TextKey MOVED_LINE = TextKey.of("jscore.converter.moved_line", "Moving: %s");
    public static final TextKey SWITCHED_OFF = TextKey.of("jscore.converter.switched_off", "Switched off");

    /** The fixed converters, from the low class up. */
    public static final BlockEntry<EnergyConverterBlock> LOW = fixed(VoltageClass.LOW, "FE Converter LV");
    public static final BlockEntry<EnergyConverterBlock> MEDIUM = fixed(VoltageClass.MEDIUM, "FE Converter MV");
    public static final BlockEntry<EnergyConverterBlock> HIGH = fixed(VoltageClass.HIGH, "FE Converter HV");
    public static final BlockEntry<EnergyConverterBlock> EXTRA_HIGH = fixed(VoltageClass.EXTRA_HIGH,
            "FE Converter EHV");
    public static final BlockEntry<EnergyConverterBlock> ULTRA_HIGH = fixed(VoltageClass.ULTRA_HIGH,
            "FE Converter UHV");
    /** The converter whose class the player sets. */
    public static final BlockEntry<EnergyConverterBlock> CONFIGURABLE = CoreItems.CONTENT.block(
                    "configurable_energy_converter", properties -> new EnergyConverterBlock(properties, null))
            .named("Configurable Energy Converter").properties(EnergyConverters::cabinet)
            .look(IBlockLook.facingByState(state -> {
                final VoltageClass voltage = VoltageClass.ofRank(state.getValue(EnergyConverterBlock.VOLTAGE))
                        .orElse(VoltageClass.LOW);
                return faces("configurable_energy_converter_" + voltage.rank(), "front_configurable", voltage);
            })).item().itemLook(IItemLook.parent("block/configurable_energy_converter_1"))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(JsTechTab.BLOCKS_AND_TOOLS).register();

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyConverterBlockEntity>> CONVERTER_BE =
            CoreItems.CONTENT.blockEntity("energy_converter", EnergyConverterBlockEntity::new, LOW, MEDIUM, HIGH,
                    EXTRA_HIGH, ULTRA_HIGH, CONFIGURABLE);
    public static final DeferredHolder<MenuType<?>, MenuType<EnergyConverterMenu>> CONVERTER_MENU =
            CoreItems.CONTENT.menu("energy_converter", EnergyConverterMenu::new);

    static {
        // Any mod alone bridges FE and J's Energy with these, so the Core itself uses them.
        for (final DeferredBlock<EnergyConverterBlock> each : all()) {
            SharedUses.declare(JsCore.MODID, each::get);
        }
    }

    private EnergyConverters() {
    }

    /** Declares the converters, from the Core's constructor, while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** Hands the mod's bus what this answers, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterCapabilitiesEvent.class, EnergyConverters::onRegisterCapabilities);
    }

    public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(EnergyPorts.PORT, CONVERTER_BE.get(), EnergyConverterBlockEntity::port);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CONVERTER_BE.get(), EnergyConverterBlockEntity::fe);
    }

    /** Every converter block, the fixed ones from the low class up, then the configurable one. */
    public static List<DeferredBlock<EnergyConverterBlock>> all() {
        final List<DeferredBlock<EnergyConverterBlock>> all = new ArrayList<>(List.of(LOW, MEDIUM, HIGH, EXTRA_HIGH,
                ULTRA_HIGH));
        all.add(CONFIGURABLE);
        return all;
    }

    /** The fixed converter of {@code voltage}. */
    public static BlockEntry<EnergyConverterBlock> fixedOf(final VoltageClass voltage) {
        return switch (voltage) {
            case LOW -> LOW;
            case MEDIUM -> MEDIUM;
            case HIGH -> HIGH;
            case EXTRA_HIGH -> EXTRA_HIGH;
            case ULTRA_HIGH -> ULTRA_HIGH;
        };
    }

    /** The legend on {@code voltage}'s class key: LV, MV, HV, EHV or UHV. */
    public static TextKey key(final VoltageClass voltage) {
        return switch (voltage) {
            case LOW -> LOW_KEY;
            case MEDIUM -> MEDIUM_KEY;
            case HIGH -> HIGH_KEY;
            case EXTRA_HIGH -> EXTRA_HIGH_KEY;
            case ULTRA_HIGH -> ULTRA_HIGH_KEY;
        };
    }

    /** The word on {@code voltage}'s class plate, before its rating: LOW, MEDIUM, HIGH, EXTRA-HIGH or ULTRA-HIGH. */
    public static TextKey word(final VoltageClass voltage) {
        return switch (voltage) {
            case LOW -> LOW_WORD;
            case MEDIUM -> MEDIUM_WORD;
            case HIGH -> HIGH_WORD;
            case EXTRA_HIGH -> EXTRA_HIGH_WORD;
            case ULTRA_HIGH -> ULTRA_HIGH_WORD;
        };
    }

    /** The line saying which way a converter works. */
    public static TextKey way(final boolean toJe) {
        return toJe ? TO_JE_LINE : TO_FE_LINE;
    }

    private static BlockEntry<EnergyConverterBlock> fixed(final VoltageClass voltage, final String english) {
        final String id = "fe_converter_" + shortName(voltage);
        return CoreItems.CONTENT.block(id, properties -> new EnergyConverterBlock(properties, voltage)).named(english)
                .properties(EnergyConverters::cabinet)
                .look(IBlockLook.facing(faces(id, "front_" + shortName(voltage), voltage)))
                .item((block, properties) -> new EnergyBlockItem(block, properties, JsEnergy.ENERGY,
                        JsEnergy.classOf(voltage)))
                .tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(JsTechTab.BLOCKS_AND_TOOLS).register();
    }

    /* The cabinet's faces: its front toward whoever placed it, FE on their left, J's Energy of its class on the right. */
    private static IBlockModel faces(final String name, final String front, final VoltageClass voltage) {
        return new IBlockModel.SixFaces(name, "block/converter/bottom", "block/converter/top", "block/converter/"
                + front, "block/converter/back", "block/converter/fe", "block/converter/je_" + shortName(voltage),
                "block/converter/back");
    }

    private static BlockBehaviour.Properties cabinet(final BlockBehaviour.Properties properties) {
        return properties.strength(3.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    /* The class's short name in ids and texture names. */
    private static String shortName(final VoltageClass voltage) {
        return switch (voltage) {
            case LOW -> "lv";
            case MEDIUM -> "mv";
            case HIGH -> "hv";
            case EXTRA_HIGH -> "ehv";
            case ULTRA_HIGH -> "uhv";
        };
    }
}
