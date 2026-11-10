/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import static dev.jstech.computers.registry.ComputingContent.CONTENT;
import static dev.jstech.computers.registry.ComputingContent.shelf;

import dev.jstech.computers.registry.ComputingContent.Shelf;
import dev.jstech.core.content.BlockBuilder;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.multiblock.MultiblockPorts;
import dev.jstech.core.multiblock.PortKind;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The blocks the chain's machines are built of before they form (computers/docs/design/survival.md): the five
 * casings by the work they stand (mechanical, heat, corrosion, vacuum and gases, the clean room), the Machine
 * Terminal and the entries every machine shares, and each machine's working blocks, which make it what it is. Each is
 * on the chain's shelf of the era it first appears in. The casings and the working blocks merge into a formed
 * machine's model; the terminal and the entries keep their own block, drawing nothing while their machine stands.
 */
public final class ChainMachineBlocks {

    private static final Map<String, BlockEntry<? extends Block>> BY_ID = new LinkedHashMap<>();
    private static final String TEXTURES = "block/chain/machine/";

    public static final BlockEntry<Block> MACHINE_CASING = put(plain("machine_casing", HardwareEra.VINTAGE)
            .named("Machine Casing").register());
    public static final BlockEntry<Block> REFRACTORY_CASING = put(plain("refractory_casing", HardwareEra.VINTAGE)
            .named("Refractory Casing").register());
    public static final BlockEntry<Block> LINED_CASING = put(plain("lined_casing", HardwareEra.VINTAGE)
            .named("Lined Casing").register());
    public static final BlockEntry<Block> VACUUM_CASING = put(plain("vacuum_casing", HardwareEra.VINTAGE)
            .named("Vacuum Casing").register());
    public static final BlockEntry<Block> CLEAN_ROOM_CASING = put(plain("clean_room_casing", HardwareEra.VINTAGE)
            .named("Clean Room Casing").register());
    public static final BlockEntry<ChainTerminalBlock> TERMINAL = put(CONTENT.block("machine_terminal",
                    ChainTerminalBlock::new)
            .named("Machine Terminal")
            .properties(properties -> properties.mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL))
            .look(IBlockLook.facing(new IBlockModel.Orientable("machine_terminal", TEXTURES + "machine_casing",
                    TEXTURES + "terminal", TEXTURES + "machine_casing")))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE).item().tab(shelf(Shelf.CHAIN, HardwareEra.VINTAGE)).register());
    public static final BlockEntry<ChainFormedBlock> ITEM_INLET = put(entry("item_inlet").named("Item Inlet")
            .register());
    public static final BlockEntry<ChainFormedBlock> ITEM_OUTLET = put(entry("item_outlet").named("Item Outlet")
            .register());
    public static final BlockEntry<ChainFormedBlock> LIQUID_INLET = put(entry("liquid_inlet").named("Liquid Inlet")
            .register());
    public static final BlockEntry<ChainFormedBlock> LIQUID_OUTLET = put(entry("liquid_outlet")
            .named("Liquid Outlet").register());
    public static final BlockEntry<ChainFormedBlock> GAS_INLET = put(entry("gas_inlet").named("Gas Inlet")
            .register());
    public static final BlockEntry<ChainFormedBlock> GAS_OUTLET = put(entry("gas_outlet").named("Gas Outlet")
            .register());
    public static final BlockEntry<ChainFormedBlock> POWER_ENTRY = put(entry("power_entry").named("Power Entry")
            .register());
    public static final BlockEntry<ChainFormedBlock> PERIPHERAL_PORT = put(entry("peripheral_port")
            .named("Peripheral Port").register());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChainEntryBlockEntity>> ENTRY_BE =
            CONTENT.blockEntity("chain_entry", ChainEntryBlockEntity::new, ITEM_INLET, ITEM_OUTLET, LIQUID_INLET,
                    LIQUID_OUTLET, GAS_INLET, GAS_OUTLET, POWER_ENTRY, PERIPHERAL_PORT);
    /** The port each entry makes its place, by its id; the peripheral port opens onto no store of the machine's. */
    public static final Map<String, PortKind> PORTS = Map.of("item_inlet", PortKind.ITEM_INPUT,
            "item_outlet", PortKind.ITEM_OUTPUT, "liquid_inlet", PortKind.FLUID_INPUT,
            "liquid_outlet", PortKind.FLUID_OUTPUT, "gas_inlet", PortKind.GAS_INPUT, "gas_outlet", PortKind.GAS_OUTPUT,
            "power_entry", PortKind.ENERGY_INPUT);

    static {
        working();
    }

    private ChainMachineBlocks() {
    }

    /** Declares the blocks, from the mod's constructor, and hands the mod's bus what reaches a machine through them. */
    public static void declare(final IEventBus modBus) {
        modBus.addListener(RegisterCapabilitiesEvent.class,
                event -> MultiblockPorts.register(event, ENTRY_BE.get()));
    }

    /** The block of {@code id}, one of the chain machines' own. */
    public static BlockEntry<? extends Block> byId(final String id) {
        final BlockEntry<? extends Block> block = BY_ID.get(id);
        if (block == null) {
            throw new IllegalArgumentException("no chain machine block " + id);
        }
        return block;
    }

    /** Every block of the chain's machines that a player holds, in the order the shelf shows them. */
    public static Map<String, BlockEntry<? extends Block>> all() {
        return Collections.unmodifiableMap(BY_ID);
    }

    /** The blocks every machine shares: the five casings, the Machine Terminal and the entries. */
    public static List<BlockEntry<? extends Block>> shared() {
        return List.of(MACHINE_CASING, REFRACTORY_CASING, LINED_CASING, VACUUM_CASING, CLEAN_ROOM_CASING, TERMINAL,
                ITEM_INLET, ITEM_OUTLET, LIQUID_INLET, LIQUID_OUTLET, GAS_INLET, GAS_OUTLET, POWER_ENTRY,
                PERIPHERAL_PORT);
    }

    /** Whether {@code id} names an entry: a block of the shape that keeps its own block while its machine stands. */
    public static boolean isEntry(final String id) {
        return PORTS.containsKey(id) || "peripheral_port".equals(id);
    }

    /*
     * Each machine's working blocks, by the names their approved mocks gave them: those of one era's station or line
     * shared where the eras share them (the Wafer Stage, the Feeder Bank), each era's own where they differ.
     */
    private static void working() {
        put(plain("crusher_roll", HardwareEra.VINTAGE).named("Crusher Roll").register());
        put(plain("crusher_drive", HardwareEra.VINTAGE).named("Crusher Drive").register());
        put(plain("grinder_drum", HardwareEra.VINTAGE).named("Grinder Drum").register());
        put(plain("grinder_drive", HardwareEra.VINTAGE).named("Grinder Drive").register());
        put(plain("induction_furnace_coil", HardwareEra.VINTAGE).named("Induction Coil").register());
        put(plain("induction_furnace_capacitor_bank", HardwareEra.VINTAGE).named("Capacitor Bank").register());
        put(plain("electrolytic_cell_cell_stack", HardwareEra.VINTAGE).named("Cell Stack").register());
        put(plain("rolling_mill_mill_housing", HardwareEra.VINTAGE).named("Mill Housing").register());
        put(plain("rolling_mill_work_roll", HardwareEra.VINTAGE).named("Work Roll").register());
        put(plain("rolling_mill_roller_table", HardwareEra.VINTAGE).named("Roller Table").register());
        put(plain("wire_drawer_capstan", HardwareEra.VINTAGE).named("Capstan and Die").register());
        put(plain("coke_oven_oven_chamber", HardwareEra.VINTAGE).named("Oven Chamber").register());
        put(plain("coke_oven_oven_door", HardwareEra.VINTAGE).named("Oven Door").register());
        put(plain("coke_oven_standpipe", HardwareEra.VINTAGE).named("Standpipe").register());
        put(plain("chemical_reactor_vessel", HardwareEra.VINTAGE).named("Reactor Vessel").register());
        put(plain("chemical_reactor_agitator", HardwareEra.VINTAGE).named("Agitator").register());
        put(plain("air_separation_unit_compressor", HardwareEra.VINTAGE).named("Air Compressor").register());
        put(plain("air_separation_unit_adsorber", HardwareEra.VINTAGE).named("Adsorber").register());
        put(plain("air_separation_unit_column", HardwareEra.VINTAGE).named("Distillation Column").register());
        put(plain("air_separation_unit_vaporizer", HardwareEra.VINTAGE).named("Vaporizer").register());
        put(plain("siemens_reactor_bell", HardwareEra.VINTAGE).named("Reactor Bell").register());
        put(plain("siemens_reactor_base_plate", HardwareEra.VINTAGE).named("Reactor Base Plate").register());
        put(plain("crystal_puller_hot_zone", HardwareEra.VINTAGE).named("Hot Zone").register());
        put(plain("crystal_puller_pull_chamber", HardwareEra.VINTAGE).named("Pull Chamber").register());
        put(plain("wafer_saw_wire_spool", HardwareEra.VINTAGE).named("Wire Spool").register());
        put(plain("wafer_saw_wire_web", HardwareEra.VINTAGE).named("Wire Web").register());
        put(plain("lithography_wafer_stage", HardwareEra.VINTAGE).named("Wafer Stage").register());
        put(plain("lithography_glass_optics", HardwareEra.VINTAGE).named("Glass Projection Optics").register());
        put(plain("lithography_fluoride_optics", HardwareEra.TRANSITION).named("Calcium Fluoride Projection Optics")
                .register());
        put(plain("lithography_mirror_optics", HardwareEra.ADVANCED).named("Mirror Projection Optics").register());
        put(plain("lithography_mercury_lamp", HardwareEra.VINTAGE).named("Mercury Lamp").register());
        put(plain("lithography_krf_laser", HardwareEra.LEGACY).named("KrF Laser").register());
        put(plain("lithography_arf_laser", HardwareEra.TRANSITION).named("ArF Laser").register());
        put(plain("lithography_euv_source", HardwareEra.ADVANCED).named("EUV Source").register());
        put(plain("plasma_etcher_etch_chamber", HardwareEra.VINTAGE).named("Etch Chamber").register());
        put(plain("ion_implanter_analyzer_magnet", HardwareEra.VINTAGE).named("Analyzer Magnet").register());
        put(plain("ion_implanter_end_station", HardwareEra.VINTAGE).named("End Station").register());
        put(plain("deposition_chamber_sputter_chamber", HardwareEra.VINTAGE).named("Sputter Chamber").register());
        put(plain("packaging_line_wire_bonder", HardwareEra.VINTAGE).named("Wire Bonder").register());
        put(plain("packaging_line_mould_press", HardwareEra.VINTAGE).named("Mould Press").register());
        put(plain("laminator_hot_platen", HardwareEra.VINTAGE).named("Hot Platen").register());
        put(plain("laminator_press_crown", HardwareEra.VINTAGE).named("Press Crown").register());
        put(plain("board_etcher_spray_tunnel", HardwareEra.VINTAGE).named("Spray Tunnel").register());
        put(plain("board_etcher_etchant_sump", HardwareEra.VINTAGE).named("Etchant Sump").register());
        put(plain("ceramic_kiln_kiln_chamber", HardwareEra.VINTAGE).named("Kiln Chamber").register());
        put(plain("ceramic_kiln_kiln_car", HardwareEra.VINTAGE).named("Kiln Car").register());
        put(plain("coil_winder_winding_head", HardwareEra.VINTAGE).named("Winding Head").register());
        put(plain("coil_winder_supply_spool", HardwareEra.VINTAGE).named("Supply Spool").register());
        put(plain("optical_polisher_polishing_lap", HardwareEra.VINTAGE).named("Polishing Lap").register());
        put(plain("optical_polisher_draw_tower", HardwareEra.VINTAGE).named("Draw Tower").register());
        put(plain("display_line_fill_chamber", HardwareEra.VINTAGE).named("Fill Chamber").register());
        put(plain("display_line_gantry", HardwareEra.VINTAGE).named("Gantry").register());
        put(plain("display_line_test_stand", HardwareEra.VINTAGE).named("Test Stand").register());
        put(plain("assembly_feeder_bank", HardwareEra.VINTAGE).named("Feeder Bank").register());
        put(plain("assembly_wave_solder", HardwareEra.VINTAGE).named("Wave Solder").register());
        put(plain("assembly_vintage_placement_head", HardwareEra.VINTAGE).named("Vintage Placement Head").register());
        put(plain("assembly_legacy_placement_head", HardwareEra.LEGACY).named("Legacy Placement Head").register());
        put(plain("assembly_transition_placement_head", HardwareEra.TRANSITION).named("Transition Placement Head")
                .register());
        put(plain("assembly_standard_placement_head", HardwareEra.STANDARD).named("Standard Placement Head")
                .register());
        put(plain("assembly_advanced_placement_head", HardwareEra.ADVANCED).named("Advanced Placement Head")
                .register());
        put(plain("assembly_transition_reflow_oven", HardwareEra.TRANSITION).named("Transition Reflow Oven")
                .register());
        put(plain("assembly_standard_reflow_oven", HardwareEra.STANDARD).named("Standard Reflow Oven").register());
        put(plain("assembly_advanced_reflow_oven", HardwareEra.ADVANCED).named("Advanced Reflow Oven").register());
        put(plain("prebuilder_case_lift", HardwareEra.VINTAGE).named("Case Lift").register());
        put(plain("prebuilder_robot_arm", HardwareEra.VINTAGE).named("Robot Arm").register());
        put(plain("prebuilder_parts_tray", HardwareEra.VINTAGE).named("Parts Tray").register());
        put(plain("preinstaller_cradle", HardwareEra.VINTAGE).named("Cradle").register());
        put(plain("preinstaller_console", HardwareEra.VINTAGE).named("Console").register());
        put(plain("preinstaller_board_fixture", HardwareEra.VINTAGE).named("Board Fixture").register());
        put(plain("preinstaller_disk_dock", HardwareEra.VINTAGE).named("Disk Dock").register());
    }

    private static <B extends Block> BlockEntry<B> put(final BlockEntry<B> block) {
        BY_ID.put(block.getId().getPath(), block);
        return block;
    }

    /* A block of the shape with nothing of its own, drawn whole of its picture, which merges once formed. */
    private static BlockBuilder<Block> plain(final String id, final Era era) {
        return CONTENT.block(id, Block::new)
                .properties(properties -> properties.mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL))
                .look(IBlockLook.fixed(new IBlockModel.CubeAll(id, TEXTURES + id)))
                .tag(BlockTags.MINEABLE_WITH_PICKAXE).item().tab(shelf(Shelf.CHAIN, era));
    }

    /* An entry: its picture whole while its machine is apart, nothing while it stands formed. */
    private static BlockBuilder<ChainFormedBlock> entry(final String id) {
        final IBlockModel whole = new IBlockModel.CubeAll(id, TEXTURES + id);
        final IBlockModel formed = new IBlockModel.ParticleOnly(id + "_formed", TEXTURES + id);
        return CONTENT.block(id, properties -> new ChainFormedBlock(properties,
                        Device.of(() -> ENTRY_BE.get())))
                .properties(properties -> properties.mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL))
                .look(IBlockLook.byState(state -> state.getValue(ChainFormedBlock.FORMED) ? formed : whole))
                .tag(BlockTags.MINEABLE_WITH_PICKAXE).item().tab(shelf(Shelf.CHAIN, HardwareEra.VINTAGE));
    }
}
