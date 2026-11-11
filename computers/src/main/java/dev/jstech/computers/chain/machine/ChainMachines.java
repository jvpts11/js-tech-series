/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import static dev.jstech.computers.registry.ComputingContent.CONTENT;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.WorkbenchBlockEntity;
import dev.jstech.computers.chain.ChainBenches;
import dev.jstech.computers.menu.LargeScalePrinterMenu;
import dev.jstech.computers.printer.LargeScalePrinterBlockEntity;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.Device.IMenuFactory;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.core.machine.ProcessingMachineBlockEntity.Layout;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.multiblock.BlockMatch;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.core.multiblock.PortKind;
import dev.jstech.core.multiblock.ShapeMatch;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

/**
 * The chain's machines (computers/docs/design/survival.md), each declared once here: its shape as its approved mock
 * draws it, the seconds a step and the watts the design gives it, its slots and tanks for the
 * recipes it works, and what that makes: its pattern, its formed model, its own terminal and that terminal's block
 * entity, and its kind of recipe. The Electronics Assembly Line of an era works the recipes of its era's bench and the
 * older eras', as the bench does, drawing the bench's watts.
 */
public final class ChainMachines {

    private static final Map<String, Declared> DECLARED = new LinkedHashMap<>();
    /* What a tank holds, of a liquid or a gas, in millibuckets, and a gas tank's pressure over the open air's. */
    private static final int TANK = 4_000;
    private static final long PRESSURE = 10;
    private static final String TEXTURES = "block/chain/machine/";
    /* The game's ticks a second, over which a draw in watts is spent as joules a tick. */
    private static final int TICKS = 20;

    static {
        machine("crusher").named("Crusher").step(8, 2_000).slots(1, 2, 0, 0, 0, 0)
                .shape("aba,cac,ada", "aa#,aea,aaa", "aaa,afa,aaa")
                .legend("a=machine_casing,b=item_outlet,c=crusher_drive,d=power_entry,e=crusher_roll,f=item_inlet")
                .declare("crusher_roll");
        machine("grinder").named("Grinder").step(4, 1_000).slots(1, 1, 0, 0, 0, 0)
                .shape("ab,cd", "#b,ef")
                .legend("a=item_outlet,b=grinder_drum,c=power_entry,d=machine_casing,e=grinder_drive,f=item_inlet")
                .declare("grinder_drum");
        machine("induction_furnace").named("Induction Furnace").step(8, 4_000).slots(3, 1, 0, 1, 0, 0)
                .shape("aba,cda,eaa", "aa#,ada,afa", "aaa,gaa,aaa")
                .legend("a=refractory_casing,b=item_outlet,c=liquid_outlet,d=induction_furnace_coil,e=power_entry,"
                        + "f=induction_furnace_capacitor_bank,g=item_inlet")
                .declare("induction_furnace_coil");
        machine("electrolytic_cell").named("Electrolytic Cell").step(10, 3_000).slots(4, 2, 1, 0, 0, 2)
                .shape("abc,dbe", "fb#,fbg")
                .legend("a=item_outlet,b=electrolytic_cell_cell_stack,c=lined_casing,d=power_entry,e=liquid_inlet,"
                        + "f=gas_outlet,g=item_inlet")
                .declare("electrolytic_cell_cell_stack");
        machine("rolling_mill").named("Rolling Mill").step(4, 1_500).slots(1, 1, 0, 0, 0, 0)
                .shape("aba,cde", "#fg,aea")
                .legend("a=machine_casing,b=rolling_mill_roller_table,c=power_entry,d=item_outlet,"
                        + "e=rolling_mill_mill_housing,f=rolling_mill_work_roll,g=item_inlet")
                .declare("rolling_mill_work_roll");
        machine("wire_drawer").named("Wire Drawer").step(4, 1_000).slots(1, 1, 0, 0, 0, 0)
                .shape("a#b", "ccd")
                .legend("a=item_outlet,b=power_entry,c=wire_drawer_capstan,d=item_inlet")
                .declare("wire_drawer_capstan");
        machine("coke_oven").named("Coke Oven").step(30, 0).slots(1, 1, 0, 1, 0, 0)
                .shape("aba,aca,daa", "#ea,acc,aaa", "aaa,afa,aga")
                .legend("a=refractory_casing,b=item_outlet,c=coke_oven_oven_chamber,d=liquid_outlet,"
                        + "e=coke_oven_oven_door,f=item_inlet,g=coke_oven_standpipe")
                .declare("coke_oven_oven_chamber");
        machine("chemical_reactor").named("Chemical Reactor").step(10, 2_000).slots(3, 2, 2, 1, 2, 1)
                .shape("abc,dce", "cb#,cff", "ghi,cjj")
                .legend("a=item_outlet,b=chemical_reactor_vessel,c=lined_casing,d=liquid_outlet,e=power_entry,"
                        + "f=liquid_inlet,g=gas_outlet,h=chemical_reactor_agitator,i=item_inlet,j=gas_inlet")
                .declare("chemical_reactor_vessel");
        machine("air_separation_unit").named("Air Separation Unit").step(10, 3_000).slots(0, 0, 0, 0, 0, 4)
                .shape("abc,ccc,dcc,dcc", "cc#,cee,fcg,fgg", "ccc,ccc,fcc,fcg")
                .legend("a=power_entry,b=air_separation_unit_compressor,c=vacuum_casing,"
                        + "d=air_separation_unit_vaporizer,e=air_separation_unit_adsorber,f=gas_outlet,"
                        + "g=air_separation_unit_column")
                .works(AirSeparationBlockEntity::new).declare("air_separation_unit_column");
        machine("siemens_reactor").named("Siemens Reactor").step(60, 5_000).slots(1, 1, 0, 0, 2, 0)
                .shape("ab,cd", "e#,fb", "eb,fg")
                .legend("a=siemens_reactor_base_plate,b=refractory_casing,c=item_outlet,d=power_entry,"
                        + "e=siemens_reactor_bell,f=gas_inlet,g=item_inlet")
                .declare("siemens_reactor_bell");
        machine("crystal_puller").named("Crystal Puller").step(120, 6_000).slots(4, 1, 0, 0, 1, 0)
                .shape("ab,cd", "c#,eb", "fb,bg")
                .legend("a=item_outlet,b=refractory_casing,c=crystal_puller_hot_zone,d=power_entry,e=gas_inlet,"
                        + "f=crystal_puller_pull_chamber,g=item_inlet")
                .declare("crystal_puller_hot_zone");
        machine("wafer_saw").named("Wafer Saw").step(20, 2_000).slots(1, 1, 0, 0, 0, 0)
                .shape("ab,cd", "b#,ef")
                .legend("a=wafer_saw_wire_web,b=machine_casing,c=item_outlet,d=power_entry,e=wafer_saw_wire_spool,"
                        + "f=item_inlet")
                .declare("wafer_saw_wire_web");
        machine("plasma_etcher").named("Plasma Etcher").step(10, 3_000).slots(1, 1, 1, 0, 2, 0)
                .shape("ab,cd", "e#,fd")
                .legend("a=item_inlet,b=power_entry,c=item_outlet,d=gas_inlet,e=plasma_etcher_etch_chamber,"
                        + "f=liquid_inlet")
                .works(WaferMachineBlockEntity::new).declare("plasma_etcher_etch_chamber");
        machine("ion_implanter").named("Ion Implanter").step(10, 4_000).slots(2, 1, 0, 0, 0, 0)
                .shape("abb,cbd", "ef#,bbb")
                .legend("a=item_inlet,b=vacuum_casing,c=item_outlet,d=power_entry,e=ion_implanter_end_station,"
                        + "f=ion_implanter_analyzer_magnet")
                .works(WaferMachineBlockEntity::new).declare("ion_implanter_analyzer_magnet");
        machine("deposition_chamber").named("Deposition Chamber").step(10, 3_000).slots(3, 1, 0, 0, 0, 0)
                .shape("ab,cd", "e#,ee")
                .legend("a=item_inlet,b=vacuum_casing,c=item_outlet,d=power_entry,e=deposition_chamber_sputter_chamber")
                .works(WaferMachineBlockEntity::new).declare("deposition_chamber_sputter_chamber");
        machine("packaging_line").named("Packaging Line").step(20, 2_000).slots(4, 1, 0, 0, 0, 0)
                .shape("abc,bdb", "ef#,bbb")
                .legend("a=item_outlet,b=machine_casing,c=item_inlet,d=power_entry,e=packaging_line_mould_press,"
                        + "f=packaging_line_wire_bonder")
                .works(WaferMachineBlockEntity::new).declare("packaging_line_wire_bonder");
        machine("laminator").named("Laminator").step(10, 1_500).slots(3, 1, 0, 0, 0, 0)
                .shape("abc,dee", "eb#,efe")
                .legend("a=item_outlet,b=laminator_hot_platen,c=item_inlet,d=power_entry,e=machine_casing,"
                        + "f=laminator_press_crown")
                .declare("laminator_hot_platen");
        machine("board_etcher").named("Board Etcher").step(10, 1_500).slots(1, 1, 1, 0, 0, 0)
                .shape("abc,def", "gg#,fff")
                .legend("a=item_outlet,b=board_etcher_etchant_sump,c=item_inlet,d=power_entry,e=liquid_inlet,"
                        + "f=lined_casing,g=board_etcher_spray_tunnel")
                .declare("board_etcher_spray_tunnel");
        machine("ceramic_kiln").named("Ceramic Kiln").step(20, 3_000).slots(3, 1, 0, 0, 0, 0)
                .shape("ab,cd", "e#,ef")
                .legend("a=ceramic_kiln_kiln_car,b=item_inlet,c=item_outlet,d=power_entry,e=ceramic_kiln_kiln_chamber,"
                        + "f=refractory_casing")
                .declare("ceramic_kiln_kiln_chamber");
        machine("coil_winder").named("Coil Winder").step(4, 500).slots(3, 1, 0, 0, 0, 0)
                .shape("abc", "de#")
                .legend("a=item_outlet,b=power_entry,c=item_inlet,d=coil_winder_supply_spool,"
                        + "e=coil_winder_winding_head")
                .declare("coil_winder_winding_head");
        machine("optical_polisher").named("Optical Polisher").step(10, 1_000).slots(2, 1, 0, 0, 0, 0)
                .shape("ab,cd", "e#,fe")
                .legend("a=optical_polisher_polishing_lap,b=item_inlet,c=item_outlet,d=power_entry,e=machine_casing,"
                        + "f=optical_polisher_draw_tower")
                .declare("optical_polisher_polishing_lap");
        machine("display_line").named("Display Line").step(30, 3_000).slots(4, 1, 2, 0, 0, 0)
                .shape("abc,bdb", "ef#,ggb", "bhb,bbb")
                .legend("a=item_outlet,b=vacuum_casing,c=item_inlet,d=power_entry,e=display_line_fill_chamber,"
                        + "f=display_line_test_stand,g=liquid_inlet,h=display_line_gantry")
                .declare("display_line_test_stand");
        machine("lithography_vintage").named("Vintage Lithography Station").era(HardwareEra.VINTAGE)
                .step(20, 2_000).slots(2, 1, 1, 0, 0, 0)
                .shape("abc,dae", "af#,aga", "aha,aaa")
                .legend("a=clean_room_casing,b=lithography_wafer_stage,c=item_inlet,d=power_entry,e=item_outlet,"
                        + "f=lithography_glass_optics,g=liquid_inlet,h=lithography_mercury_lamp")
                .works(WaferMachineBlockEntity::new).declare("lithography_mercury_lamp");
        machine("lithography_legacy").named("Legacy Lithography Station").era(HardwareEra.LEGACY)
                .step(20, 3_000).slots(2, 1, 1, 0, 1, 0).linked()
                .shape("abc,dae", "fg#,hij", "aaa,aaa")
                .legend("a=clean_room_casing,b=lithography_wafer_stage,c=item_inlet,d=power_entry,e=item_outlet,"
                        + "f=lithography_krf_laser,g=lithography_glass_optics,h=gas_inlet,i=liquid_inlet,"
                        + "j=peripheral_port")
                .works(WaferMachineBlockEntity::new).declare("lithography_krf_laser");
        // The Transition's and the Standard's stations are built of the same blocks, so they are one, of both eras.
        machine("lithography_arf").named("ArF Lithography Station").era(HardwareEra.TRANSITION)
                .serves(HardwareEra.TRANSITION, 4_000).serves(HardwareEra.STANDARD, 6_000)
                .step(20, 6_000).slots(2, 1, 2, 0, 1, 0).linked()
                .shape("abc,def", "gh#,iej", "aaa,aaa")
                .legend("a=clean_room_casing,b=lithography_wafer_stage,c=item_inlet,d=power_entry,e=liquid_inlet,"
                        + "f=item_outlet,g=lithography_arf_laser,h=lithography_fluoride_optics,i=gas_inlet,"
                        + "j=peripheral_port")
                .works(WaferMachineBlockEntity::new).declare("lithography_arf_laser");
        machine("lithography_advanced").named("Advanced Lithography Station").era(HardwareEra.ADVANCED)
                .step(20, 10_000).slots(3, 1, 1, 0, 0, 0).linked()
                .shape("abc,dae", "fg#,ahi", "aaa,aaa")
                .legend("a=clean_room_casing,b=lithography_wafer_stage,c=item_inlet,d=power_entry,e=item_outlet,"
                        + "f=lithography_euv_source,g=lithography_mirror_optics,h=liquid_inlet,i=peripheral_port")
                .works(WaferMachineBlockEntity::new).declare("lithography_euv_source");
        for (final Era era : List.of(HardwareEra.VINTAGE, HardwareEra.LEGACY, HardwareEra.TRANSITION,
                HardwareEra.STANDARD, HardwareEra.ADVANCED)) {
            final String solder = era.isAtMost(HardwareEra.LEGACY) ? "assembly_wave_solder"
                    : "assembly_" + era.path() + "_reflow_oven";
            machine("assembly_" + era.path()).named(englishEra(era) + " Electronics Assembly Line").era(era)
                    .step(0, WorkbenchBlockEntity.watts(era)).slots(WorkbenchBlockEntity.TRAYS, 1, 0, 0, 0, 0)
                    .shape("abc,ded", "fg#,ddd")
                    .legend("a=item_outlet,b=assembly_feeder_bank,c=item_inlet,d=machine_casing,e=power_entry,f="
                            + solder + ",g=assembly_" + era.path() + "_placement_head")
                    .declare("assembly_" + era.path() + "_placement_head");
        }
        // The two that turn parts into working computers, one of each for every era, doing work of their own.
        machine("prebuilder").named("Prebuilder").step(10, 1_000).slots(PrebuilderBlockEntity.INPUTS, 1, 0, 0, 0, 0)
                .shape("abc,dee", "#ff,eeg", "eee,eee")
                .legend("a=item_outlet,b=prebuilder_case_lift,c=item_inlet,d=power_entry,e=machine_casing,"
                        + "f=prebuilder_robot_arm,g=prebuilder_parts_tray")
                .works(PrebuilderBlockEntity::new).declare();
        machine("preinstaller").named("Preinstaller").step(10, 1_000)
                .slots(PreinstallerBlockEntity.INPUTS, 1, 0, 0, 0, 0)
                .shape("ab,cd", "#e,fg")
                .legend("a=item_outlet,b=preinstaller_cradle,c=power_entry,d=item_inlet,e=preinstaller_console,"
                        + "f=preinstaller_disk_dock,g=preinstaller_board_fixture")
                .works(PreinstallerBlockEntity::new).declare();
        // The Legacy's book factory and the only printer of books, with a window of its own; its draw is an estimate.
        machine("large_scale_printer").named("Large Scale Printer").step(2, 2_000)
                .slots(LargeScalePrinterBlockEntity.INPUTS, LargeScalePrinterBlockEntity.OUTPUTS, 0, 0, 0, 0)
                .shape("abbc,bbbd,ebbb", "fgg#,bbbb,bbbh", "bbib,bbbb,bbbb")
                .legend("a=item_outlet,b=machine_casing,c=large_scale_printer_paper_feeder,d=item_inlet,"
                        + "e=power_entry,f=large_scale_printer_perfect_binder,g=large_scale_printer_print_unit,"
                        + "h=peripheral_port,i=large_scale_printer_inking_unit")
                .works(LargeScalePrinterBlockEntity::new).opens(LargeScalePrinterMenu::open).declare();
    }

    private ChainMachines() {
    }

    /** Declares the machines, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** Every machine of the chain, in the order the design lists them. */
    public static Collection<ChainMachine> all() {
        return DECLARED.values().stream().map(Declared::machine).toList();
    }

    /** The machine of {@code id}, if it is one. */
    public static Optional<ChainMachine> byId(final String id) {
        return Optional.ofNullable(DECLARED.get(id)).map(Declared::machine);
    }

    /** The block entity type of {@code machine}'s terminal. */
    public static BlockEntityType<ChainMachineBlockEntity> typeOf(final ChainMachine machine) {
        return DECLARED.get(machine.id()).type().get();
    }

    /** The block of {@code machine}'s own terminal. */
    public static BlockEntry<ChainMachineTerminalBlock> terminalOf(final ChainMachine machine) {
        return DECLARED.get(machine.id()).terminal();
    }

    /** The kinds of recipe {@code machine} works, the first first, each with the era it is a station's for. */
    public static List<Worked> workedBy(final ChainMachine machine) {
        return DECLARED.get(machine.id()).worked();
    }

    /** Every machine's terminal block entity type, for the renderers and the capabilities. */
    public static List<DeferredHolder<BlockEntityType<?>, BlockEntityType<ChainMachineBlockEntity>>> types() {
        return DECLARED.values().stream().map(Declared::type).toList();
    }

    /**
     * Makes the Machine Terminal at {@code pos} the terminal of the machine whose shape stands whole round it, facing
     * {@code facing}; whether one did.
     */
    public static boolean formAt(final ServerLevel level, final BlockPos pos, final Direction facing) {
        for (final Declared declared : DECLARED.values()) {
            final Optional<MultiblockPattern> pattern = MultiblockPatterns.get(declared.machine().patternId());
            if (pattern.isPresent() && pattern.get().check(MultiblockPatterns.provider(level), pos.asLong())
                    instanceof ShapeMatch.Formed) {
                level.setBlock(pos, declared.terminal().get().defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, facing), Block.UPDATE_ALL);
                return true;
            }
        }
        return false;
    }

    private static Builder machine(final String id) {
        return new Builder(id);
    }

    /* The English name of an era as a machine's name carries it: "Vintage". */
    private static String englishEra(final Era era) {
        final String path = era.path();
        return Character.toUpperCase(path.charAt(0)) + path.substring(1);
    }

    /**
     * A kind of recipe a machine works, and the era whose recipes those are when the machine is a Lithography Station,
     * which asks for the computer of the era before; none for every other machine. A station of two eras draws each
     * era's own on its recipes.
     *
     * @param kind        the kind
     * @param era         the era of a station's kind; none for another machine's
     * @param joulesATick what a tick of its recipes spends when they say nothing, in joules; 0 for the machine's own
     */
    public record Worked(ProcessingKind kind, @Nullable Era era, int joulesATick) {
    }

    /* Makes the block entity of a machine's terminal. */
    @FunctionalInterface
    private interface IEntityMaker {

        ChainMachineBlockEntity make(ChainMachine machine, BlockPos pos, BlockState state);
    }

    /* A machine as declared: its record, its terminal, its terminal's type and the kinds of recipe it works. */
    private record Declared(ChainMachine machine, BlockEntry<ChainMachineTerminalBlock> terminal,
                            DeferredHolder<BlockEntityType<?>, BlockEntityType<ChainMachineBlockEntity>> type,
                            List<Worked> worked) {
    }

    /* Declares one machine, a step at a time as its line in the table reads. */
    private static final class Builder {

        private final String id;
        private String english = "";
        private Era era;
        private int seconds;
        private int watts;
        private Layout layout = Layout.items(0, 0);
        private List<List<String>> layers = List.of();
        private Map<Character, String> legend = Map.of();
        private final Map<Era, Integer> serves = new LinkedHashMap<>();
        private boolean linked;
        private IEntityMaker entity = ChainMachineBlockEntity::new;
        private IMenuFactory<ChainMachineBlockEntity> menu = MachineMenu::new;

        Builder(final String id) {
            this.id = id;
        }

        /* A machine that does work of its own rather than recipes: its terminal's block entity. */
        Builder works(final IEntityMaker maker) {
            this.entity = maker;
            return this;
        }

        /* A machine with a window of its own instead of every machine's: what its terminal opens. */
        Builder opens(final IMenuFactory<ChainMachineBlockEntity> factory) {
            this.menu = factory;
            return this;
        }

        Builder named(final String name) {
            this.english = name;
            return this;
        }

        Builder era(final Era of) {
            this.era = of;
            return this;
        }

        /* An era whose recipes a station works, when they are more than its own, and what it draws on them. */
        Builder serves(final Era of, final int drawWatts) {
            this.serves.put(of, drawWatts);
            return this;
        }

        Builder step(final int stepSeconds, final int drawWatts) {
            this.seconds = stepSeconds;
            this.watts = drawWatts;
            return this;
        }

        /* Its item inputs and outputs, then its liquid tanks in and out, then its gas tanks in and out. */
        Builder slots(final int in, final int out, final int liquidIn, final int liquidOut, final int gasIn,
                      final int gasOut) {
            Layout made = Layout.items(in, out);
            if (liquidIn + liquidOut > 0) {
                made = made.withTanks(liquidIn, liquidOut, TANK);
            }
            if (gasIn + gasOut > 0) {
                made = made.withMatter(MatterState.GAS, gasIn, gasOut, TANK, PRESSURE);
            }
            this.layout = made;
            return this;
        }

        Builder linked() {
            this.linked = true;
            return this;
        }

        /* Its layers from the bottom, each its rows from the front split by commas. */
        Builder shape(final String... drawn) {
            this.layers = ChainShapes.layers(drawn);
            return this;
        }

        Builder legend(final String drawn) {
            this.legend = ChainShapes.legend(drawn);
            return this;
        }

        /* Declares a machine of recipes: its pattern, model, terminal and type, its kind of recipe shown by hero. */
        void declare(final String hero) {
            declareWith(machine -> kinds(machine, hero));
        }

        /* Declares a machine that does work of its own, with no kind of recipe. */
        void declare() {
            declareWith(machine -> List.of());
        }

        private void declareWith(final Function<ChainMachine, List<Worked>> worked) {
            if (!ChainShapes.sound(layers, legend)) {
                throw new IllegalStateException("the shape of " + id + " is not a whole box with one terminal");
            }
            final ChainMachine machine = new ChainMachine(id, english, era, seconds, watts, layout, layers, legend,
                    linked);
            pattern(machine);
            CONTENT.model("chain/" + id).file("jsmodels/chain/" + id + ".glb").formedFrom(machine.patternId())
                    .register();
            final String casing = TEXTURES + legend.get(layers.getFirst().getFirst().charAt(0));
            final IMenuFactory<ChainMachineBlockEntity> opened = menu;
            final BlockEntry<ChainMachineTerminalBlock> terminal = CONTENT.block(machine.terminalPath(),
                            properties -> new ChainMachineTerminalBlock(properties,
                                    Device.of(() -> DECLARED.get(id).type().get())
                                            .ticks(MachineBlockEntity::serverTick).opensMenu(opened), id))
                    .named(english + " Terminal")
                    .properties(properties -> properties.mapColor(MapColor.METAL).strength(3.0F)
                            .sound(SoundType.METAL))
                    .look(IBlockLook.facing(new IBlockModel.Orientable(machine.terminalPath(), casing,
                            TEXTURES + "terminal", casing)).whileOn(ChainFormedBlock.FORMED,
                            new IBlockModel.ParticleOnly(machine.terminalPath() + "_formed", TEXTURES + "terminal")))
                    .drops(Drops.NONE).tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .register();
            final IEntityMaker maker = entity;
            final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChainMachineBlockEntity>> type =
                    CONTENT.blockEntity(machine.terminalPath(), (pos, state) -> maker.make(machine, pos, state),
                            terminal);
            DECLARED.put(id, new Declared(machine, terminal, type, worked.apply(machine)));
        }

        /*
         * The kinds of recipe it works: an assembly line its era's bench's and the older eras', a station a kind for
         * each era it serves, every other machine a kind of its own.
         */
        private List<Worked> kinds(final ChainMachine machine, final String hero) {
            final BlockEntry<? extends Block> shownBy = ChainMachineBlocks.byId(hero);
            if (id.startsWith("assembly_")) {
                final List<Worked> bench = new ArrayList<>();
                for (final ProcessingKind kind : ChainBenches.assembledAt(era)) {
                    kind.alsoWorkedBy(shownBy);
                    bench.add(new Worked(kind, null, 0));
                }
                return Collections.unmodifiableList(bench);
            }
            if (era == null) {
                final ProcessingKind made = CONTENT.processing(id, english, shownBy).exactInputs();
                return List.of(new Worked(machine.takesEnergy() ? made.runsOn(JsEnergy.ENERGY) : made, null, 0));
            }
            final List<Worked> stations = new ArrayList<>();
            for (final Map.Entry<Era, Integer> of : (serves.isEmpty() ? Map.of(era, watts) : serves).entrySet()) {
                final Era served = of.getKey();
                stations.add(new Worked(CONTENT.processing(served.path() + "_lithography", englishEra(served)
                        + " Lithography", shownBy).runsOn(JsEnergy.ENERGY).exactInputs(), served,
                        of.getValue() / TICKS));
            }
            return List.copyOf(stations);
        }

        /* Its pattern: each letter its block, each entry the port it makes, the terminal its own. */
        private static void pattern(final ChainMachine machine) {
            final MultiblockPattern.Builder drawn = MultiblockPattern.builder(machine.id());
            for (final List<String> layer : machine.layers()) {
                drawn.layer(layer.toArray(String[]::new));
            }
            machine.legend().forEach((letter, block) -> {
                drawn.where(letter, BlockMatch.blocks(JsComputers.MODID + ":" + block));
                final PortKind port = ChainMachineBlocks.PORTS.get(block);
                if (port != null) {
                    drawn.port(letter, port);
                }
            });
            drawn.terminal(JsComputers.MODID + ":" + machine.terminalPath());
            MultiblockPatterns.declare(machine.patternId(), drawn.build());
        }
    }
}
