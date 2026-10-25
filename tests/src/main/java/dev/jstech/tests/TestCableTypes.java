/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.cable.BreachRules;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.CoreCables;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.fluid.CoreFluidTags;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.grid.GridPolicies;
import dev.jstech.core.jstechseries.peripheral.ControlLinks;
import dev.jstech.core.matter.PressureBursts;
import dev.jstech.industrial.line.CompressedAir;
import dev.jstech.industrial.line.IndustrialPipeMarks;
import dev.jstech.industrial.line.PipeFailures;
import dev.jstech.industrial.line.PneumaticPost;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Cables only the tests lay, registered with the Core as any mod's cables are: a long-distance cable, six pixels thick,
 * which never shares a block, an energy cable that loses a tenth of what crosses each block of it, four pipes, a plain
 * one, the same insulated, one that holds pressure and one of rubber that bursts when something too hot reaches it, a
 * gas line, a thin gas line that bursts past ten atmospheres, gas lines of steel and stainless steel under J's
 * Industrial's rules, a Compressed Air Line and a capsule tube of its, a slurry line, a line of items, a line that
 * groups the machines it joins and a cable of the industrial control line. Each is laid by a vanilla
 * item and drawn in a jacket and plug the series already has, so the test mod declares no item, texture or model of
 * its own.
 */
public final class TestCableTypes {

    public static final long PLAIN_RATE = 100L;
    public static final int PLAIN_HOTTEST = 1_000;
    public static final long PRESSURE_RATE = 800L;
    public static final int PRESSURE_HOTTEST = 2_000;
    public static final long GAS_RATE = 500L;
    public static final long SLURRY_RATE = 200L;
    public static final long ITEM_RATE = 8L;
    public static final long HEAT_PIPE_CONDUCTANCE = 200L;
    public static final int RUBBER_HOTTEST = 500;
    public static final int RUBBER_BURSTS_AFTER = 20;
    /** The thin gas line: the pressure it holds, and what a block of it holds inside, in millibuckets. */
    public static final long THIN_GAS_HOLDS = 10L;
    public static final long THIN_GAS_INSIDE = 8L;
    /** How long the test steel gas line stands hydrogen, in ticks: two spans of the lines' counting. */
    public static final int STEEL_WEARS_IN = 40;
    public static final DeferredRegister<CableType> CABLES = DeferredRegister.create(CoreCables.KEY, JsTests.MODID);
    /** A grid of the test mod's own, which shares energy by the Core's policy, as a mod's line of a voltage class. */
    public static final GridKind OWN_POWER = GridKind.declare(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
            "own_power"), GridPolicies.ENERGY);
    /** An energy cable on the test mod's own grid, which never joins the Core's energy cables. */
    public static final DeferredHolder<CableType, CableType> OWN_GRID_ENERGY = CABLES.register("own_grid_energy",
            () -> CableType.builder(Connection.of(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                            "own_grid_energy")))
                    .grid(OWN_POWER).alone().carries(Long.MAX_VALUE, 0)
                    .jacket(ResourceLocation.fromNamespaceAndPath("jscore", "block/cable/insulated_copper"))
                    .plug(ResourceLocation.fromNamespaceAndPath("jscore", "block/cable/plug/insulated_copper"))
                    .build(() -> Items.GLOWSTONE_DUST));
    public static final DeferredHolder<CableType, CableType> LONG_DISTANCE = CABLES.register("long_distance",
            () -> CableType.builder(Connection.of(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                            "long_distance")))
                    .alone().thickness(6).carries(1L, 0)
                    .jacket(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/hbw"))
                    .plug(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/plug/hbw"))
                    .build(() -> Items.STRING));
    public static final DeferredHolder<CableType, CableType> LOSSY_ENERGY = CABLES.register("lossy_energy",
            () -> CableType.builder(Connection.of(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                            "lossy_energy")))
                    .grid(GridKind.POWER).alone().carries(Long.MAX_VALUE, 0).loses(100_000)
                    .jacket(ResourceLocation.fromNamespaceAndPath("jscore", "block/cable/insulated_copper"))
                    .plug(ResourceLocation.fromNamespaceAndPath("jscore", "block/cable/plug/insulated_copper"))
                    .build(() -> Items.REDSTONE));
    /** The coolest pipe: a hundred millibuckets a tick, nothing above 1000 K, no gas and nothing corrosive. */
    public static final DeferredHolder<CableType, CableType> PLAIN_PIPE = CABLES.register("plain_pipe",
            () -> CableType.builder(Connection.of(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                            "plain_pipe")))
                    .grid(GridKind.FLUID).alone().carries(PLAIN_RATE, 0).withstands(0, PLAIN_HOTTEST)
                    .jacket(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/hpc"))
                    .plug(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/plug/qsfp"))
                    .build(() -> Items.BUCKET));
    /** The plain pipe insulated: what it carries never freezes in it. */
    public static final DeferredHolder<CableType, CableType> INSULATED_PIPE = CABLES.register("insulated_pipe",
            () -> CableType.builder(Connection.of(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                            "insulated_pipe")))
                    .grid(GridKind.FLUID).alone().carries(PLAIN_RATE, 0).withstands(0, PLAIN_HOTTEST).insulated()
                    .jacket(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/hbw"))
                    .plug(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/plug/qsfp"))
                    .build(() -> Items.WHITE_WOOL));
    /** A pipe that holds pressure: eight hundred millibuckets a tick, up to 2000 K, gases and corrosives. */
    public static final DeferredHolder<CableType, CableType> PRESSURE_PIPE = CABLES.register("pressure_pipe",
            () -> CableType.builder(Connection.of(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                            "pressure_pipe")))
                    .grid(GridKind.FLUID).alone().carries(PRESSURE_RATE, 0).withstands(0, PRESSURE_HOTTEST)
                    .takes(CoreFluidTags.GASES).takes(CoreFluidTags.CORROSIVE)
                    .jacket(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/crafting"))
                    .plug(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/plug/crafting"))
                    .build(() -> Items.IRON_BARS));
    /** A gas line, holding pressure: five hundred millibuckets a tick. */
    public static final DeferredHolder<CableType, CableType> GAS_LINE = CABLES.register("gas_line",
            () -> CableType.builder(Connection.of(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                            "gas_line")))
                    .grid(GridKind.GAS).alone().carries(GAS_RATE, 0)
                    .jacket(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/hpc"))
                    .plug(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/plug/qsfp"))
                    .build(() -> Items.PHANTOM_MEMBRANE));
    /** A thin gas line: it holds ten atmospheres, and bursts the first tick a gas comes at more. */
    public static final DeferredHolder<CableType, CableType> THIN_GAS_LINE = CABLES.register("thin_gas_line",
            () -> CableType.builder(Connection.of(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                            "thin_gas_line")))
                    .grid(GridKind.GAS).alone().carries(GAS_RATE, 0).holds(THIN_GAS_HOLDS)
                    .beyond(BreachRules.breaksAfter(1, PressureBursts.pipe(THIN_GAS_INSIDE)))
                    .jacket(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/hpc"))
                    .plug(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/plug/qsfp"))
                    .build(() -> Items.GLASS_BOTTLE));
    /**
     * A plain steel gas line under J's Industrial's rules, which hydrogen wears through in two spans of counting and a
     * corrosive gas eats, falling as an iron nugget.
     */
    public static final DeferredHolder<CableType, CableType> STEEL_GAS_LINE = CABLES.register("steel_gas_line",
            () -> CableType.builder(Connection.of(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                            "steel_gas_line")))
                    .grid(GridKind.GAS).alone().carries(GAS_RATE, 0)
                    .wornBy(IndustrialPipeMarks.HYDROGEN, STEEL_WEARS_IN)
                    .beyond(PipeFailures.rule(() -> new ItemStack(Items.IRON_NUGGET), THIN_GAS_INSIDE))
                    .jacket(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/hpc"))
                    .plug(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/plug/qsfp"))
                    .build(() -> Items.CHAIN));
    /** A stainless gas line under J's Industrial's rules, made for the corrosive gases. */
    public static final DeferredHolder<CableType, CableType> STAINLESS_GAS_LINE = CABLES.register(
            "stainless_gas_line", () -> CableType.builder(Connection.of(ResourceLocation.fromNamespaceAndPath(
                            JsTests.MODID, "stainless_gas_line")))
                    .grid(GridKind.GAS).alone().carries(GAS_RATE, 0).takes(IndustrialPipeMarks.CORROSIVE_GASES)
                    .beyond(PipeFailures.rule(() -> new ItemStack(Items.IRON_NUGGET), THIN_GAS_INSIDE))
                    .jacket(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/hpc"))
                    .plug(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/plug/qsfp"))
                    .build(() -> Items.LANTERN));
    /** A Compressed Air Line of J's Industrial's line, holding ten atmospheres. */
    public static final DeferredHolder<CableType, CableType> COMPRESSED_AIR_LINE = CABLES.register(
            "compressed_air_line", () -> CableType.builder(CompressedAir.LINE)
                    .grid(GridKind.GAS).alone().carries(GAS_RATE, 0).holds(THIN_GAS_HOLDS)
                    .jacket(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/hpc"))
                    .plug(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/plug/qsfp"))
                    .build(() -> Items.FEATHER));
    /** A capsule tube of J's Industrial's pneumatic post, whose runs join the stations on them. */
    public static final DeferredHolder<CableType, CableType> CAPSULE_TUBE = CABLES.register("capsule_tube",
            () -> CableType.builder(PneumaticPost.LINE).grid(PneumaticPost.TUBES).alone()
                    .jacket(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/hbw"))
                    .plug(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/plug/hbw"))
                    .build(() -> Items.PAPER));
    /** A lined line of slurry: two hundred millibuckets a tick. */
    public static final DeferredHolder<CableType, CableType> SLURRY_LINE = CABLES.register("slurry_line",
            () -> CableType.builder(Connection.of(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                            "slurry_line")))
                    .grid(GridKind.SLURRY).alone().carries(SLURRY_RATE, 0)
                    .jacket(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/crafting"))
                    .plug(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/plug/crafting"))
                    .build(() -> Items.CLAY_BALL));

    /** A line of items: eight items a tick. */
    public static final DeferredHolder<CableType, CableType> ITEM_LINE = CABLES.register("item_line",
            () -> CableType.builder(Connection.of(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                            "item_line")))
                    .grid(GridKind.ITEM).alone().carries(ITEM_RATE, 0)
                    .jacket(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/crafting"))
                    .plug(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/plug/crafting"))
                    .build(() -> Items.HOPPER));
    /** A heat pipe: two hundred watts a kelvin between all the blocks it plugs into, whatever its length. */
    public static final DeferredHolder<CableType, CableType> HEAT_PIPE = CABLES.register("heat_pipe",
            () -> CableType.builder(Connection.of(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                            "heat_pipe")))
                    .grid(GridKind.HEAT).alone().carries(HEAT_PIPE_CONDUCTANCE, 0)
                    .jacket(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/hpc"))
                    .plug(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/plug/qsfp"))
                    .build(() -> Items.BLAZE_ROD));
    /** A grid of the test mod's own whose runs group the machines they join. */
    public static final GridKind GROUPS = GridKind.declare(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
            "groups"), GridPolicies.GROUP);
    /** A line that carries nothing and groups the machines it joins. */
    public static final DeferredHolder<CableType, CableType> GROUP_LINE = CABLES.register("group_line",
            () -> CableType.builder(Connection.of(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                            "group_line")))
                    .grid(GROUPS).alone()
                    .jacket(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/hbw"))
                    .plug(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/plug/hbw"))
                    .build(() -> Items.LEAD));
    /** A pipe of rubber, which stands nothing above 500 K and bursts after a second of something hotter, spilling it. */
    public static final DeferredHolder<CableType, CableType> RUBBER_PIPE = CABLES.register("rubber_pipe",
            () -> CableType.builder(Connection.of(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                            "rubber_pipe")))
                    .grid(GridKind.FLUID).alone().carries(PLAIN_RATE, 0).withstands(0, RUBBER_HOTTEST)
                    .beyond(BreachRules.breaksAfter(RUBBER_BURSTS_AFTER,
                            (level, breach) -> BreachRules.spill(level, breach.at(), breach.what())))
                    .jacket(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/hpc"))
                    .plug(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/plug/qsfp"))
                    .build(() -> Items.KELP));
    /** How many cables the test control cable reaches across, as the industrial control cable's eight. */
    public static final int CONTROL_REACH = 8;
    /** A cable of the Core's industrial control line, as a mod declares its own: a controller to its machines. */
    public static final DeferredHolder<CableType, CableType> CONTROL = CABLES.register("control",
            () -> CableType.builder(ControlLinks.LINE)
                    .grid(ControlLinks.GRID).alone().carries(0L, CONTROL_REACH)
                    .jacket(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/hbw"))
                    .plug(ResourceLocation.fromNamespaceAndPath("jsc", "block/cable/plug/hbw"))
                    .build(() -> Items.TRIPWIRE_HOOK));

    private TestCableTypes() {
    }

    public static void register(final IEventBus modEventBus) {
        CABLES.register(modEventBus);
    }
}
