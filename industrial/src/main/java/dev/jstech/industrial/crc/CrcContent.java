/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.crc;

import dev.jstech.core.content.BlockBuilder;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.DeclaredModel;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemBuilder;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.grid.LineGroups;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.multiblock.MultiblockPorts;
import dev.jstech.industrial.IndustrialModule;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The Computational Research Centre's content: its blocks, its terminal's and its entries' block entities, the parts
 * that go in its places, and its formed model. Every one is shown on J's Industrial's research shelf.
 */
public final class CrcContent {

    /* Every block of it: steel cabinets, painted. */
    private static final float STRENGTH = 3.5F;
    private static final String CASING_TEXTURE = "block/crc/crc_casing";
    /* The formed model's register lamps, which blink while it works. */
    private static final int LAMPS = 12;

    public static final BlockEntry<Block> CASING = structural("crc_casing").named("CRC Casing").register();
    public static final BlockEntry<Block> GATE = structural("crc_gate").named("CRC Gate").register();
    public static final BlockEntry<Block> BUS = structural("crc_bus").named("CRC Bus").register();
    public static final BlockEntry<CrcBlock> TERMINAL = facing("crc_terminal",
            Device.of(() -> CrcContent.TERMINAL_BE.get()).ticks(MachineBlockEntity::serverTick)
                    .opensMenu(MachineMenu::new)).named("CRC Terminal").register();
    public static final BlockEntry<CrcBlock> POWER_ENTRY = entryBlock("crc_power_entry").named("CRC Power Entry")
            .register();
    public static final BlockEntry<CrcBlock> LINK_SOCKET = entryBlock("crc_link_socket").named("CRC Link Socket")
            .register();
    public static final BlockEntry<CrcBlock> COOLANT_INLET = entryBlock("crc_coolant_inlet")
            .named("CRC Coolant Inlet").register();
    public static final BlockEntry<CrcBlock> COOLANT_OUTLET = entryBlock("crc_coolant_outlet")
            .named("CRC Coolant Outlet").register();
    public static final BlockEntry<CrcBlock> HEAT_PORT = entryBlock("crc_heat_port").named("CRC Heat Port")
            .register();
    /** The items of the parts that go in its places. */
    public static final ItemEntry<CrcPartItem> RESEARCH_PROCESSOR = part(CrcPart.RESEARCH_PROCESSOR)
            .named("Research Processor").register();
    public static final ItemEntry<CrcPartItem> INTEGRATED_RESEARCH_PROCESSOR = part(
            CrcPart.INTEGRATED_RESEARCH_PROCESSOR).named("Integrated Research Processor").register();
    public static final ItemEntry<CrcPartItem> NANOSCALE_RESEARCH_PROCESSOR = part(
            CrcPart.NANOSCALE_RESEARCH_PROCESSOR).named("Nanoscale Research Processor").register();
    public static final ItemEntry<CrcPartItem> HEAT_SINK = part(CrcPart.HEAT_SINK).named("Heat Sink").register();
    public static final ItemEntry<CrcPartItem> LIQUID_COOLER = part(CrcPart.LIQUID_COOLER).named("Liquid Cooler")
            .register();
    public static final ItemEntry<CrcPartItem> HEAT_PIPE_COOLER = part(CrcPart.HEAT_PIPE_COOLER)
            .named("Heat Pipe Cooler").register();

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrcBlockEntity>> TERMINAL_BE =
            IndustrialModule.CONTENT.blockEntity("crc_terminal", CrcBlockEntity::new, TERMINAL);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrcEntryBlockEntity>> ENTRY_BE =
            IndustrialModule.CONTENT.blockEntity("crc_entry", CrcEntryBlockEntity::new, POWER_ENTRY, LINK_SOCKET,
                    COOLANT_INLET, COOLANT_OUTLET, HEAT_PORT);

    /** The item of each part, by the part. */
    public static final Map<CrcPart, ItemEntry<CrcPartItem>> PARTS = Collections.unmodifiableMap(new EnumMap<>(Map.of(
            CrcPart.RESEARCH_PROCESSOR, RESEARCH_PROCESSOR,
            CrcPart.INTEGRATED_RESEARCH_PROCESSOR, INTEGRATED_RESEARCH_PROCESSOR,
            CrcPart.NANOSCALE_RESEARCH_PROCESSOR, NANOSCALE_RESEARCH_PROCESSOR,
            CrcPart.HEAT_SINK, HEAT_SINK,
            CrcPart.LIQUID_COOLER, LIQUID_COOLER,
            CrcPart.HEAT_PIPE_COOLER, HEAT_PIPE_COOLER)));

    /** The formed model, one cabinet of three frames drawn by the terminal over the whole structure. */
    public static final DeclaredModel MODEL = IndustrialModule.CONTENT.model("computational_research_centre")
            .file("jsmodels/computational_research_centre.geo.json")
            .texture("textures/model/computational_research_centre.png")
            .glow(IntStream.range(0, LAMPS).mapToObj(k -> "lamp_" + k).toArray(String[]::new))
            .register();

    private CrcContent() {
    }

    /** Declares the content, and hands the mod's bus what reaches the terminal through the entries. */
    public static void register(final IEventBus modEventBus) {
        modEventBus.addListener(RegisterCapabilitiesEvent.class, CrcContent::onRegisterCapabilities);
    }

    /** The blocks of the structure, in the order the shelf shows them. */
    public static List<BlockEntry<?>> blocks() {
        return List.of(TERMINAL, CASING, GATE, BUS, POWER_ENTRY, LINK_SOCKET, COOLANT_INLET, COOLANT_OUTLET,
                HEAT_PORT);
    }

    private static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        MultiblockPorts.register(event, ENTRY_BE.get());
        event.registerBlockEntity(LineGroups.MEMBER, ENTRY_BE.get(), CrcEntryBlockEntity::member);
    }

    /* A block of the structure with nothing of its own, which merges into the formed model. */
    private static BlockBuilder<Block> structural(final String path) {
        return IndustrialModule.CONTENT.block(path, Block::new)
                .properties(properties -> properties.mapColor(MapColor.SAND).strength(STRENGTH).sound(SoundType.METAL)
                        .requiresCorrectToolForDrops())
                .look(IBlockLook.fixed(new IBlockModel.CubeAll(path, "block/crc/" + path)))
                .item().tab(IndustrialModule.RESEARCH).tag(BlockTags.MINEABLE_WITH_PICKAXE);
    }

    /*
     * A block of the structure that faces whoever placed it, its face on the front and the casing round it, and draws
     * nothing of its own while the structure stands formed.
     */
    private static BlockBuilder<CrcBlock> facing(final String path, final Device<?> device) {
        return IndustrialModule.CONTENT.block(path, properties -> new CrcBlock(properties, device))
                .properties(properties -> properties.mapColor(MapColor.SAND).strength(STRENGTH).sound(SoundType.METAL)
                        .requiresCorrectToolForDrops())
                .look(IBlockLook.facing(new IBlockModel.Orientable(path, CASING_TEXTURE, "block/crc/" + path,
                        CASING_TEXTURE)).whileOn(CrcBlock.FORMED, new IBlockModel.ParticleOnly(path + "_formed",
                        CASING_TEXTURE)))
                .item().tab(IndustrialModule.RESEARCH).tag(BlockTags.MINEABLE_WITH_PICKAXE);
    }

    /* An entry of its rear: a block of the structure through which something comes in or goes out. */
    private static BlockBuilder<CrcBlock> entryBlock(final String path) {
        return facing(path, Device.of(() -> CrcContent.ENTRY_BE.get()));
    }

    /* The item of a part that goes in its places. */
    private static ItemBuilder<CrcPartItem> part(final CrcPart part) {
        return IndustrialModule.CONTENT.item(part.path(), properties -> new CrcPartItem(properties, part))
                .look(IItemLook.FLAT).tab(IndustrialModule.RESEARCH);
    }
}
