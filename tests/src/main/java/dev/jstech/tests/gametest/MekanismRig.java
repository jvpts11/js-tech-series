/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.block.part.CraftingInterfacePart;
import dev.jstech.computers.block.part.CraftingRouterPart;
import dev.jstech.computers.block.part.ReceivingBusPart;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.crafting.CraftingFloor;
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.testkit.CraftingRig;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.Upgrade;
import mekanism.common.tile.component.TileComponentUpgrade;
import mekanism.common.tile.factory.TileEntityFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * One Mekanism machine fed by a Crafting Interface, for the machine GameTests and client tests. The network's crafting
 * cable (light blue) leaves the Crafting Computer (5,2,2) southward along x=5; the interface sits on (5,2,4) facing
 * east into a crafting cable of its own (red), which climbs over the machine and runs under it, so a router can be put
 * against each input face. Mekanism machines take inputs on their top and, for the "extra" slot, their bottom; they
 * give outputs on their right face (west, for the factory north orientation) and, for two-output machines, their left
 * face (east) too; energy goes in through any face. Routers go on the inputs, Receiving Buses on the outputs, each on
 * the cable of its colour. Machines are placed the way a player places them, so their factory side configuration
 * applies, and every input is routed through the router whose face takes it.
 */
public final class MekanismRig {

    public static final int SETTLE = 4;
    public static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    public static final BlockPos INTERFACE = new BlockPos(5, 2, 4);
    public static final BlockPos MACHINE = new BlockPos(6, 2, 7);
    public static final BlockPos CABLE_WEST = new BlockPos(5, 2, 7);
    public static final BlockPos CABLE_ABOVE = new BlockPos(6, 3, 7);
    public static final BlockPos CABLE_BELOW = new BlockPos(6, 1, 7);
    public static final BlockPos CABLE_EAST = new BlockPos(7, 2, 7);
    public static final BlockPos CABLE_NORTH = new BlockPos(6, 2, 6);

    /*
     * A second machine of the same kind, further south, for tests that need two physical machines. It has an
     * interface of its own on (5,2,9) facing east into its own cable (orange); its routers hang from B_ABOVE (top
     * input) and B_BELOW (bottom extra), its Receiving Bus from B_RUN (right, west).
     */
    public static final BlockPos INTERFACE_B = new BlockPos(5, 2, 9);
    public static final BlockPos MACHINE_B = new BlockPos(6, 2, 10);
    public static final BlockPos B_RUN = new BlockPos(5, 2, 10);
    public static final BlockPos B_ABOVE = new BlockPos(6, 3, 10);
    public static final BlockPos B_BELOW = new BlockPos(6, 1, 10);

    private static final DyeColor NETWORK = DyeColor.LIGHT_BLUE;
    private static final DyeColor OWN = DyeColor.RED;
    private static final DyeColor OWN_B = DyeColor.ORANGE;
    private static final CraftingFloor.Site SITE = new CraftingFloor.Site(INTERFACE, Direction.EAST);
    private static final CraftingFloor.Site SITE_B = new CraftingFloor.Site(INTERFACE_B, Direction.EAST);

    private MekanismRig() {
    }

    public record Rig(TestWorldBuilder world, TestWorldBuilder.CraftingNetwork net) {
    }

    public static ResourceLocation mek(final String path) {
        return ResourceLocation.fromNamespaceAndPath("mekanism", path);
    }

    public static ResourceLocation generators(final String path) {
        return ResourceLocation.fromNamespaceAndPath("mekanismgenerators", path);
    }

    public static Item item(final ResourceLocation id) {
        return BuiltInRegistries.ITEM.get(id);
    }

    public static StorageKey itemKey(final ResourceLocation id) {
        return StorageKey.of(item(id));
    }

    public static StorageKey water() {
        return StorageKey.of(new FluidStack(Fluids.WATER, 1));
    }

    /** Builds the crafting network and the machine rig in {@code world}; fails loudly if the machine is missing. */
    public static TestWorldBuilder.CraftingNetwork place(final TestWorldBuilder world,
                                                         final ResourceLocation machineId) {
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        CraftingRig.layDyed(world, NETWORK, new BlockPos(5, 2, 3), INTERFACE, new BlockPos(5, 2, 5),
                new BlockPos(5, 2, 6), CABLE_NORTH);
        // The interface's own cable: over the machine to its top, and under it to its bottom.
        CraftingRig.layDyed(world, OWN, new BlockPos(6, 2, 4), new BlockPos(7, 2, 4), new BlockPos(7, 3, 4),
                new BlockPos(7, 3, 5), new BlockPos(7, 3, 6), new BlockPos(7, 3, 7), new BlockPos(7, 1, 4),
                new BlockPos(7, 1, 5), new BlockPos(7, 1, 6), new BlockPos(7, 1, 7));
        CraftingRig.addPart(world, SITE, new CraftingInterfacePart(HardwareEra.STANDARD));
        final Block machine = BuiltInRegistries.BLOCK.get(machineId);
        if (machine == null || machine == Blocks.AIR) {
            throw new IllegalStateException(machineId + " must exist on the dev runtime");
        }
        world.placeFromItem(MACHINE, machine);
        final Direction facing = world.getBlockState(MACHINE)
                .getOptionalValue(BlockStateProperties.HORIZONTAL_FACING).orElse(Direction.NORTH);
        if (facing != Direction.NORTH) {
            throw new IllegalStateException("the rig's face math assumes the factory orientation (north); got "
                    + facing);
        }
        net.cc().forgetFloor();
        return net;
    }

    public static Rig build(final GameTestHelper helper, final ResourceLocation machineId) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        return new Rig(world, place(world, machineId));
    }

    /** A router against the top, a Receiving Bus against the right (west) face. */
    public static void mountBuses(final TestWorldBuilder world) {
        CraftingRig.layDyed(world, OWN, CABLE_ABOVE);
        CraftingRig.addPart(world, new CraftingFloor.Site(CABLE_ABOVE, Direction.DOWN), new CraftingRouterPart());
        CraftingRig.layDyed(world, NETWORK, CABLE_WEST);
        CraftingRig.addPart(world, new CraftingFloor.Site(CABLE_WEST, Direction.EAST), new ReceivingBusPart());
    }

    public static void mountBuses(final GameTestHelper helper) {
        mountBuses(TestWorldBuilder.forGameTest(helper));
    }

    /** A second router against the bottom: the "extra" slot of infusers and compressors lives there. */
    public static void mountBottomInputRouter(final TestWorldBuilder world) {
        CraftingRig.layDyed(world, OWN, CABLE_BELOW);
        CraftingRig.addPart(world, new CraftingFloor.Site(CABLE_BELOW, Direction.UP), new CraftingRouterPart());
    }

    public static void mountBottomInputRouter(final GameTestHelper helper) {
        mountBottomInputRouter(TestWorldBuilder.forGameTest(helper));
    }

    /** A router against the right (west) face: the second input of two-input machines. */
    public static void mountRightInputRouter(final TestWorldBuilder world) {
        CraftingRig.layDyed(world, OWN, CABLE_ABOVE, new BlockPos(5, 3, 7), CABLE_WEST);
        CraftingRig.addPart(world, new CraftingFloor.Site(CABLE_WEST, Direction.EAST), new CraftingRouterPart());
    }

    /** Places a second machine of {@code machineId} south of the first, with an interface and a cable of its own. */
    public static void placeSecondMachine(final TestWorldBuilder world, final ResourceLocation machineId) {
        CraftingRig.layDyed(world, NETWORK, CABLE_WEST, new BlockPos(5, 2, 8), INTERFACE_B, B_RUN);
        CraftingRig.layDyed(world, OWN_B, new BlockPos(6, 2, 9), new BlockPos(7, 2, 9), new BlockPos(7, 3, 9),
                new BlockPos(7, 3, 10), new BlockPos(7, 1, 9), new BlockPos(7, 1, 10));
        CraftingRig.addPart(world, SITE_B, new CraftingInterfacePart(HardwareEra.STANDARD));
        world.placeFromItem(MACHINE_B, BuiltInRegistries.BLOCK.get(machineId));
    }

    /**
     * Mounts the second machine's routers, top (main input) and bottom (the "extra" slot), and its right (west)
     * Receiving Bus. A null filter leaves that router unfiltered; a non-null one restricts it to that item.
     */
    public static void mountSecondMachineBuses(final TestWorldBuilder world, final ItemStack topFilter,
                                               final ItemStack bottomFilter) {
        CraftingRig.layDyed(world, OWN_B, B_ABOVE, B_BELOW);
        final CraftingRouterPart top = CraftingRig.addPart(world,
                new CraftingFloor.Site(B_ABOVE, Direction.DOWN), new CraftingRouterPart());
        final CraftingRouterPart bottom = CraftingRig.addPart(world,
                new CraftingFloor.Site(B_BELOW, Direction.UP), new CraftingRouterPart());
        if (topFilter != null) {
            top.getFilterHandler().setStackInSlot(0, topFilter);
        }
        if (bottomFilter != null) {
            bottom.getFilterHandler().setStackInSlot(0, bottomFilter);
        }
        CraftingRig.addPart(world, new CraftingFloor.Site(B_RUN, Direction.EAST), new ReceivingBusPart());
    }

    /** A second Receiving Bus against the left (east) face, for machines that output on both sides. */
    public static void mountLeftReceivingBus(final TestWorldBuilder world) {
        CraftingRig.layDyed(world, NETWORK, new BlockPos(7, 2, 6), CABLE_EAST);
        CraftingRig.addPart(world, new CraftingFloor.Site(CABLE_EAST, Direction.WEST), new ReceivingBusPart());
    }

    public static void mountLeftReceivingBus(final GameTestHelper helper) {
        mountLeftReceivingBus(TestWorldBuilder.forGameTest(helper));
    }

    /** A router against the left (east) face: the first input of two-input machines. */
    public static void mountLeftInputRouter(final TestWorldBuilder world) {
        CraftingRig.layDyed(world, OWN, CABLE_EAST);
        CraftingRig.addPart(world, new CraftingFloor.Site(CABLE_EAST, Direction.WEST), new CraftingRouterPart());
    }

    /** A Receiving Bus against the front (north) face: where two-input machines give their output. */
    public static void mountFrontReceivingBus(final TestWorldBuilder world) {
        CraftingRig.addPart(world, new CraftingFloor.Site(CABLE_NORTH, Direction.SOUTH), new ReceivingBusPart());
    }

    /**
     * Puts {@code recipe} in every interface of the rig, the second machine's too when it is there, and routes each of
     * its inputs through the router whose face takes it. The routers must be mounted first.
     */
    public static void hold(final TestWorldBuilder world, final NetworkRecipe recipe) {
        for (final CraftingFloor.Site site : sites(world)) {
            holdAt(world, site, recipe);
        }
    }

    /** Puts {@code recipe} in the first machine's interface only, routed. */
    public static void holdFirst(final TestWorldBuilder world, final NetworkRecipe recipe) {
        holdAt(world, SITE, recipe);
    }

    /** Puts {@code recipe} in the second machine's interface only, routed. */
    public static void holdSecond(final TestWorldBuilder world, final NetworkRecipe recipe) {
        holdAt(world, SITE_B, recipe);
    }

    /** The first machine's interface. */
    public static CraftingInterfacePart part(final TestWorldBuilder world) {
        final CraftingInterfacePart part = new CraftingFloor.Site(world.absolute(INTERFACE), Direction.EAST)
                .part(world.level(), CraftingInterfacePart.class);
        if (part == null) {
            throw new IllegalStateException("no rig interface");
        }
        return part;
    }

    /**
     * What a failed machine test needs to be read: the machine's slots, the routes the interface holds, the routers on
     * its cable by face and id, and what the Receiving Bus beside the machine credited.
     */
    public static String describe(final TestWorldBuilder world) {
        final StringBuilder out = new StringBuilder();
        final IItemHandler slots = world.level().getCapability(Capabilities.ItemHandler.BLOCK, world.absolute(MACHINE),
                null);
        out.append("slots=[");
        for (int i = 0; slots != null && i < slots.getSlots(); i++) {
            out.append(i == 0 ? "" : ", ").append(slots.getStackInSlot(i));
        }
        out.append("] routes=").append(part(world).patterns().stream().map(held -> held.routes().toString()).toList());
        final CraftingFloor.Site absolute = new CraftingFloor.Site(world.absolute(INTERFACE), Direction.EAST);
        for (final CraftingFloor.Site router : CraftingFloor.through(world.level(), absolute.cable()).reach(absolute)
                .routers()) {
            final CraftingRouterPart routerPart = router.part(world.level(), CraftingRouterPart.class);
            out.append(" router ").append(router.face()).append('=')
                    .append(routerPart == null ? "none" : routerPart.id());
        }
        final ReceivingBusPart bus = new CraftingFloor.Site(world.absolute(CABLE_WEST), Direction.EAST)
                .part(world.level(), ReceivingBusPart.class);
        out.append(" bus=").append(bus == null ? "none" : bus.log().entries().stream()
                .map(entry -> entry.kind() + ":" + entry.what() + "x" + entry.amount()).toList());
        return out.toString();
    }

    /**
     * Sets a factory up the way a player who cared would: every speed and energy upgrade it takes, and its
     * auto-sort switched on. Without the sorting a factory only ever fills one of its slots, so it works one
     * item at a time and is no faster than the bare machine; without the speed upgrades each of those
     * operations still runs at the base rate. Returns false when the machine is not a factory.
     */
    public static boolean tuneFactory(final ServerLevel level, final BlockPos machine) {
        if (!(level.getBlockEntity(machine) instanceof TileEntityFactory<?> factory)) {
            return false;
        }
        if (!factory.isSorting()) {
            factory.toggleSorting();
        }
        final TileComponentUpgrade upgrades = factory.getComponent();
        for (final Upgrade upgrade : new Upgrade[] {Upgrade.SPEED, Upgrade.ENERGY}) {
            if (upgrades.supports(upgrade)) {
                upgrades.addUpgrades(upgrade, upgrade.getMax() - upgrades.getUpgrades(upgrade));
                factory.recalculateUpgrades(upgrade);
            }
        }
        return true;
    }

    /**
     * Tops the machine's buffer up through the FE capability on its back face (a creative cube's role); returns
     * false when the machine exposes no FE there.
     */
    public static boolean power(final ServerLevel level, final BlockPos machine) {
        final IEnergyStorage fe = level.getCapability(Capabilities.EnergyStorage.BLOCK, machine, Direction.SOUTH);
        if (fe == null) {
            return false;
        }
        fe.receiveEnergy(Integer.MAX_VALUE, false);
        return true;
    }

    public static void power(final GameTestHelper helper) {
        helper.assertTrue(power(helper.getLevel(), helper.absolutePos(MACHINE)),
                "the machine must expose FE on its back face");
    }

    /** Whether the rig's interface feeds the machine of {@code machineId}. */
    public static boolean reaches(final TestWorldBuilder world, final ResourceLocation machineId) {
        final CraftingFloor.Site absolute = new CraftingFloor.Site(world.absolute(INTERFACE), Direction.EAST);
        final BlockPos machine = CraftingFloor.through(world.level(), absolute.cable()).reach(absolute).machine();
        return machine != null && BuiltInRegistries.BLOCK.getKey(world.level().getBlockState(machine).getBlock())
                .equals(machineId);
    }

    public static void assertReaches(final GameTestHelper helper, final ResourceLocation machineId) {
        helper.assertTrue(reaches(TestWorldBuilder.forGameTest(helper), machineId),
                "the interface must feed " + machineId + " through its routers");
    }

    private static void holdAt(final TestWorldBuilder world, final CraftingFloor.Site site,
                               final NetworkRecipe recipe) {
        final CraftingInterfacePart part = new CraftingFloor.Site(world.absolute(site.cable()), site.face())
                .part(world.level(), CraftingInterfacePart.class);
        if (part == null || !part.place(recipe) && !part.holds(recipe)) {
            throw new IllegalStateException("the interface at " + site + " does not take " + recipe);
        }
        CraftingRig.autoRoute(world, site);
        // The parts were set since the computer last read its crafting network: it reads it again at once.
        world.blockEntity(COMPUTER, CraftingComputerBlockEntity.class).forgetFloor();
    }

    private static List<CraftingFloor.Site> sites(final TestWorldBuilder world) {
        final List<CraftingFloor.Site> sites = new ArrayList<>();
        sites.add(SITE);
        if (new CraftingFloor.Site(world.absolute(INTERFACE_B), Direction.EAST)
                .part(world.level(), CraftingInterfacePart.class) != null) {
            sites.add(SITE_B);
        }
        return sites;
    }
}
