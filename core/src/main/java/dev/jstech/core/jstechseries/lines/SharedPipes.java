/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.lines;

import dev.jstech.core.JsCore;
import dev.jstech.core.cable.BreachRules;
import dev.jstech.core.cable.CableEntry;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.CableBuilder;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.jstechseries.JsTechTab;
import dev.jstech.core.jstechseries.SharedUses;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The basic pipes the series' mods share, so a mod used alone can carry liquids and gases: the liquid pipe, the gas
 * pipe and the cryogenic pipe, each holding its block alone, a sight glass in each block showing what flows at its
 * level. With J's Industrial installed they are its Copper Pipe, Iron Gas Pipe and Vacuum-Jacketed Pipe, which it gives
 * its industrial recipes. A liquid too hot or too cold for a pipe bursts or cracks it after a while, and spills. At a
 * gas line's end, the Gas Vent lets out into the air a gas nobody wants.
 */
@TextHolder
public final class SharedPipes {

    public static final TextKey COPPER_JOB = TextKey.of("jscore.cable.copper_pipe.job",
            "Liquid pipe, up to 200 °C, the basic pipe");
    public static final TextKey IRON_GAS_JOB = TextKey.of("jscore.cable.iron_gas_pipe.job",
            "Gas pipe at low pressure, the basic gas pipe");
    public static final TextKey VACUUM_JOB = TextKey.of("jscore.cable.vacuum_jacketed_pipe.job",
            "Liquid pipe for cryogenic liquids, double-walled with a vacuum between");

    /** How much each carries a tick, in millibuckets. */
    public static final long COPPER_FLOW = 100L;
    public static final long IRON_GAS_FLOW = 100L;
    public static final long VACUUM_FLOW = 400L;
    /** The temperatures each stands, in kelvin: 200 °C at most; and the cold a plain pipe stands, before it cracks. */
    public static final int HOTTEST = 473;
    public static final int PLAIN_COLDEST = 200;
    /** How long a pipe stands what it is not made for before it bursts, in ticks. */
    public static final int BURSTS_AFTER = 100;
    private static final int THICKNESS = 10;

    /** The liquid pipe: the Copper Pipe. */
    public static final CableEntry COPPER_PIPE = pipe("copper_pipe", GridKind.FLUID, COPPER_FLOW, PLAIN_COLDEST,
            COPPER_JOB).named("Copper Pipe").tab(JsTechTab.BLOCKS_AND_TOOLS).register();
    /** The gas pipe: the Iron Gas Pipe. */
    public static final CableEntry IRON_GAS_PIPE = pipe("iron_gas_pipe", GridKind.GAS, IRON_GAS_FLOW, PLAIN_COLDEST,
            IRON_GAS_JOB).named("Iron Gas Pipe").tab(JsTechTab.BLOCKS_AND_TOOLS).register();
    /** The cryogenic pipe: the Vacuum-Jacketed Pipe. */
    public static final CableEntry VACUUM_JACKETED_PIPE = pipe("vacuum_jacketed_pipe", GridKind.FLUID, VACUUM_FLOW,
            0, VACUUM_JOB).named("Vacuum-Jacketed Pipe").tab(JsTechTab.BLOCKS_AND_TOOLS).register();

    /** The end of a gas line that lets out into the air the gas it is given: the Gas Vent. */
    public static final BlockEntry<DeviceBlock> GAS_VENT = CoreItems.CONTENT.block("gas_vent",
                    Device.of(() -> SharedPipes.GAS_VENT_BE.get()).ticks((level, pos, state, vent) -> vent.tick())
                            ::block).named("Gas Vent")
            .properties(properties -> properties.strength(2.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .noOcclusion())
            .look(IBlockLook.facing(new IBlockModel.Handmade("gas_vent"))).item()
            .tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(JsTechTab.BLOCKS_AND_TOOLS).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GasVentBlockEntity>> GAS_VENT_BE =
            CoreItems.CONTENT.blockEntity("gas_vent", GasVentBlockEntity::new, GAS_VENT);

    static {
        // A mod used alone carries its liquids and gases in these, so the Core itself uses them.
        SharedUses.declare(JsCore.MODID, () -> COPPER_PIPE);
        SharedUses.declare(JsCore.MODID, () -> IRON_GAS_PIPE);
        SharedUses.declare(JsCore.MODID, () -> VACUUM_JACKETED_PIPE);
        SharedUses.declare(JsCore.MODID, GAS_VENT::get);
    }

    private SharedPipes() {
    }

    /** Declares the pipes, from the Core's constructor, while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** Hands the mod's bus what this answers, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterCapabilitiesEvent.class, SharedPipes::onRegisterCapabilities);
    }

    public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(MatterCapabilities.GAS, GAS_VENT_BE.get(), GasVentBlockEntity::inlet);
    }

    private static CableBuilder pipe(final String id, final GridKind grid, final long flow, final int coldest,
                                     final TextKey job) {
        return CoreItems.CONTENT.cable(id, CableType.builder(Connection.of(rl(id))).describe(job, null)
                .grid(grid).alone().thickness(THICKNESS).carries(flow, 0).withstands(coldest, HOTTEST)
                .sounds(SoundType.METAL)
                .beyond(BreachRules.breaksAfter(BURSTS_AFTER,
                        (level, breach) -> BreachRules.spill(level, breach.at(), breach.what())))
                .jacket(rl("block/cable/" + id)).windows(rl("block/cable/" + id + "_glass"))
                .fitting(rl("block/cable/" + id + "_fitting")).plug(rl("block/cable/plug/" + id)));
    }

    private static ResourceLocation rl(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }
}
