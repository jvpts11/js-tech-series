/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.part.ExportBusPart;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.network.DataLine;
import dev.jstech.core.network.DataLines;
import dev.jstech.core.network.DataLink;
import dev.jstech.core.network.FailoverRole;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NetworkUuidState;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.NetworkFixtures.networkOf;
import static dev.jstech.tests.testkit.NetworkFixtures.placeRunningMainframe;
import static dev.jstech.tests.testkit.NetworkFixtures.registryState;
import static dev.jstech.tests.testkit.NetworkFixtures.sameNetwork;

/**
 * GameTests for how cables connect and split the data network: severed fragments, extended and rejoined
 * networks, orphaned networks that re-adopt, failover between Mainframes, and which face takes a cable.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class NetworkConnectivityGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private NetworkConnectivityGameTests() {
    }

    @GameTest(template = ARENA)
    public static void cableSever_dropsFarFragmentFromNetwork(final GameTestHelper helper) {
        final BlockPos a = new BlockPos(1, 2, 2);
        final BlockPos c1 = new BlockPos(2, 2, 2);
        final BlockPos c2 = new BlockPos(3, 2, 2);
        final BlockPos c3 = new BlockPos(4, 2, 2);
        placeRunningMainframe(helper, a);
        TestCables.lay(helper, c1, ComputingModule.HBW_CABLE);
        TestCables.lay(helper, c2, ComputingModule.HBW_CABLE);
        TestCables.lay(helper, c3, ComputingModule.HBW_CABLE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(sameNetwork(helper, c1, c3), "c1 and c3 should start on one network");
                    helper.assertTrue(networkOf(helper, c3).isPresent(), "c3 should start networked");
                })
                .thenExecute(() -> helper.setBlock(c2, Blocks.AIR)) // sever the segment
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertFalse(sameNetwork(helper, c1, c3), "c1 and c3 must split into two networks");
                    helper.assertTrue(networkOf(helper, c1).isPresent(), "c1 (next to mainframe) keeps a network");
                    helper.assertTrue(networkOf(helper, c3).isEmpty(), "severed far cable c3 must be network-less");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void cablePlaced_extendsNetworkUuid(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos c1 = new BlockPos(2, 2, 2);
        final BlockPos c2 = new BlockPos(3, 2, 2);
        final BlockPos c3 = new BlockPos(4, 2, 2);
        placeRunningMainframe(helper, m);
        TestCables.lay(helper, c1, ComputingModule.HBW_CABLE);
        TestCables.lay(helper, c2, ComputingModule.HBW_CABLE);
        final NetworkUuid[] uuid = new NetworkUuid[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(networkOf(helper, c2).isPresent(), "c2 is networked before extending");
                    uuid[0] = networkOf(helper, c2).orElseThrow();
                    helper.assertTrue(networkOf(helper, c3).isEmpty(), "c3 is not placed yet");
                })
                .thenExecute(() -> TestCables.lay(helper, c3, ComputingModule.HBW_CABLE)) // extend at runtime
                .thenExecuteAfter(SETTLE, () ->
                        helper.assertTrue(networkOf(helper, c3).equals(Optional.of(uuid[0])),
                                "a cable placed onto a live network joins it and inherits the UUID"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void cableReplaced_rejoinsSeveredFragment(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos c1 = new BlockPos(2, 2, 2);
        final BlockPos c2 = new BlockPos(3, 2, 2);
        final BlockPos c3 = new BlockPos(4, 2, 2);
        placeRunningMainframe(helper, m);
        TestCables.lay(helper, c1, ComputingModule.HBW_CABLE);
        TestCables.lay(helper, c2, ComputingModule.HBW_CABLE);
        TestCables.lay(helper, c3, ComputingModule.HBW_CABLE);
        final NetworkUuid[] uuid = new NetworkUuid[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> uuid[0] = networkOf(helper, c3).orElseThrow())
                .thenExecute(() -> helper.setBlock(c2, Blocks.AIR)) // sever -> c3 fragment goes network-less
                .thenExecuteAfter(SETTLE, () ->
                        helper.assertTrue(networkOf(helper, c3).isEmpty(), "severed far cable loses the network"))
                .thenExecute(() -> TestCables.lay(helper, c2, ComputingModule.HBW_CABLE)) // re-place the bridge
                .thenExecuteAfter(SETTLE, () ->
                        helper.assertTrue(networkOf(helper, c3).equals(Optional.of(uuid[0])),
                                "re-placing the cable rejoins the fragment and restores its UUID"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void mainframeDestroyed_orphansNetworkAndReAdopts(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos c1 = new BlockPos(2, 2, 2);
        final BlockPos c2 = new BlockPos(3, 2, 2);
        placeRunningMainframe(helper, m);
        TestCables.lay(helper, c1, ComputingModule.HBW_CABLE);
        TestCables.lay(helper, c2, ComputingModule.HBW_CABLE);
        final NetworkUuid[] uuid = new NetworkUuid[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    final Optional<NetworkUuid> net = networkOf(helper, c1);
                    helper.assertTrue(net.isPresent(),
                            "cables should carry the mainframe's network while it runs");
                    uuid[0] = net.get();
                    helper.assertTrue(registryState(helper, uuid[0]) == NetworkUuidState.ACTIVE,
                            "a running network is ACTIVE");
                })
                .thenExecute(() -> helper.setBlock(m, Blocks.AIR)) // destroy the mainframe
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(networkOf(helper, c1).equals(Optional.of(uuid[0])),
                            "an orphaned network keeps its UUID on the cables, not erased");
                    helper.assertTrue(networkOf(helper, c2).equals(Optional.of(uuid[0])),
                            "the whole orphaned segment keeps the same UUID");
                    helper.assertTrue(registryState(helper, uuid[0]) == NetworkUuidState.ORPHANED,
                            "destroying the only Mainframe marks the network ORPHANED");
                })
                .thenExecute(() -> placeRunningMainframe(helper, m)) // a replacement on the same topology
                .thenExecuteAfter(SETTLE + 2, () -> {
                    helper.assertTrue(networkOf(helper, c1).equals(Optional.of(uuid[0])),
                            "a new Mainframe re-adopts the orphaned UUID, not a fresh one");
                    helper.assertTrue(registryState(helper, uuid[0]) == NetworkUuidState.ACTIVE,
                            "re-adoption brings the network back to ACTIVE");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 220)
    public static void failover_standbyJoinsPrimaryAndTakesOver(final GameTestHelper helper) {
        final BlockPos primaryPos = new BlockPos(1, 2, 2);
        final BlockPos standbyPos = new BlockPos(5, 2, 2);
        final MainframeBlockEntity primary = placeRunningMainframe(helper, primaryPos); // Failover OFF
        final MainframeBlockEntity standby = placeRunningMainframe(helper, standbyPos);
        standby.toggleFailover(); // ON -> a standby that joins the primary's network
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        TestCables.lay(helper, new BlockPos(3, 2, 2), ComputingModule.HBW_CABLE);
        TestCables.lay(helper, new BlockPos(4, 2, 2), ComputingModule.HBW_CABLE);
        final NetworkUuid[] uuid = new NetworkUuid[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    helper.assertFalse(primary.hasNetworkConflict(), "primary + standby must not conflict");
                    helper.assertFalse(standby.hasNetworkConflict(), "primary + standby must not conflict");
                    uuid[0] = primary.networkUuid();
                    helper.assertTrue(uuid[0] != null, "the primary owns a network");
                    helper.assertTrue(uuid[0].equals(standby.networkUuid()),
                            "the standby joins the primary's network, not its own");
                    helper.assertTrue(standby.failoverRole() == FailoverRole.PASSIVE,
                            "the standby stands by while the primary runs; got " + standby.failoverRole());
                })
                .thenExecute(() -> helper.setBlock(primaryPos, Blocks.AIR)) // destroy the primary
                // The standby takes over only after the takeover delay (60 ticks); wait it out.
                .thenExecuteAfter(70, () -> {
                    helper.assertTrue(standby.failoverRole() == FailoverRole.ACTIVE,
                            "the standby promotes to run the orphaned network; got " + standby.failoverRole());
                    helper.assertTrue(uuid[0].equals(standby.networkUuid()),
                            "the promoted standby keeps the SAME network UUID, never a fresh one");
                    helper.assertTrue(registryState(helper, uuid[0]) == NetworkUuidState.ACTIVE,
                            "the network is ACTIVE again under the promoted standby");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void failover_enablingErasesOwnNetwork(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos c = new BlockPos(2, 2, 2);
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m); // Failover OFF, a primary
        TestCables.lay(helper, c, ComputingModule.HBW_CABLE);
        final NetworkUuid[] uuid = new NetworkUuid[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    uuid[0] = networkOf(helper, c).orElse(null);
                    helper.assertTrue(uuid[0] != null, "the primary owns a network on its cable");
                    helper.assertTrue(registryState(helper, uuid[0]) == NetworkUuidState.ACTIVE, "and it is ACTIVE");
                })
                .thenExecute(mainframe::toggleFailover) // become a standby
                .thenExecuteAfter(SETTLE + 2, () -> {
                    helper.assertTrue(networkOf(helper, c).isEmpty(),
                            "enabling Failover erases the owned network from the cable");
                    helper.assertTrue(mainframe.networkUuid() == null,
                            "a standby with no primary owns no network");
                    helper.assertTrue(registryState(helper, uuid[0]) == null,
                            "the erased network is dropped from the registry");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void failover_lastMemberOutOrphansNotGhostActive(final GameTestHelper helper) {
        final BlockPos primaryPos = new BlockPos(1, 2, 2);
        final BlockPos standbyPos = new BlockPos(5, 2, 2);
        final MainframeBlockEntity primary = placeRunningMainframe(helper, primaryPos);
        final MainframeBlockEntity standby = placeRunningMainframe(helper, standbyPos);
        standby.toggleFailover();
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        TestCables.lay(helper, new BlockPos(3, 2, 2), ComputingModule.HBW_CABLE);
        TestCables.lay(helper, new BlockPos(4, 2, 2), ComputingModule.HBW_CABLE);
        final NetworkUuid[] uuid = new NetworkUuid[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    uuid[0] = primary.networkUuid();
                    helper.assertTrue(uuid[0] != null, "the primary forms a network");
                    helper.assertTrue(registryState(helper, uuid[0]) == NetworkUuidState.ACTIVE, "running network is ACTIVE");
                })
                .thenExecute(() -> helper.setBlock(primaryPos, Blocks.AIR)) // destroy the primary first
                // ...then the standby too, BEFORE it can promote (well under the 60-tick takeover delay)
                .thenExecuteAfter(2, () -> helper.setBlock(standbyPos, Blocks.AIR))
                .thenExecuteAfter(SETTLE, () -> helper.assertTrue(
                        registryState(helper, uuid[0]) == NetworkUuidState.ORPHANED,
                        "with the last Mainframe gone the network must be ORPHANED, not a ghost ACTIVE; got "
                                + registryState(helper, uuid[0])))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void computer_acceptsCableOnRearFaceOnly(final GameTestHelper helper) {
        final BlockPos pc = new BlockPos(2, 2, 2);
        helper.setBlock(pc, ComputingModule.PERSONAL_COMPUTER.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        net.minecraft.core.Direction.NORTH));
        final net.minecraft.world.level.block.state.BlockState state = helper.getBlockState(pc);
        final IFaceConnector block = (IFaceConnector) state.getBlock();
        final Connection ethernet = DataLines.of(new DataLink(DataLine.ACCESS, HardwareEra.LEGACY));
        // A north-facing computer's rear is south: only that face takes a cable.
        helper.assertTrue(block.accepts(state, net.minecraft.core.Direction.SOUTH, ethernet),
                "the rear (south) face must accept a cable");
        helper.assertFalse(block.accepts(state, net.minecraft.core.Direction.NORTH, ethernet),
                "the front must reject a cable");
        helper.assertFalse(block.accepts(state, net.minecraft.core.Direction.EAST, ethernet),
                "a side must reject a cable");
        helper.assertFalse(block.accepts(state, net.minecraft.core.Direction.UP, ethernet),
                "the top must reject a cable");
        // The Mainframe is the exception: it takes its backbone cable on any face.
        final BlockPos mf = new BlockPos(4, 2, 2);
        helper.setBlock(mf, ComputingModule.MAINFRAME.get());
        final net.minecraft.world.level.block.state.BlockState mfState = helper.getBlockState(mf);
        final IFaceConnector mainframe = (IFaceConnector) mfState.getBlock();
        final Connection hbw = DataLines.of(new DataLink(DataLine.BACKBONE, HardwareEra.LEGACY));
        helper.assertTrue(mainframe.accepts(mfState, net.minecraft.core.Direction.EAST, hbw),
                "the Mainframe accepts a cable on any face");
        helper.assertTrue(mainframe.accepts(mfState, net.minecraft.core.Direction.UP, hbw),
                "the Mainframe accepts a cable on any face");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void cablePart_survivesReload(final GameTestHelper helper) {
        final BlockPos cablePos = new BlockPos(2, 2, 2);
        TestCables.lay(helper, cablePos, ComputingModule.HBW_CABLE);
        if (!(helper.getBlockEntity(cablePos) instanceof CableBlockEntity cable)) {
            helper.fail("no cable block entity");
            return;
        }
        final ExportBusPart part = new ExportBusPart(HardwareEra.STANDARD);
        cable.addPart(Direction.EAST, part);
        part.setFilter(new ItemStack(Items.COBBLESTONE));
        part.adjustKeep(5);
        part.adjustMax(20);

        final var registries = helper.getLevel().registryAccess();
        final net.minecraft.nbt.CompoundTag saved = cable.saveWithFullMetadata(registries);
        final var reloaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                helper.absolutePos(cablePos), cable.getBlockState(), saved, registries);
        helper.assertTrue(reloaded instanceof CableBlockEntity, "reloaded BE should be a cable");

        final CableBlockEntity back = (CableBlockEntity) reloaded;
        helper.assertTrue(back.hasPart(Direction.EAST), "the part must survive on the same face");
        helper.assertTrue(!back.hasPart(Direction.WEST), "no part should appear on an empty face");
        helper.assertTrue(back.getPart(Direction.EAST) instanceof ExportBusPart, "the part type must persist");
        final ExportBusPart reloadedPart = (ExportBusPart) back.getPart(Direction.EAST);
        helper.assertTrue(reloadedPart.filterItem() == Items.COBBLESTONE, "the filter must persist");
        helper.assertTrue(reloadedPart.keep() == 5, "keep must persist");
        helper.assertTrue(reloadedPart.max() == 20, "max must persist");
        helper.succeed();
    }
}
