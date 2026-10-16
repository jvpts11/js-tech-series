/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.network.ConnectivityIndex;
import dev.jstech.core.network.DataLine;
import dev.jstech.core.network.DataLink;
import dev.jstech.core.network.NetworkCategory;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.network.NetworkTables;
import dev.jstech.core.network.StateRow;
import dev.jstech.core.network.StateTable;
import dev.jstech.core.operation.OperationType;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.util.DeclarationError;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestNetworkMembers;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Machines of any mod on the data network through the Core: a reactor beside a data cable of a network is a member of
 * it, read and never given work, and leaves once the cable goes; its state is a row of the test mod's table, a column
 * for each value; an accelerator carries out the test mod's own kind of Operation; a member that says what cannot
 * stand stops as it is made; and a member's identity is kept in a save.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class NetworkMemberGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos CABLE = new BlockPos(2, 2, 2);
    private static final double EPSILON = 1.0e-9;

    private NetworkMemberGameTests() {
    }

    @GameTest(template = ARENA)
    public static void members_joinTheNetworkOfACableBesideThemAndLeaveWhenItGoes(final GameTestHelper helper) {
        final NetworkUuid network = cable(helper);
        helper.setBlock(CABLE.west(), TestNetworkMembers.REACTOR.get());
        final NetworkSystem system = NetworkSystem.get(helper.getLevel());
        // A block entity is loaded the tick after its block is placed, and joins the network then.
        helper.startSequence().thenWaitUntil(() -> {
            final List<NetworkSystem.MemberNode> members = system.membersOf(network);
            helper.assertTrue(members.size() == 1, "the reactor on the network, of " + members.size());
            helper.assertTrue(members.getFirst().category() == NetworkCategory.B, "read, never given work");
            helper.assertTrue(members.getFirst().pos() == helper.absolutePos(CABLE.west()).asLong(),
                    "where it stands");
            helper.assertTrue(network.equals(reactor(helper, CABLE.west()).membership().network().orElse(null)),
                    "and it knows which network it is on");
        }).thenExecute(() -> connectivity(helper).onCableRemoved(helper.absolutePos(CABLE).asLong()))
                .thenWaitUntil(() -> helper.assertTrue(system.membersOf(network).isEmpty(),
                        "off the network once its cable went"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void tables_holdARowForEachMemberAndAColumnForEachValue(final GameTestHelper helper) {
        final NetworkUuid network = cable(helper);
        helper.setBlock(CABLE.west(), TestNetworkMembers.REACTOR.get());
        helper.setBlock(CABLE.east(), TestNetworkMembers.REACTOR.get());
        reactor(helper, CABLE.west()).reads(350.0, 1200.0, true, "Alpha");
        reactor(helper, CABLE.east()).reads(20.0, 0.0, false, "Beta");
        helper.startSequence().thenWaitUntil(() -> {
            final List<StateRow> rows = NetworkTables.rows(helper.getLevel(), network, TestNetworkMembers.REACTORS);
            helper.assertTrue(rows.size() == 2, "a row for each reactor, of " + rows.size());
            final StateRow alpha = "Alpha".equals(rows.getFirst().text("name").orElse("")) ? rows.getFirst()
                    : rows.get(1);
            helper.assertTrue(Math.abs(alpha.number("temperature").orElse(-1) - 350.0) < EPSILON, "its temperature");
            helper.assertTrue(Math.abs(alpha.number("power").orElse(-1) - 1200.0) < EPSILON, "its power");
            helper.assertTrue(alpha.flag("running").orElse(false), "and that it runs");
            helper.assertTrue(alpha.number("name").isEmpty(), "a text column gives no number");
            final List<StateTable> tables = NetworkTables.tablesOf(helper.getLevel(), network);
            helper.assertTrue(tables.equals(List.of(TestNetworkMembers.REACTORS)),
                    "the network has rows in that table");
        }).thenExecute(() -> clear(helper, network)).thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void members_carryOutTheKindsOfOperationTheyDeclare(final GameTestHelper helper) {
        final NetworkUuid network = cable(helper);
        helper.setBlock(CABLE.west(), TestNetworkMembers.REACTOR.get());
        helper.setBlock(CABLE.east(), TestNetworkMembers.ACCELERATOR.get());
        final NetworkSystem system = NetworkSystem.get(helper.getLevel());
        helper.startSequence().thenWaitUntil(() -> {
            helper.assertTrue(system.membersOf(network).size() == 2, "both on the network");
            final List<NetworkSystem.MemberNode> able = system.membersCarryingOut(network,
                    TestNetworkMembers.ACCELERATE.id());
            helper.assertTrue(able.size() == 1 && able.getFirst().pos() == helper.absolutePos(CABLE.east()).asLong(),
                    "only the accelerator carries out the test mod's kind of Operation");
            helper.assertTrue(able.getFirst().category() == NetworkCategory.C, "a member given work");
        }).thenExecute(() -> clear(helper, network)).thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void members_thatCannotBeWhatTheySay_stopAsTheyAreMade(final GameTestHelper helper) {
        final BlockPos pos = helper.absolutePos(CABLE);
        try {
            new TestNetworkMembers.ReactorBlockEntity(TestNetworkMembers.REACTOR_ENTITY.get(), pos,
                    TestNetworkMembers.REACTOR.get().defaultBlockState()) {
                @Override
                public NetworkCategory category() {
                    return NetworkCategory.A;
                }
            };
            throw new GameTestAssertException("a member that only carries the network was made");
        } catch (final DeclarationError expected) {
            helper.assertTrue(expected.getMessage().contains("no member"), "it says why: " + expected.getMessage());
        }
        try {
            new TestNetworkMembers.ReactorBlockEntity(TestNetworkMembers.REACTOR_ENTITY.get(), pos,
                    TestNetworkMembers.REACTOR.get().defaultBlockState()) {
                @Override
                public List<OperationType<?>> carriesOut() {
                    return List.of(TestNetworkMembers.ACCELERATE);
                }
            };
            throw new GameTestAssertException("a member only read that carries out work was made");
        } catch (final DeclarationError expected) {
            helper.assertTrue(expected.getMessage().contains("only read"), "it says why: " + expected.getMessage());
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void stateRows_refuseValuesTheirColumnsDoNotHold(final GameTestHelper helper) {
        final StateRow.Writer row = new StateRow.Writer(TestNetworkMembers.REACTORS);
        refused(helper, () -> row.set("temperature", "hot"), "text in a column of numbers");
        refused(helper, () -> row.set("colour", 1.0), "a column the table has not");
        refused(helper, () -> row.set("running", 1.0), "a number in a column of flags");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void members_keepTheirIdentityInASave(final GameTestHelper helper) {
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        helper.setBlock(CABLE, TestNetworkMembers.REACTOR.get());
        final TestNetworkMembers.ReactorBlockEntity reactor = reactor(helper, CABLE);
        final NodeUuid node = reactor.membership().node();
        final CompoundTag saved = reactor.saveWithoutMetadata(registries);
        final TestNetworkMembers.ReactorBlockEntity read = new TestNetworkMembers.ReactorBlockEntity(
                TestNetworkMembers.REACTOR_ENTITY.get(), reactor.getBlockPos(), reactor.getBlockState());
        read.loadWithComponents(saved, registries);
        helper.assertTrue(read.membership().node().equals(node), "the same identity read back");
        helper.succeed();
    }

    /* A data cable of a network of its own at {@code CABLE}, as a network's cable is to the Core. */
    private static NetworkUuid cable(final GameTestHelper helper) {
        final long at = helper.absolutePos(CABLE).asLong();
        final NetworkUuid network = new NetworkUuid(UUID.randomUUID());
        connectivity(helper).onCablePlaced(at, Set.of(), new DataLink(DataLine.ACCESS, HardwareEra.STANDARD));
        connectivity(helper).assignUuid(at, network);
        return network;
    }

    private static void clear(final GameTestHelper helper, final NetworkUuid network) {
        connectivity(helper).onCableRemoved(helper.absolutePos(CABLE).asLong());
        connectivity(helper).clearNetwork(network);
    }

    private static ConnectivityIndex connectivity(final GameTestHelper helper) {
        return NetworkSystem.get(helper.getLevel()).connectivity();
    }

    private static TestNetworkMembers.ReactorBlockEntity reactor(final GameTestHelper helper, final BlockPos at) {
        if (!(helper.getBlockEntity(at) instanceof TestNetworkMembers.ReactorBlockEntity reactor)) {
            throw new GameTestAssertException("no reactor at " + at.toShortString());
        }
        return reactor;
    }

    private static void refused(final GameTestHelper helper, final Runnable write, final String what) {
        try {
            write.run();
        } catch (final IllegalArgumentException expected) {
            return;
        }
        throw new GameTestAssertException("the row took " + what);
    }
}
