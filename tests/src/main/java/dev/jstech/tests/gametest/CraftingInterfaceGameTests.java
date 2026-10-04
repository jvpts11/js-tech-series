/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.part.CraftingInterfacePart;
import dev.jstech.computers.block.part.ReceivingBusPart;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.crafting.CraftingFloor;
import dev.jstech.computers.crafting.CraftingLog;
import dev.jstech.computers.crafting.InterfaceRoutes;
import dev.jstech.computers.crafting.NetworkProcessingOperation;
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.InterfaceView;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.operation.payload.crafting.CraftingViews;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.Text;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestMachineBlockEntity;
import dev.jstech.tests.TestMachines;
import dev.jstech.tests.testkit.CraftingRig;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests for the Crafting Interface model: how an interface feeds its machine (against it, or through the routers of
 * a crafting cable of its own), which interfaces a Crafting Computer's cards drive, how the router an input goes
 * through is chosen, and how a Crafting Receiving Bus credits what comes out: to the job that fed it, never more than
 * it fed for, late outputs to the job they belong to, anything undeclared to the network, and nothing that was in the
 * machine before. Every job runs on a real test machine.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CraftingInterfaceGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    private static final String NO_RECEIVING = "jsc.crafting.view.no_receiving";
    private static final String REACHES_NETWORK = "jsc.crafting.view.reaches_network";
    private static final BlockPos MAINFRAME = new BlockPos(1, 2, 2);
    /* Where the second kiln stands, beside the first rig's. */
    private static final BlockPos SECOND_KILN = new BlockPos(4, 2, 5);

    private CraftingInterfaceGameTests() {
    }

    // How an interface feeds

    /** Against a block with an inventory it feeds that block; against air it feeds nothing; any inventory counts. */
    @GameTest(template = ARENA)
    public static void floor_anInterfaceFeedsTheMachineItSitsAgainst(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingRig rig = CraftingRig.direct(world, net.cc(), TestMachines.KILN.get());
        final CraftingFloor.Site loose = new CraftingFloor.Site(new BlockPos(6, 2, 4), Direction.EAST);
        CraftingRig.addPart(world, loose, new CraftingInterfacePart(HardwareEra.STANDARD));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final CraftingFloor floor = floor(world, rig);
                    final CraftingFloor.Reach reach = floor.reach(rig.absoluteInterface());
                    helper.assertTrue(reach.mode() == CraftingFloor.Mode.DIRECT, "against the kiln it feeds direct");
                    helper.assertTrue(world.absolute(rig.machinePos()).equals(reach.machine()),
                            "its machine is the kiln");
                    helper.assertTrue(floor.reach(abs(world, loose)).mode() == CraftingFloor.Mode.NONE,
                            "against air it feeds nothing");
                    helper.assertTrue(floor.tiedTo(abs(world, rig.busSite())).equals(List.of(rig.absoluteInterface())),
                            "the bus against the kiln ties itself to the kiln's interface");
                    world.setBlock(loose.faced(), Blocks.CHEST);
                    helper.assertTrue(floor(world, rig).reach(abs(world, loose)).mode() == CraftingFloor.Mode.DIRECT,
                            "a chest is a machine too: anything with an inventory can be fed");
                })
                .thenSucceed();
    }

    /**
     * An interface facing a crafting cable feeds through the routers on that cable, kept apart from the network by the
     * part on the face; joined to the network, its window warns.
     */
    @GameTest(template = ARENA)
    public static void floor_aRoutedInterfaceFindsTheRoutersOfItsOwnCable(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingRig rig = CraftingRig.routed(world, net.cc(), TestMachines.MIXER.get(), true);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final CraftingFloor floor = floor(world, rig);
                    final CraftingFloor.Reach reach = floor.reach(rig.absoluteInterface());
                    helper.assertTrue(reach.mode() == CraftingFloor.Mode.CABLE, "it feeds through its own cable");
                    helper.assertTrue(reach.routers().size() == 2, "both routers are on it; got " + reach.routers());
                    helper.assertTrue(world.absolute(rig.machinePos()).equals(reach.machine()),
                            "its machine is the one the routers face");
                    helper.assertTrue(reach.reachesAt() == null && !reach.sharedCable() && !reach.severalMachines(),
                            "its cable is its own and stays off the network");
                    helper.assertTrue(floor.looseRouters().isEmpty(), "no router sits loose on the network");
                    helper.assertTrue(floor.tiedTo(abs(world, rig.busSite())).equals(List.of(rig.absoluteInterface())),
                            "the bus against the mixer ties itself to the routed interface");
                    helper.assertFalse(hasWarning(view(world, rig), REACHES_NETWORK), "no warning while apart");
                    // One block of cable joins its own cable to the network's.
                    CraftingRig.lay(world, new BlockPos(6, 2, 6));
                })
                .thenExecuteAfter(2, () -> {
                    final CraftingFloor.Reach reach = floor(world, rig).reach(rig.absoluteInterface());
                    helper.assertTrue(reach.reachesAt() != null, "joined, its cable runs into the network");
                    helper.assertTrue(hasWarning(view(world, rig), REACHES_NETWORK),
                            "and its window says so; warnings " + view(world, rig).warnings());
                })
                .thenSucceed();
    }

    /** A computer drives as many interfaces as its cards drive, in the order its cable reaches them; the rest wait. */
    @GameTest(template = ARENA)
    public static void floor_cardsDriveAsManyInterfacesAsTheirEraAllows(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingComputerBlockEntity cc = net.cc();
        for (int z = 3; z <= 6; z++) {
            CraftingRig.lay(world, new BlockPos(5, 2, z));
            CraftingRig.addPart(world, new CraftingFloor.Site(new BlockPos(5, 2, z), Direction.EAST),
                    new CraftingInterfacePart(HardwareEra.STANDARD));
            CraftingRig.addPart(world, new CraftingFloor.Site(new BlockPos(5, 2, z), Direction.WEST),
                    new CraftingInterfacePart(HardwareEra.STANDARD));
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    cc.forgetFloor();
                    final CraftingFloor floor = cc.floor();
                    helper.assertTrue(floor.interfaces().size() == 8, "eight interfaces on the cable");
                    helper.assertTrue(cc.interfaceBudget() == 5 && floor.driven().size() == 5,
                            "a Transition card drives five; driven " + floor.driven().size() + " of budget "
                                    + cc.interfaceBudget());
                    helper.assertTrue(floor.drivenBy(floor.interfaces().get(7)) == null,
                            "the last one the cable reaches waits, driven by none");
                    cc.getHardware().setStackInSlot(CraftingComputerBlockEntity.PCIE_SLOTS_START + 2,
                            new ItemStack(ComputingModule.CRAFTING_CARD_T2.get()));
                    cc.forgetFloor();
                    helper.assertTrue(cc.floor().driven().size() == 8, "a second card drives the rest");
                    cc.getHardware().setStackInSlot(CraftingComputerBlockEntity.PCIE_SLOTS_START, ItemStack.EMPTY);
                    cc.getHardware().setStackInSlot(CraftingComputerBlockEntity.PCIE_SLOTS_START + 2,
                            ItemStack.EMPTY);
                    cc.forgetFloor();
                    helper.assertTrue(cc.floor().driven().isEmpty(), "with no card it drives none");
                })
                .thenSucceed();
    }

    // The interface itself

    /** An interface holds as many patterns as its era, each once, and gives back the one taken out. */
    @GameTest(template = ARENA)
    public static void interface_holdsAsManyPatternsAsItsEraEachOnce(final GameTestHelper helper) {
        final CraftingInterfacePart part = new CraftingInterfacePart(HardwareEra.VINTAGE);
        helper.assertTrue(part.capacity() == 3, "a Vintage interface holds three; got " + part.capacity());
        helper.assertTrue(part.place(proc(Items.COBBLESTONE, Items.STONE)), "the first goes in");
        helper.assertFalse(part.place(proc(Items.COBBLESTONE, Items.STONE)), "the same recipe twice is refused");
        helper.assertTrue(part.place(proc(Items.SAND, Items.GLASS)), "a second goes in");
        helper.assertTrue(part.place(proc(Items.RAW_IRON, Items.IRON_INGOT)), "a third goes in");
        helper.assertFalse(part.place(proc(Items.CLAY_BALL, Items.BRICK)), "a fourth is refused: it is full");
        final NetworkRecipe taken = part.take(0);
        helper.assertTrue(taken != null && taken.sameRecipe(proc(Items.COBBLESTONE, Items.STONE)),
                "taking out the first gives it back");
        helper.assertTrue(part.take(99) == null && part.take(-1) == null, "taking out what is not there is null");
        helper.assertTrue(part.place(proc(Items.CLAY_BALL, Items.BRICK)), "the room taken out is free again");
        helper.assertTrue(new CraftingInterfacePart(HardwareEra.ADVANCED).capacity() == 12,
                "an Advanced interface holds twelve");
        helper.succeed();
    }

    /** Most jobs clamps into its range; the mode follows how it feeds until it is set, and programs are marked. */
    @GameTest(template = ARENA)
    public static void interface_settingsClampAndFollowHowItFeeds(final GameTestHelper helper) {
        final CraftingInterfacePart part = new CraftingInterfacePart(HardwareEra.STANDARD);
        helper.assertTrue(part.maxJobs() == 0, "most jobs starts at auto, 0");
        part.setMaxJobs(-9, "");
        helper.assertTrue(part.maxJobs() == 0, "a negative most jobs clamps to auto");
        part.setMaxJobs(4, "");
        helper.assertTrue(part.maxJobs() == 4, "four is kept");
        part.setMaxJobs(1000, "");
        helper.assertTrue(part.maxJobs() == 64, "a huge most jobs clamps to 64");
        helper.assertFalse(part.exclusive(false), "unset, an interface against its machine is not exclusive");
        helper.assertTrue(part.exclusive(true), "unset, an interface feeding through routers is exclusive");
        part.setExclusive(true, "Kiln line");
        helper.assertTrue(part.exclusive(false) && part.exclusiveSet(), "set, the mode is what was set");
        helper.assertTrue(part.setBy(CraftingInterfacePart.MODE).equals("Kiln line"),
                "the program that set it is marked");
        part.setPaused(true, "");
        helper.assertTrue(part.paused() && part.setBy(CraftingInterfacePart.STATE).isEmpty(),
                "a hand leaves no program mark");
        helper.succeed();
    }

    /** Everything an interface keeps survives a save: id, name, patterns and routes, settings, marks and its log. */
    @GameTest(template = ARENA)
    public static void interface_keepsEverythingThroughASave(final GameTestHelper helper) {
        final CraftingInterfacePart part = new CraftingInterfacePart(HardwareEra.TRANSITION);
        final UUID router = UUID.randomUUID();
        part.setName("Kiln A");
        part.place(proc(Items.COBBLESTONE, Items.STONE));
        part.place(proc(Items.SAND, Items.GLASS));
        part.route(1, StorageKey.of(Items.SAND), router, "Router script");
        part.setExclusive(true, "");
        part.setPaused(true, "");
        part.setMaxJobs(3, "");
        part.log().add(new CraftingLog.Entry(10L, StorageKey.of(Items.STONE).id(), 4L, 4L, CraftingLog.COMPLETED,
                ""));
        final CompoundTag saved = new CompoundTag();
        part.save(saved, helper.getLevel().registryAccess());
        final CraftingInterfacePart loaded = new CraftingInterfacePart(HardwareEra.TRANSITION);
        loaded.load(saved, helper.getLevel().registryAccess());
        helper.assertTrue(loaded.id().equals(part.id()), "the id is kept, so buses tied by hand still find it");
        helper.assertTrue(loaded.name().equals("Kiln A"), "the name is kept");
        helper.assertTrue(loaded.patterns().size() == 2, "both patterns are kept");
        helper.assertTrue(router.equals(loaded.patterns().get(1).routes().get(StorageKey.of(Items.SAND).id())),
                "the route chosen for the sand is kept");
        helper.assertTrue(loaded.exclusive(false) && loaded.paused() && loaded.maxJobs() == 3,
                "the mode, the state and most jobs are kept");
        helper.assertTrue(loaded.setBy(CraftingInterfacePart.ROUTES).equals("Router script"),
                "the program mark is kept");
        helper.assertTrue(loaded.log().entries().size() == 1, "the log is kept");
        helper.succeed();
    }

    /** An interface read from a save that knows nothing of it starts empty, with its defaults and an id of its own. */
    @GameTest(template = ARENA)
    public static void interface_readFromNothingTakesItsDefaults(final GameTestHelper helper) {
        final CraftingInterfacePart part = new CraftingInterfacePart(HardwareEra.STANDARD);
        part.load(new CompoundTag(), helper.getLevel().registryAccess());
        helper.assertTrue(part.patterns().isEmpty() && part.maxJobs() == 0 && !part.paused()
                && !part.exclusiveSet() && part.name().isEmpty(), "everything at its default");
        helper.assertTrue(part.id() != null, "it still has an id");
        helper.succeed();
    }

    // Routes

    /** An input goes through the router chosen for it; else one whose filter lists it; else one that takes anything. */
    @GameTest(template = ARENA)
    public static void routes_chosenBeatsFilterBeatsAny(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingRig rig = CraftingRig.routed(world, net.cc(), TestMachines.MIXER.get(), true);
        final ProcessingPattern mix = CraftingRig.mix(Items.DIRT, Items.GRAVEL, Items.COARSE_DIRT, 2);
        final StorageKey dirt = StorageKey.of(Items.DIRT);
        final StorageKey gravel = StorageKey.of(Items.GRAVEL);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    rig.hold(mix);
                    final CraftingFloor.Site west = abs(world, rig.routerSites().get(0));
                    final CraftingFloor.Site north = abs(world, rig.routerSites().get(1));
                    // With no filter both take anything, and the first on the cable carries every input.
                    InterfaceRoutes.Route route = route(world, rig, mix, dirt);
                    helper.assertTrue(route.why() == InterfaceRoutes.Why.ANY && north.equals(route.router()),
                            "with no filters the first router on the cable takes it; got " + route);
                    rig.router(1).getFilterHandler().setStackInSlot(0, new ItemStack(Items.GRAVEL));
                    route = route(world, rig, mix, gravel);
                    helper.assertTrue(route.why() == InterfaceRoutes.Why.FILTER && north.equals(route.router()),
                            "the gravel goes where the filter lists it; got " + route);
                    route = route(world, rig, mix, dirt);
                    helper.assertTrue(route.why() == InterfaceRoutes.Why.ANY && west.equals(route.router()),
                            "the dirt goes to the router that still takes anything; got " + route);
                    rig.part().route(0, dirt, rig.router(1).id(), "");
                    route = route(world, rig, mix, dirt);
                    helper.assertTrue(route.why() == InterfaceRoutes.Why.CHOSEN && north.equals(route.router()),
                            "a router chosen by hand wins over the filters; got " + route);
                    rig.part().route(0, dirt, null, "");
                    helper.assertTrue(route(world, rig, mix, dirt).why() == InterfaceRoutes.Why.ANY,
                            "routed back, the dirt follows the filters again");
                })
                .thenSucceed();
    }

    /** An input no router takes leaves the pattern unable to run there: its job waits and its window says why. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void routes_anInputNoRouterTakesKeepsThePatternFromRunning(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingRig rig = CraftingRig.routed(world, net.cc(), TestMachines.MIXER.get(), true);
        final ProcessingPattern mix = CraftingRig.mix(Items.DIRT, Items.GRAVEL, Items.COARSE_DIRT, 2);
        final AtomicReference<NetworkProcessingOperation> op = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.DIRT, 8);
                    net.seed(Items.GRAVEL, 8);
                    rig.router(0).getFilterHandler().setStackInSlot(0, new ItemStack(Items.DIRT));
                    rig.router(1).getFilterHandler().setStackInSlot(0, new ItemStack(Items.DIRT));
                    rig.hold(mix);
                })
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertFalse(view(world, rig).patterns().get(0).runs(),
                            "the window shows the pattern cannot run here");
                    op.set(net.mainframe().submitNetworkProcessing(mix, 2, "test"));
                })
                .thenExecuteAfter(40, () -> {
                    helper.assertTrue(op.get().isWaiting() && !op.get().isDone(),
                            "the job waits for a way in, past its timeout");
                    helper.assertTrue(net.storage(helper.getLevel()).count(StorageKey.of(Items.DIRT)) == 8,
                            "nothing is fed while an input has no way in");
                    rig.router(1).getFilterHandler().setStackInSlot(0, new ItemStack(Items.GRAVEL));
                })
                .thenExecuteAfter(60, () -> helper.assertTrue(op.get().isDone()
                                && op.get().toRecord().status() == OperationRecord.STATUS_COMPLETED,
                        "given a way in, the job runs and completes"))
                .thenSucceed();
    }

    /** A router moves nothing on its own: only a job feeds through it. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void router_movesNothingOnItsOwn(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingRig rig = CraftingRig.routed(world, net.cc(), TestMachines.KILN.get(), false);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 16);
                    rig.router(0).getFilterHandler().setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
                })
                .thenExecuteAfter(40, () -> {
                    helper.assertTrue(kiln(rig).input(0).isEmpty(), "the kiln was fed nothing");
                    helper.assertTrue(net.storage(helper.getLevel()).count(StorageKey.of(Items.COBBLESTONE)) == 16,
                            "the cobblestone stayed in the network");
                })
                .thenSucceed();
    }

    // Who gets the jobs

    /** Two interfaces holding the same recipe share its jobs, each credited by its own bus. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void jobs_twoInterfacesWithOneRecipeShareItsJobs(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingRig rig = CraftingRig.direct(world, net.cc(), TestMachines.KILN.get());
        final CraftingInterfacePart second = secondKiln(world);
        final ProcessingPattern cobble = CraftingRig.pattern(Items.COBBLESTONE, Items.STONE, 200);
        final AtomicReference<NetworkProcessingOperation> op1 = new AtomicReference<>();
        final AtomicReference<NetworkProcessingOperation> op2 = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 64);
                    rig.hold(cobble);
                    second.place(NetworkRecipe.ofProcessing(cobble));
                    net.cc().forgetFloor();
                })
                .thenExecuteAfter(SETTLE, () -> {
                    op1.set(net.mainframe().submitNetworkProcessing(cobble, 8, "a"));
                    op2.set(net.mainframe().submitNetworkProcessing(cobble, 8, "b"));
                })
                .thenExecuteAfter(4, () -> helper.assertTrue(op1.get().interfaceId() != null
                                && op2.get().interfaceId() != null
                                && !op1.get().interfaceId().equals(op2.get().interfaceId()),
                        "each job went to an interface of its own"))
                .thenExecuteAfter(80, () -> {
                    helper.assertTrue(done(op1.get()) && done(op2.get()), "both jobs complete");
                    helper.assertTrue(op1.get().produced() == 8 && op2.get().produced() == 8,
                            "each was credited exactly what it asked for");
                    helper.assertTrue(stored(helper, Items.STONE) == 16, "sixteen stone in the network");
                    final TestMachineBlockEntity other = world.blockEntity(SECOND_KILN, TestMachineBlockEntity.class);
                    helper.assertTrue(kiln(rig).made() == 8 && other.made() == 8, "each kiln made eight");
                })
                .thenSucceed();
    }

    /** Two recipes for one output on two machines: each job is credited only what its own machine made. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void jobs_twoRecipesForOneOutputAreCreditedApart(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingRig rig = CraftingRig.direct(world, net.cc(), TestMachines.KILN.get());
        final CraftingInterfacePart second = secondKiln(world);
        final ProcessingPattern raw = CraftingRig.pattern(Items.RAW_IRON, Items.IRON_INGOT, 200);
        final ProcessingPattern ore = CraftingRig.pattern(Items.IRON_ORE, Items.IRON_INGOT, 200);
        final AtomicReference<NetworkProcessingOperation> op1 = new AtomicReference<>();
        final AtomicReference<NetworkProcessingOperation> op2 = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.RAW_IRON, 16);
                    net.seed(Items.IRON_ORE, 16);
                    rig.hold(raw);
                    second.place(NetworkRecipe.ofProcessing(ore));
                    net.cc().forgetFloor();
                })
                .thenExecuteAfter(SETTLE, () -> {
                    op1.set(net.mainframe().submitNetworkProcessing(raw, 4, "raw"));
                    op2.set(net.mainframe().submitNetworkProcessing(ore, 6, "ore"));
                })
                .thenExecuteAfter(80, () -> {
                    helper.assertTrue(done(op1.get()) && done(op2.get()), "both jobs complete");
                    helper.assertTrue(op1.get().produced() == 4 && op2.get().produced() == 6,
                            "each was credited its own ingots: " + op1.get().produced() + " and "
                                    + op2.get().produced());
                    helper.assertTrue(stored(helper, Items.IRON_INGOT) == 10, "ten ingots in the network");
                })
                .thenSucceed();
    }

    /** Two interfaces on one machine take turns: one runs while the other's job waits, then the other runs. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void jobs_twoInterfacesOnOneMachineTakeTurns(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingRig rig = CraftingRig.direct(world, net.cc(), TestMachines.KILN.get());
        CraftingRig.lay(world, new BlockPos(4, 2, 3), new BlockPos(4, 2, 4), new BlockPos(4, 2, 5));
        final CraftingInterfacePart second = CraftingRig.addPart(world,
                new CraftingFloor.Site(new BlockPos(4, 2, 5), Direction.EAST),
                new CraftingInterfacePart(HardwareEra.STANDARD));
        final ProcessingPattern cobble = CraftingRig.pattern(Items.COBBLESTONE, Items.STONE, 200);
        final ProcessingPattern sand = CraftingRig.pattern(Items.SAND, Items.GLASS, 200);
        final AtomicReference<NetworkProcessingOperation> op1 = new AtomicReference<>();
        final AtomicReference<NetworkProcessingOperation> op2 = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 16);
                    net.seed(Items.SAND, 16);
                    rig.hold(cobble);
                    second.place(NetworkRecipe.ofProcessing(sand));
                    net.cc().forgetFloor();
                })
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(floor(world, rig).tiedTo(abs(world, rig.busSite())).size() == 2,
                            "the bus credits both interfaces on its machine");
                    op1.set(net.mainframe().submitNetworkProcessing(cobble, 4, "cobble"));
                    op2.set(net.mainframe().submitNetworkProcessing(sand, 4, "sand"));
                })
                .thenExecuteAfter(4, () -> {
                    helper.assertFalse(op1.get().isWaiting(), "the first runs");
                    helper.assertTrue(op2.get().isWaiting(), "the second waits its turn on the machine");
                })
                .thenExecuteAfter(100, () -> {
                    helper.assertTrue(done(op1.get()) && done(op2.get()), "both complete in turn");
                    helper.assertTrue(stored(helper, Items.STONE) == 4 && stored(helper, Items.GLASS) == 4,
                            "four stone and four glass");
                })
                .thenSucceed();
    }

    /** An exclusive interface runs one recipe at a time: jobs of the same recipe together, another one after. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void jobs_anExclusiveInterfaceRunsOneRecipeAtATime(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingRig rig = CraftingRig.direct(world, net.cc(), TestMachines.KILN.get());
        final ProcessingPattern cobble = CraftingRig.pattern(Items.COBBLESTONE, Items.STONE, 200);
        final ProcessingPattern sand = CraftingRig.pattern(Items.SAND, Items.GLASS, 200);
        final AtomicReference<NetworkProcessingOperation> a = new AtomicReference<>();
        final AtomicReference<NetworkProcessingOperation> b = new AtomicReference<>();
        final AtomicReference<NetworkProcessingOperation> c = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 16);
                    net.seed(Items.SAND, 16);
                    rig.hold(cobble);
                    rig.hold(sand);
                    rig.part().setExclusive(true, "");
                })
                .thenExecuteAfter(SETTLE, () -> {
                    a.set(net.mainframe().submitNetworkProcessing(cobble, 3, "a"));
                    b.set(net.mainframe().submitNetworkProcessing(cobble, 3, "b"));
                    c.set(net.mainframe().submitNetworkProcessing(sand, 3, "c"));
                })
                .thenExecuteAfter(4, () -> {
                    helper.assertFalse(a.get().isWaiting() || b.get().isWaiting(), "the two cobble jobs run together");
                    helper.assertTrue(c.get().isWaiting(), "the sand job waits for the recipe to change");
                })
                .thenExecuteAfter(100, () -> {
                    helper.assertTrue(done(a.get()) && done(b.get()) && done(c.get()), "all three complete");
                    helper.assertTrue(stored(helper, Items.STONE) == 6 && stored(helper, Items.GLASS) == 3,
                            "six stone and three glass");
                })
                .thenSucceed();
    }

    /** An interface that is not exclusive takes different recipes at once, as far as its machine takes them. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void jobs_aSharedInterfaceTakesDifferentRecipesAtOnce(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingRig rig = CraftingRig.direct(world, net.cc(), TestMachines.KILN.get());
        final ProcessingPattern cobble = CraftingRig.pattern(Items.COBBLESTONE, Items.STONE, 200);
        final ProcessingPattern sand = CraftingRig.pattern(Items.SAND, Items.GLASS, 200);
        final AtomicReference<NetworkProcessingOperation> a = new AtomicReference<>();
        final AtomicReference<NetworkProcessingOperation> b = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 16);
                    net.seed(Items.SAND, 16);
                    rig.hold(cobble);
                    rig.hold(sand);
                })
                .thenExecuteAfter(SETTLE, () -> {
                    a.set(net.mainframe().submitNetworkProcessing(cobble, 3, "a"));
                    b.set(net.mainframe().submitNetworkProcessing(sand, 3, "b"));
                })
                .thenExecuteAfter(4, () -> helper.assertFalse(a.get().isWaiting() || b.get().isWaiting(),
                        "both recipes are on the interface at once"))
                .thenExecuteAfter(100, () -> {
                    helper.assertTrue(done(a.get()) && done(b.get()), "both complete");
                    helper.assertTrue(stored(helper, Items.STONE) == 3 && stored(helper, Items.GLASS) == 3,
                            "three stone and three glass");
                })
                .thenSucceed();
    }

    // Crediting

    /** A bus tied by hand credits outputs its machine ejects into a chest it faces instead. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void credit_aBusTiedByHandCreditsWhatLandsInAChest(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingRig rig = CraftingRig.direct(world, net.cc(), TestMachines.KILN.get());
        final BlockPos chest = new BlockPos(5, 2, 6);
        world.setBlock(chest, Blocks.CHEST);
        CraftingRig.lay(world, new BlockPos(6, 2, 6));
        final ReceivingBusPart chestBus = CraftingRig.addPart(world,
                new CraftingFloor.Site(new BlockPos(6, 2, 6), Direction.WEST), new ReceivingBusPart());
        final ProcessingPattern cobble = CraftingRig.pattern(Items.COBBLESTONE, Items.STONE, 200);
        final AtomicReference<NetworkProcessingOperation> op = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 16);
                    kiln(rig).setEjectTo(Direction.SOUTH);
                    rig.hold(cobble);
                    chestBus.tieByHand(List.of(rig.part().id()));
                })
                .thenExecuteAfter(SETTLE, () -> op.set(net.mainframe().submitNetworkProcessing(cobble, 4, "chest")))
                .thenExecuteAfter(60, () -> {
                    helper.assertTrue(done(op.get()), "the job completes on what landed in the chest");
                    helper.assertTrue(stored(helper, Items.STONE) == 4, "the four stone reached the network");
                    helper.assertTrue(world.blockEntity(chest, ChestBlockEntity.class).isEmpty(),
                            "the chest is empty");
                })
                .thenSucceed();
    }

    /** Output that leaves the machine where no bus is tied never reaches the job: it settles, nothing lost. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void credit_outputTakenWhereNoBusIsTiedLeavesTheJobShort(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingRig rig = CraftingRig.direct(world, net.cc(), TestMachines.KILN.get());
        final BlockPos chest = new BlockPos(5, 2, 6);
        world.setBlock(chest, Blocks.CHEST);
        final ProcessingPattern cobble = CraftingRig.pattern(Items.COBBLESTONE, Items.STONE, 20);
        final AtomicReference<NetworkProcessingOperation> op = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 16);
                    kiln(rig).setEjectTo(Direction.SOUTH);
                    rig.hold(cobble);
                })
                .thenExecuteAfter(SETTLE, () -> op.set(net.mainframe().submitNetworkProcessing(cobble, 4, "lost")))
                .thenExecuteAfter(80, () -> {
                    helper.assertTrue(op.get().isDone() && op.get().toRecord().status()
                            == OperationRecord.STATUS_FAILED, "the job settles with nothing credited");
                    final ChestBlockEntity box = world.blockEntity(chest, ChestBlockEntity.class);
                    helper.assertTrue(box.countItem(Items.STONE) == 4, "the four stone are in the chest");
                    helper.assertTrue(stored(helper, Items.COBBLESTONE) == 12, "four cobblestone were fed");
                })
                .thenSucceed();
    }

    /** What comes out after a job settled goes where that job's outputs go, never to the next job. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void credit_lateOutputsGoToTheJobThatFedThem(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingRig rig = CraftingRig.direct(world, net.cc(), TestMachines.KILN.get());
        final ProcessingPattern cobble = CraftingRig.pattern(Items.COBBLESTONE, Items.STONE, 40);
        final AtomicReference<NetworkProcessingOperation> first = new AtomicReference<>();
        final AtomicReference<NetworkProcessingOperation> next = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 16);
                    kiln(rig).setHeld(true);
                    rig.hold(cobble);
                })
                .thenExecuteAfter(SETTLE, () -> first.set(net.mainframe().submitNetworkProcessing(cobble, 4,
                        "first")))
                .thenExecuteAfter(60, () -> {
                    helper.assertTrue(first.get().isDone() && first.get().toRecord().status()
                            == OperationRecord.STATUS_FAILED, "the first job timed out with its four lots fed");
                    helper.assertTrue(rig.part().owed().size() == 1 && rig.part().owed().get(0).remaining() == 4,
                            "the interface keeps what it is still owed");
                    kiln(rig).setHeld(false);
                    next.set(net.mainframe().submitNetworkProcessing(cobble, 2, "next"));
                })
                .thenExecuteAfter(80, () -> {
                    helper.assertTrue(done(next.get()), "the next job completes");
                    helper.assertTrue(next.get().produced() == 2,
                            "it was credited only its own two, not the first job's four; got "
                                    + next.get().produced());
                    helper.assertTrue(stored(helper, Items.STONE) == 6, "all six stone reached the network");
                    helper.assertTrue(rig.part().owed().isEmpty(), "the first job was paid off");
                    helper.assertTrue(hasEntry(rig.bus().log(), CraftingLog.LATE, Items.STONE),
                            "the bus logged the late arrivals");
                })
                .thenSucceed();
    }

    /** An output no pattern declares goes to the network as unexpected, and both logs say so. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void credit_anUndeclaredByProductIsUnexpected(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingRig rig = CraftingRig.direct(world, net.cc(), TestMachines.KILN.get());
        final ProcessingPattern gravel = CraftingRig.pattern(Items.GRAVEL, Items.FLINT, 200);
        final AtomicReference<NetworkProcessingOperation> op = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.GRAVEL, 16);
                    rig.hold(gravel);
                })
                .thenExecuteAfter(SETTLE, () -> op.set(net.mainframe().submitNetworkProcessing(gravel, 4, "flint")))
                .thenExecuteAfter(60, () -> {
                    helper.assertTrue(done(op.get()) && op.get().produced() == 4, "the job made its four flint");
                    helper.assertTrue(stored(helper, Items.SAND) == 4,
                            "the sand the kiln gave too went to the network");
                    helper.assertTrue(hasEntry(rig.bus().log(), CraftingLog.UNEXPECTED, Items.SAND),
                            "the bus logged the sand as unexpected");
                    helper.assertTrue(hasEntry(rig.part().log(), CraftingLog.UNEXPECTED, Items.SAND),
                            "and so did the interface");
                })
                .thenSucceed();
    }

    /** A chance output a pattern declares is the job's, as much as it fed for, and never unexpected. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void credit_aDeclaredChanceOutputIsTheJobs(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingRig rig = CraftingRig.direct(world, net.cc(), TestMachines.KILN.get());
        final ProcessingPattern gravel = new ProcessingPattern(
                List.of(new ProcessingPattern.ProcessingInput(StorageKey.of(Items.GRAVEL), 1L)),
                List.of(new ProcessingPattern.ProcessingOutput(StorageKey.of(Items.FLINT), 1L,
                                ProcessingPattern.FULL_CHANCE),
                        new ProcessingPattern.ProcessingOutput(StorageKey.of(Items.SAND), 1L, 50)),
                200);
        final AtomicReference<NetworkProcessingOperation> op = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.GRAVEL, 16);
                    rig.hold(gravel);
                })
                .thenExecuteAfter(SETTLE, () -> op.set(net.mainframe().submitNetworkProcessing(gravel, 4, "flint")))
                .thenExecuteAfter(60, () -> {
                    helper.assertTrue(done(op.get()), "the job completes");
                    helper.assertTrue(op.get().credited(1) == 4, "the sand was credited to the job; got "
                            + op.get().credited(1));
                    helper.assertFalse(hasEntry(rig.bus().log(), CraftingLog.UNEXPECTED, Items.SAND),
                            "and never counted unexpected");
                })
                .thenSucceed();
    }

    /** Two buses on one machine credit one job between them, never twice. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void credit_twoBusesOnOneMachineCreditOnce(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingRig rig = CraftingRig.direct(world, net.cc(), TestMachines.KILN.get());
        CraftingRig.lay(world, new BlockPos(6, 2, 6), new BlockPos(5, 2, 6));
        CraftingRig.addPart(world, new CraftingFloor.Site(new BlockPos(5, 2, 6), Direction.NORTH),
                new ReceivingBusPart());
        final ProcessingPattern cobble = CraftingRig.pattern(Items.COBBLESTONE, Items.STONE, 200);
        final AtomicReference<NetworkProcessingOperation> op = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 16);
                    rig.hold(cobble);
                })
                .thenExecuteAfter(SETTLE, () -> op.set(net.mainframe().submitNetworkProcessing(cobble, 8, "two")))
                .thenExecuteAfter(80, () -> {
                    helper.assertTrue(done(op.get()) && op.get().produced() == 8, "the job got its eight");
                    helper.assertTrue(stored(helper, Items.STONE) == 8, "exactly eight stone in the network");
                    helper.assertTrue(kiln(rig).output().isEmpty(), "nothing left in the kiln");
                })
                .thenSucceed();
    }

    /** A job is never credited more than it fed for: more put into the machine by hand is unexpected. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void credit_neverMoreThanTheLotsFed(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingRig rig = CraftingRig.direct(world, net.cc(), TestMachines.KILN.get());
        final ProcessingPattern cobble = CraftingRig.pattern(Items.COBBLESTONE, Items.STONE, 200);
        final AtomicReference<NetworkProcessingOperation> op = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 16);
                    kiln(rig).setHeld(true);
                    rig.hold(cobble);
                })
                .thenExecuteAfter(SETTLE, () -> op.set(net.mainframe().submitNetworkProcessing(cobble, 2, "cap")))
                .thenExecuteAfter(10, () -> {
                    // A player drops ten stone into the kiln while the job runs.
                    kiln(rig).preload(new ItemStack(Items.STONE, 10));
                    kiln(rig).setHeld(false);
                })
                .thenExecuteAfter(60, () -> {
                    helper.assertTrue(done(op.get()) && op.get().produced() == 2,
                            "the job was credited its two and no more; got " + op.get().produced());
                    helper.assertTrue(stored(helper, Items.STONE) + kiln(rig).output().getCount() == 12,
                            "every stone is accounted for");
                    helper.assertTrue(hasEntry(rig.bus().log(), CraftingLog.UNEXPECTED, Items.STONE),
                            "what it was not owed went to the network as unexpected");
                })
                .thenSucceed();
    }

    /** An output that is also an input, a catalyst, comes back to the network as the job's. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void credit_aCatalystComesBack(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingRig rig = CraftingRig.routed(world, net.cc(), TestMachines.MIXER.get(), true);
        final ProcessingPattern grow = new ProcessingPattern(
                List.of(new ProcessingPattern.ProcessingInput(StorageKey.of(Items.DIRT), 1L),
                        new ProcessingPattern.ProcessingInput(StorageKey.of(Items.BONE_MEAL), 1L)),
                List.of(new ProcessingPattern.ProcessingOutput(StorageKey.of(Items.GRASS_BLOCK), 1L,
                                ProcessingPattern.FULL_CHANCE),
                        new ProcessingPattern.ProcessingOutput(StorageKey.of(Items.BONE_MEAL), 1L,
                                ProcessingPattern.FULL_CHANCE)),
                200);
        final AtomicReference<NetworkProcessingOperation> op = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.DIRT, 16);
                    net.seed(Items.BONE_MEAL, 16);
                    rig.router(0).getFilterHandler().setStackInSlot(0, new ItemStack(Items.DIRT));
                    rig.router(1).getFilterHandler().setStackInSlot(0, new ItemStack(Items.BONE_MEAL));
                    rig.hold(grow);
                })
                .thenExecuteAfter(SETTLE, () -> op.set(net.mainframe().submitNetworkProcessing(grow, 4, "grow")))
                .thenExecuteAfter(100, () -> {
                    helper.assertTrue(done(op.get()), "the job completes");
                    helper.assertTrue(stored(helper, Items.GRASS_BLOCK) == 4, "four grass blocks made");
                    helper.assertTrue(stored(helper, Items.BONE_MEAL) == 16, "every bone meal came back");
                    helper.assertTrue(stored(helper, Items.DIRT) == 12, "four dirt used");
                })
                .thenSucceed();
    }

    /** An interface no bus credits warns in its window, and its job settles on its timeout. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void credit_anInterfaceWithNoBusWarnsAndItsJobTimesOut(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        CraftingRig.lay(world, new BlockPos(5, 2, 3), new BlockPos(5, 2, 4));
        world.setBlock(CraftingRig.DIRECT_MACHINE, TestMachines.KILN.get());
        final CraftingFloor.Site site = new CraftingFloor.Site(new BlockPos(5, 2, 4), Direction.SOUTH);
        final CraftingInterfacePart part = CraftingRig.addPart(world, site,
                new CraftingInterfacePart(HardwareEra.STANDARD));
        final ProcessingPattern cobble = CraftingRig.pattern(Items.COBBLESTONE, Items.STONE, 20);
        final AtomicReference<NetworkProcessingOperation> op = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 16);
                    part.place(NetworkRecipe.ofProcessing(cobble));
                    net.cc().forgetFloor();
                    final InterfaceView view = CraftingViews.of(helper.getLevel(), world.absolute(site.cable()),
                            site.face(), part);
                    helper.assertTrue(hasWarning(view, NO_RECEIVING), "the window warns no bus credits it; got "
                            + view.warnings());
                    op.set(net.mainframe().submitNetworkProcessing(cobble, 4, "nobus"));
                })
                .thenExecuteAfter(80, () -> {
                    helper.assertTrue(op.get().isDone() && op.get().toRecord().status()
                            == OperationRecord.STATUS_FAILED, "the job settles with nothing credited");
                    final TestMachineBlockEntity kiln = world.blockEntity(CraftingRig.DIRECT_MACHINE,
                            TestMachineBlockEntity.class);
                    helper.assertTrue(kiln.output().getCount() == 4, "the four stone wait in the kiln");
                })
                .thenSucceed();
    }

    // Fixtures

    /*
     * A second kiln at (4,2,5), with its interface on (4,2,4) facing south and its bus on (3,2,5) facing east, on cable
     * that joins the first rig's at (5,2,3).
     */
    private static CraftingInterfacePart secondKiln(final TestWorldBuilder world) {
        CraftingRig.lay(world, new BlockPos(4, 2, 3), new BlockPos(4, 2, 4), new BlockPos(3, 2, 4),
                new BlockPos(3, 2, 5));
        world.setBlock(SECOND_KILN, TestMachines.KILN.get());
        CraftingRig.addPart(world, new CraftingFloor.Site(new BlockPos(3, 2, 5), Direction.EAST),
                new ReceivingBusPart());
        return CraftingRig.addPart(world, new CraftingFloor.Site(new BlockPos(4, 2, 4), Direction.SOUTH),
                new CraftingInterfacePart(HardwareEra.STANDARD));
    }

    private static CraftingFloor floor(final TestWorldBuilder world, final CraftingRig rig) {
        return CraftingFloor.through(world.level(), world.absolute(rig.interfaceSite().cable()));
    }

    private static CraftingFloor.Site abs(final TestWorldBuilder world, final CraftingFloor.Site site) {
        return new CraftingFloor.Site(world.absolute(site.cable()), site.face());
    }

    private static InterfaceView view(final TestWorldBuilder world, final CraftingRig rig) {
        return CraftingViews.of(world.level(), world.absolute(rig.interfaceSite().cable()),
                rig.interfaceSite().face(), rig.part());
    }

    private static InterfaceRoutes.Route route(final TestWorldBuilder world, final CraftingRig rig,
                                               final ProcessingPattern pattern, final StorageKey input) {
        final CraftingFloor.Reach reach = floor(world, rig).reach(rig.absoluteInterface());
        final CraftingInterfacePart.HeldPattern held = rig.part().holding(pattern);
        if (held == null) {
            throw new IllegalStateException("the interface does not hold " + pattern);
        }
        return InterfaceRoutes.routeFor(world.level(), held, input, reach);
    }

    private static boolean hasWarning(final InterfaceView view, final String key) {
        for (final Text warning : view.warnings()) {
            if (warning instanceof Text.Translated translated && translated.key().key().equals(key)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasEntry(final CraftingLog log, final byte kind, final Item item) {
        final String id = StorageKey.of(item).id();
        for (final CraftingLog.Entry entry : log.entries()) {
            if (entry.kind() == kind && entry.what().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static boolean done(final NetworkProcessingOperation op) {
        return op.isDone() && op.toRecord().status() == OperationRecord.STATUS_COMPLETED;
    }

    private static long stored(final GameTestHelper helper, final Item item) {
        final MainframeBlockEntity mainframe = TestWorldBuilder.forGameTest(helper).blockEntity(MAINFRAME,
                MainframeBlockEntity.class);
        return NetworkStorage.of(helper.getLevel(), mainframe.networkUuid()).count(StorageKey.of(item));
    }

    private static NetworkRecipe proc(final Item input, final Item output) {
        return NetworkRecipe.ofProcessing(CraftingRig.pattern(input, output, 200));
    }

    private static TestMachineBlockEntity kiln(final CraftingRig rig) {
        final TestMachineBlockEntity kiln = rig.machine();
        if (kiln == null) {
            throw new IllegalStateException("no test machine at " + rig.machinePos());
        }
        return kiln;
    }
}
