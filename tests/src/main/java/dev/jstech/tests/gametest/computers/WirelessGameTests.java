/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.AccessPointBlock;
import dev.jstech.computers.blockentity.AccessPointBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.ExpansionCardKind;
import dev.jstech.computers.operation.payload.NetworkNodeInfo;
import dev.jstech.computers.operation.payload.NodeLink;
import dev.jstech.computers.operation.payload.network.NetworkPayloads;
import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.wireless.AccessPointMount;
import dev.jstech.computers.wireless.WirelessLink;
import dev.jstech.computers.wireless.WirelessStandard;
import dev.jstech.core.jstechseries.energy.SharedEnergy;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestComputerEnergy;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wireless (computers/docs/design/wireless.md): an access point on a data cable gives its network to the computers with
 * a Wi-Fi card it reaches, at the speed of the older of the two; a computer on a cable keeps its cable; one with no
 * card, or none in reach, stays off; one that is fixed comes off with what it is fixed to; and the Network Manager
 * shows the access point as a node and the computer's link as Wi-Fi.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class WirelessGameTests {

    private static final String ARENA = "empty";
    /* The batch whose draw is on, which ComputerEnergyGameTests turns on and off round it. */
    private static final String ENERGY = TestComputerEnergy.BATCH;
    /* The chain every machine of the network meets the Mainframe by: its backbone, a router, the access line. */
    private static final BlockPos MAINFRAME = new BlockPos(1, 2, 2);
    private static final BlockPos ACCESS_POINT = new BlockPos(5, 2, 2);
    private static final BlockPos PC = new BlockPos(5, 2, 8);
    /* The slot after the graphics card's, where a test machine's second card goes. */
    private static final int CARD_SLOT = PersonalComputerBlockEntity.GPU_SLOTS_START + 1;
    private static final int SETTLE = 60;

    private WirelessGameTests() {
    }

    /**
     * A computer with a Wi-Fi card and no cable joins the network of the access point that reaches it, at the speed of
     * the older end: a Standard card through a Legacy access point goes at the Legacy's 4 items a tick.
     */
    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void accessPoint_givesItsNetworkToAComputerWithACardInReach(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper);
        final AccessPointBlockEntity point = deskAccessPoint(helper, ComputingModule.LEGACY_ACCESS_POINT.get());
        final PersonalComputerBlockEntity pc = withCard(helper, PC, ComputingModule.WIFI_CARD_STANDARD.get());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(mainframe.networkUuid() != null
                        && mainframe.networkUuid().equals(point.serving()), "the access point gives the network"))
                .thenWaitUntil(() -> helper.assertTrue(mainframe.networkUuid().equals(pc.networkUuid()),
                        "the computer joins it by Wi-Fi"))
                .thenExecute(() -> {
                    final WirelessLink link = pc.wirelessLink(helper.getLevel());
                    helper.assertTrue(link != null && link.card() == WirelessStandard.STANDARD
                                    && link.accessPoint() == WirelessStandard.LEGACY
                                    && link.speed() == WirelessStandard.LEGACY,
                            "at the older end's speed; got " + link);
                    helper.assertValueEqual(pc.ownLinkItemsPerTick(helper.getLevel()), 4, "items a tick");
                    helper.assertTrue(!pc.networkCables(helper.getLevel()).isEmpty(),
                            "it is as far along the network as its access point's cable");
                })
                .thenSucceed();
    }

    /** The Network Manager lists the access point as a node on its cable, and the computer's link as Wi-Fi. */
    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void networkManager_showsTheAccessPointAndTheWirelessLink(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper);
        deskAccessPoint(helper, ComputingModule.TRANSITION_ACCESS_POINT.get());
        final PersonalComputerBlockEntity pc = withCard(helper, PC, ComputingModule.WIFI_CARD_STANDARD.get());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(mainframe.networkUuid() != null
                        && mainframe.networkUuid().equals(pc.networkUuid()), "the computer joins by Wi-Fi"))
                .thenExecute(() -> {
                    final List<NetworkNodeInfo> nodes = NetworkPayloads.collectNetworkManager(helper.getLevel(),
                            mainframe).nodes();
                    final NetworkNodeInfo point = nodes.stream()
                            .filter(node -> node.kind() == NetworkNodeInfo.KIND_ACCESS_POINT).findFirst().orElse(null);
                    helper.assertTrue(point != null && point.online() && point.name().startsWith("AP-")
                                    && WirelessStandard.byName(point.detail()) == WirelessStandard.TRANSITION
                                    && point.link().dataLink() != null,
                            "the access point is a node on its cable; got " + point);
                    final NodeLink.Wifi wifi = nodes.stream().filter(node -> node.kind() == NetworkNodeInfo.KIND_PC)
                            .map(node -> node.link().wifi()).filter(found -> found != null).findFirst().orElse(null);
                    helper.assertTrue(wifi != null && wifi.card().equals("standard")
                                    && wifi.accessPoint().equals("transition") && wifi.through().equals(point.name()),
                            "the computer's link is Wi-Fi through it; got " + wifi);
                })
                .thenSucceed();
    }

    /** A computer with no Wi-Fi card stays off the network, however near the access point. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void computer_withNoCardStaysOff(final GameTestHelper helper) {
        network(helper);
        deskAccessPoint(helper, ComputingModule.LEGACY_ACCESS_POINT.get());
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        helper.startSequence()
                .thenIdle(SETTLE)
                .thenExecute(() -> helper.assertTrue(pc.networkUuid() == null && pc.wifiCard() == null,
                        "no card, no network"))
                .thenSucceed();
    }

    /** A computer on a cable keeps its cable for its link, card or no card. */
    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void computer_onACableKeepsItsCable(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper);
        deskAccessPoint(helper, ComputingModule.LEGACY_ACCESS_POINT.get());
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.setBlock(new BlockPos(4, 2, 3), ComputingModule.ETHERNET_CABLE);
        final PersonalComputerBlockEntity pc = withCard(helper, new BlockPos(4, 2, 4),
                ComputingModule.WIFI_CARD_STANDARD.get());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(mainframe.networkUuid() != null
                        && mainframe.networkUuid().equals(pc.networkUuid()), "the computer is on the network"))
                .thenExecute(() -> helper.assertTrue(pc.wirelessLink(helper.getLevel()) == null
                        && pc.ownLinkItemsPerTick(helper.getLevel()) == Integer.MAX_VALUE, "by its cable"))
                .thenSucceed();
    }

    /** An access point reaches as far as its era's Wi-Fi, as the crow flies. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void accessPoint_reachesAsFarAsItsEraSays(final GameTestHelper helper) {
        final AccessPointBlockEntity point = deskAccessPoint(helper, ComputingModule.LEGACY_ACCESS_POINT.get());
        final BlockPos at = point.getBlockPos();
        helper.assertTrue(point.reaches(at.east(24)), "24 blocks away is in reach");
        helper.assertFalse(point.reaches(at.east(25)), "25 is not");
        helper.assertFalse(point.reaches(at.offset(18, 0, 18)), "nor 25.5 across a diagonal");
        helper.succeed();
    }

    /** A Wi-Fi card goes in the small computers alone: the Mainframe and the racks stay on the backbone. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void wifiCard_goesInTheSmallComputersAlone(final GameTestHelper helper) {
        helper.assertTrue(ExpansionCardKind.WIFI.fitsIn(HostScope.PERSONAL_COMPUTER), "a Personal Computer");
        helper.assertTrue(ExpansionCardKind.WIFI.fitsIn(HostScope.CRAFTING_COMPUTER), "a Crafting Computer");
        helper.assertTrue(ExpansionCardKind.WIFI.fitsIn(HostScope.CLUSTER_MANAGEMENT_COMPUTER),
                "a Cluster Management Computer");
        helper.assertFalse(ExpansionCardKind.WIFI.fitsIn(HostScope.MAINFRAME), "not the Mainframe");
        helper.assertFalse(ExpansionCardKind.WIFI.fitsIn(HostScope.WAN_GATEWAY_COMPUTER), "nor a WAN gateway");
        helper.succeed();
    }

    /**
     * One fixed to a ceiling takes its lines through the top alone, and comes off with the block it hangs from,
     * dropping as itself.
     */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void fixedAccessPoint_takesItsLinesThroughItsPlateAndComesOffWithIt(final GameTestHelper helper) {
        final BlockPos ceiling = new BlockPos(3, 4, 3);
        final BlockPos hanging = ceiling.below();
        helper.setBlock(ceiling, Blocks.STONE);
        helper.setBlock(hanging, ComputingModule.STANDARD_ACCESS_POINT.get().defaultBlockState()
                .setValue(AccessPointBlock.MOUNT, AccessPointMount.CEILING));
        final AccessPointBlock block = ComputingModule.STANDARD_ACCESS_POINT.get();
        final BlockState state = helper.getBlockState(hanging);
        helper.assertTrue(block.accepts(state, Direction.UP, SharedEnergy.LOW_VOLTAGE_LINE),
                "its energy comes in through the ceiling");
        helper.assertFalse(block.accepts(state, Direction.DOWN, SharedEnergy.LOW_VOLTAGE_LINE),
                "and through no other face");
        helper.setBlock(ceiling, Blocks.AIR);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    helper.assertBlockNotPresent(ComputingModule.STANDARD_ACCESS_POINT.get(), hanging);
                    final AABB around = new AABB(helper.absolutePos(hanging)).inflate(2.0);
                    helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).stream()
                                    .anyMatch(item -> item.getItem().is(ComputingModule.STANDARD_ACCESS_POINT.get()
                                            .asItem())),
                            "it drops as itself");
                })
                .thenSucceed();
    }

    /**
     * With the energy drawn and none brought to it, an access point is dark and gives no network; energy brought in
     * through the face its cables come in by lights it.
     */
    @GameTest(template = ARENA, timeoutTicks = 300, batch = ENERGY)
    public static void accessPoint_withNoEnergyIsDarkAndLightsWhenFed(final GameTestHelper helper) {
        final AccessPointBlockEntity point = deskAccessPoint(helper, ComputingModule.LEGACY_ACCESS_POINT.get());
        helper.startSequence()
                .thenIdle(SETTLE)
                .thenExecute(() -> {
                    helper.assertTrue(!point.isOn() && point.serving() == null, "dark, it gives nothing");
                    helper.assertTrue(point.energyPort(Direction.EAST) == null,
                            "and takes no energy on its front");
                    TestComputerEnergy.feed(point.energyPort(Direction.WEST));
                })
                .thenWaitUntil(() -> helper.assertTrue(point.isOn(), "fed from behind, it lights"))
                .thenSucceed();
    }

    /* A running Mainframe on its backbone, a Personal Router onto the access line, and the line run east. */
    private static MainframeBlockEntity network(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(MAINFRAME);
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE);
        return mainframe;
    }

    /* A desk access point at the end of the line, its back to it. */
    private static AccessPointBlockEntity deskAccessPoint(final GameTestHelper helper, final AccessPointBlock block) {
        helper.setBlock(ACCESS_POINT, block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
                Direction.EAST).setValue(AccessPointBlock.MOUNT, AccessPointMount.DESK));
        return TestWorldBuilder.forGameTest(helper).blockEntity(ACCESS_POINT, AccessPointBlockEntity.class);
    }

    /* A running Personal Computer with {@code card} in its slot after the graphics card's. */
    private static PersonalComputerBlockEntity withCard(final GameTestHelper helper, final BlockPos at,
                                                        final Item card) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(at);
        pc.getHardware().setStackInSlot(CARD_SLOT, new ItemStack(card));
        return pc;
    }
}
