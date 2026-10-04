/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.part.CraftingInterfacePart;
import dev.jstech.computers.block.part.CraftingRouterPart;
import dev.jstech.computers.block.part.ReceivingBusPart;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.crafting.CraftingFloor;
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.storage.ExternalDataPort;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.Cables;
import dev.jstech.core.multipart.IFacePart;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.TestMachineBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A crafting floor for the autocraft tests, built beside the Crafting Computer of the standard crafting network at
 * (5,2,2): a crafting cable from the computer, a Crafting Interface that feeds one machine, and a Crafting Receiving
 * Bus against that machine's output. The interface feeds either the machine it sits against, or the routers of a
 * crafting cable of its own; in both the bus ties itself to it by the machine it faces.
 *
 * <p>Direct: cable (5,2,3) (5,2,4) (6,2,4) (6,2,5); the machine at (5,2,5); the interface on (5,2,4) facing south;
 * the bus on (6,2,5) facing west.
 *
 * <p>Routed: the interface on (5,2,4) facing south onto its own cable (5,2,5) (5,2,6) (5,2,7) (4,2,7) (4,2,8), kept
 * off the network by the part on that face; the machine at (5,2,8); a router on (4,2,8) against its west face and,
 * with two routers, one on (5,2,7) against its north face; the network cable runs on (7,2,4) to (7,2,8) and (6,2,8),
 * where the bus faces the machine's east face.
 */
public final class CraftingRig {

    private final TestWorldBuilder world;
    private final CraftingComputerBlockEntity computer;
    private final BlockPos machine;
    private final CraftingFloor.Site interfaceSite;
    private final CraftingFloor.Site busSite;
    private final List<CraftingFloor.Site> routerSites;

    /** Where the machine of a direct rig stands. */
    public static final BlockPos DIRECT_MACHINE = new BlockPos(5, 2, 5);
    /** Where the machine of a routed rig stands. */
    public static final BlockPos ROUTED_MACHINE = new BlockPos(5, 2, 8);

    private CraftingRig(final TestWorldBuilder world, final CraftingComputerBlockEntity computer,
                        final BlockPos machine, final CraftingFloor.Site interfaceSite,
                        final CraftingFloor.Site busSite, final List<CraftingFloor.Site> routerSites) {
        this.world = world;
        this.computer = computer;
        this.machine = machine;
        this.interfaceSite = interfaceSite;
        this.busSite = busSite;
        this.routerSites = routerSites;
    }

    /** An interface against {@code machineBlock}, a Standard interface, and the bus beside it. */
    public static CraftingRig direct(final TestWorldBuilder world, final CraftingComputerBlockEntity computer,
                                     final Block machineBlock) {
        return direct(world, computer, machineBlock, HardwareEra.STANDARD);
    }

    /** An interface of {@code era} against {@code machineBlock}, and the bus beside it. */
    public static CraftingRig direct(final TestWorldBuilder world, final CraftingComputerBlockEntity computer,
                                     final Block machineBlock, final HardwareEra era) {
        lay(world, new BlockPos(5, 2, 3), new BlockPos(5, 2, 4), new BlockPos(6, 2, 4), new BlockPos(6, 2, 5));
        world.setBlock(DIRECT_MACHINE, machineBlock);
        final CraftingFloor.Site face = new CraftingFloor.Site(new BlockPos(5, 2, 4), Direction.SOUTH);
        final CraftingFloor.Site bus = new CraftingFloor.Site(new BlockPos(6, 2, 5), Direction.WEST);
        addPart(world, face, new CraftingInterfacePart(era));
        addPart(world, bus, new ReceivingBusPart());
        computer.forgetFloor();
        return new CraftingRig(world, computer, DIRECT_MACHINE, face, bus, List.of());
    }

    /**
     * An interface feeding {@code machineBlock} through the routers of its own cable: one against the machine's west
     * face, and with {@code twoRouters} a second against its north face.
     */
    public static CraftingRig routed(final TestWorldBuilder world, final CraftingComputerBlockEntity computer,
                                     final Block machineBlock, final boolean twoRouters) {
        lay(world, new BlockPos(5, 2, 3), new BlockPos(5, 2, 4), new BlockPos(6, 2, 4), new BlockPos(7, 2, 4),
                new BlockPos(7, 2, 5), new BlockPos(7, 2, 6), new BlockPos(7, 2, 7), new BlockPos(7, 2, 8),
                new BlockPos(6, 2, 8));
        lay(world, new BlockPos(5, 2, 5), new BlockPos(5, 2, 6), new BlockPos(5, 2, 7), new BlockPos(4, 2, 7),
                new BlockPos(4, 2, 8));
        world.setBlock(ROUTED_MACHINE, machineBlock);
        final CraftingFloor.Site face = new CraftingFloor.Site(new BlockPos(5, 2, 4), Direction.SOUTH);
        final CraftingFloor.Site bus = new CraftingFloor.Site(new BlockPos(6, 2, 8), Direction.WEST);
        final List<CraftingFloor.Site> routers = new ArrayList<>();
        routers.add(new CraftingFloor.Site(new BlockPos(4, 2, 8), Direction.EAST));
        if (twoRouters) {
            routers.add(new CraftingFloor.Site(new BlockPos(5, 2, 7), Direction.SOUTH));
        }
        addPart(world, face, new CraftingInterfacePart(HardwareEra.STANDARD));
        addPart(world, bus, new ReceivingBusPart());
        for (final CraftingFloor.Site router : routers) {
            addPart(world, router, new CraftingRouterPart());
        }
        computer.forgetFloor();
        return new CraftingRig(world, computer, ROUTED_MACHINE, face, bus, List.copyOf(routers));
    }

    /** Lays the crafting cable at every one of {@code relative}. */
    public static void lay(final TestWorldBuilder world, final BlockPos... relative) {
        for (final BlockPos pos : relative) {
            world.setBlock(pos, ComputingModule.CRAFTING_CABLE);
        }
    }

    /**
     * Lays the crafting cable in {@code colour} at every one of {@code relative} that holds none yet. Two runs of
     * different colours never join, however close they run, which keeps an interface's own cable off the network.
     */
    public static void layDyed(final TestWorldBuilder world, final DyeColor colour, final BlockPos... relative) {
        for (final BlockPos pos : relative) {
            if (!Cables.holds(world.level(), world.absolute(pos), ComputingModule.CRAFTING_CABLE.get())) {
                world.setBlock(pos, ComputingModule.CRAFTING_CABLE);
            }
            if (world.getBlockEntity(pos) instanceof CableBlockEntity cable) {
                cable.dye(ComputingModule.CRAFTING_CABLE.get(), colour);
            }
        }
    }

    /** Puts {@code count} more Transition Crafting Cards in the computer's free expansion slots. */
    public static void addCards(final CraftingComputerBlockEntity computer, final int count) {
        int left = count;
        for (int i = 0; i < CraftingComputerBlockEntity.PCIE_SLOTS && left > 0; i++) {
            final int slot = CraftingComputerBlockEntity.PCIE_SLOTS_START + i;
            if (computer.getHardware().getStackInSlot(slot).isEmpty()) {
                computer.getHardware().setStackInSlot(slot, new ItemStack(ComputingModule.CRAFTING_CARD_T2.get()));
                left--;
            }
        }
        computer.forgetFloor();
    }

    /**
     * Routes every input of every pattern the interface at {@code site} holds through a router on its own cable whose
     * filter allows it and whose machine face takes it, the way a player sets a machine with sided inputs up: a router
     * no other input of the pattern goes through first, so two inputs that either face would take go one to each.
     */
    public static void autoRoute(final TestWorldBuilder world, final CraftingFloor.Site site) {
        final CraftingFloor.Site absolute = new CraftingFloor.Site(world.absolute(site.cable()), site.face());
        final CraftingFloor.Reach reach = CraftingFloor.through(world.level(), absolute.cable()).reach(absolute);
        final CraftingInterfacePart part = absolute.part(world.level(), CraftingInterfacePart.class);
        if (part == null) {
            throw new IllegalStateException("no interface at " + site);
        }
        final List<CraftingInterfacePart.HeldPattern> held = part.patterns();
        for (int index = 0; index < held.size(); index++) {
            for (final ProcessingPattern step : stepsOf(held.get(index).recipe())) {
                final List<CraftingRouterPart> used = new ArrayList<>();
                for (final ProcessingPattern.ProcessingInput input : step.inputs()) {
                    CraftingRouterPart chosen = null;
                    for (final CraftingFloor.Site router : reach.routers()) {
                        final CraftingRouterPart routerPart = router.part(world.level(), CraftingRouterPart.class);
                        if (routerPart != null && takes(world, router, routerPart, input)
                                && (chosen == null || used.contains(chosen) && !used.contains(routerPart))) {
                            chosen = routerPart;
                        }
                    }
                    if (chosen != null) {
                        used.add(chosen);
                        part.route(index, input.key(), chosen.id(), "");
                    }
                }
            }
        }
    }

    /** Puts {@code part} on the cable at {@code site}; the cable must already be there. */
    public static <P extends IFacePart> P addPart(final TestWorldBuilder world, final CraftingFloor.Site site,
                                                  final P part) {
        if (!(world.getBlockEntity(site.cable()) instanceof CableBlockEntity cable)) {
            throw new IllegalStateException("no cable at " + site.cable().toShortString());
        }
        cable.addPart(site.face(), part);
        return part;
    }

    /** A one-input, one-output recipe of {@code amount} per lot each way, timing out after {@code timeout} ticks. */
    public static ProcessingPattern pattern(final Item input, final Item output, final int timeout) {
        return new ProcessingPattern(
                List.of(new ProcessingPattern.ProcessingInput(StorageKey.of(input), 1L)),
                List.of(new ProcessingPattern.ProcessingOutput(StorageKey.of(output), 1L,
                        ProcessingPattern.FULL_CHANCE)),
                timeout);
    }

    /** A two-input recipe, one of each, for the mixer: {@code first} in the west face, {@code second} the north. */
    public static ProcessingPattern mix(final Item first, final Item second, final Item output, final long made) {
        return new ProcessingPattern(
                List.of(new ProcessingPattern.ProcessingInput(StorageKey.of(first), 1L),
                        new ProcessingPattern.ProcessingInput(StorageKey.of(second), 1L)),
                List.of(new ProcessingPattern.ProcessingOutput(StorageKey.of(output), made,
                        ProcessingPattern.FULL_CHANCE)),
                ProcessingPattern.DEFAULT_TIMEOUT_TICKS);
    }

    /** The Crafting Computer the rig hangs off. */
    public CraftingComputerBlockEntity computer() {
        return computer;
    }

    /** Where the machine stands, relative. */
    public BlockPos machinePos() {
        return machine;
    }

    /** The interface's site, relative. */
    public CraftingFloor.Site interfaceSite() {
        return interfaceSite;
    }

    /** The bus's site, relative. */
    public CraftingFloor.Site busSite() {
        return busSite;
    }

    /** The routers' sites, relative, the west one first. */
    public List<CraftingFloor.Site> routerSites() {
        return routerSites;
    }

    /** The interface. */
    public CraftingInterfacePart part() {
        return partAt(interfaceSite, CraftingInterfacePart.class);
    }

    /** The bus. */
    public ReceivingBusPart bus() {
        return partAt(busSite, ReceivingBusPart.class);
    }

    /** Router {@code index}: 0 the west one, 1 the north one. */
    public CraftingRouterPart router(final int index) {
        return partAt(routerSites.get(index), CraftingRouterPart.class);
    }

    /** The test machine, or null when the rig feeds some other block. */
    @Nullable
    public TestMachineBlockEntity machine() {
        return world.getBlockEntity(machine) instanceof TestMachineBlockEntity test ? test : null;
    }

    /** Puts {@code pattern} in the interface; false when it is full or holds it already. */
    public boolean hold(final ProcessingPattern pattern) {
        return hold(NetworkRecipe.ofProcessing(pattern));
    }

    /** Puts {@code recipe} in the interface; false when it is full or holds it already. */
    public boolean hold(final NetworkRecipe recipe) {
        final boolean placed = part().place(recipe);
        computer.forgetFloor();
        return placed;
    }

    /** The interface's site in world coordinates, as the dispatcher sees it. */
    public CraftingFloor.Site absoluteInterface() {
        return new CraftingFloor.Site(world.absolute(interfaceSite.cable()), interfaceSite.face());
    }

    /** How much of {@code item} the network and the machine hold together. */
    public long total(final NetworkStorage storage, final Item item) {
        return storage.count(StorageKey.of(item)) + inMachine(item);
    }

    /** How much of {@code item} the machine holds, in any slot. */
    public long inMachine(final Item item) {
        final BlockEntity entity = world.getBlockEntity(machine);
        if (entity == null || entity.getLevel() == null) {
            return 0L;
        }
        final IItemHandler handler = entity.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                entity.getBlockPos(), null);
        if (handler == null) {
            return 0L;
        }
        long count = 0L;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            final ItemStack stack = handler.getStackInSlot(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /* The machine steps of a recipe: the processing recipe itself, or each processing stage of a pipeline. */
    private static List<ProcessingPattern> stepsOf(final NetworkRecipe recipe) {
        final List<ProcessingPattern> steps = new ArrayList<>();
        recipe.proc().ifPresent(steps::add);
        recipe.multi().ifPresent(multi -> multi.stages().forEach(stage -> stage.proc().ifPresent(steps::add)));
        return steps;
    }

    /* Whether the router's filter allows the input and the machine face it is against takes some of it now. */
    private static boolean takes(final TestWorldBuilder world, final CraftingFloor.Site router,
                                 final CraftingRouterPart part, final ProcessingPattern.ProcessingInput input) {
        final List<StorageKey> filter = part.filterKeys();
        if (!filter.isEmpty() && !filter.contains(input.key())) {
            return false;
        }
        return ExternalDataPort.at(world.level(), router.faced(), router.face().getOpposite())
                .insert(input.key(), input.amount(), true) > 0L;
    }

    private <P extends IFacePart> P partAt(final CraftingFloor.Site site, final Class<P> kind) {
        if (world.getBlockEntity(site.cable()) instanceof CableBlockEntity cable && kind.isInstance(
                cable.getPart(site.face()))) {
            return kind.cast(cable.getPart(site.face()));
        }
        throw new IllegalStateException("no " + kind.getSimpleName() + " at " + site);
    }
}
