/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.NetworkGatewayBlock;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.gateway.GatewayManager;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.GatewayManagerActionPayload;
import dev.jstech.computers.operation.payload.GatewayManagerStatePayload;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Gateway Manager's server side and the {@code gateway} command: listing the host's Gateways,
 * renaming, the permission knobs, the shares as ComputerCraft will see them, and emptying the buffer
 * into the network as operations.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class GatewayManagerGameTests {

    private GatewayManagerGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    private static final BlockPos GATEWAY = new BlockPos(6, 2, 2);

    /** The Mainframe with stock, the host computer with a Gateway on its back, and a second computer that shares. */
    private record Fleet(MainframeBlockEntity mainframe, PersonalComputerBlockEntity host, PersonalComputerBlockEntity lab) {
    }

    private static Fleet wire(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(new BlockPos(1, 2, 2));
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE.get());
        world.placeSeededRack(new BlockPos(2, 2, 1));
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE.get());
        final PersonalComputerBlockEntity host = world.placeRunningPersonalComputer(new BlockPos(5, 2, 2));
        host.console().setComputerName("desk");
        world.setBlock(new BlockPos(4, 2, 3), ComputingModule.ETHERNET_CABLE.get());
        final PersonalComputerBlockEntity lab = world.placeRunningPersonalComputer(new BlockPos(5, 2, 3));
        lab.console().setComputerName("lab");
        // The Gateway stands east of the host, its back socket against it.
        helper.setBlock(GATEWAY, ComputingModule.NETWORK_GATEWAY.get().defaultBlockState()
                .setValue(NetworkGatewayBlock.FACING, Direction.EAST));
        return new Fleet(mainframe, host, lab);
    }

    private static NetworkGatewayBlockEntity gateway(final GameTestHelper helper) {
        if (helper.getBlockEntity(GATEWAY) instanceof NetworkGatewayBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("no Gateway at " + GATEWAY);
    }

    private static GatewayManagerStatePayload state(final GameTestHelper helper, final Fleet fleet) {
        return GatewayManager.state(helper.getLevel(), fleet.host(), helper.absolutePos(GATEWAY).asLong(), "");
    }

    private static String act(final GameTestHelper helper, final Fleet fleet, final int action, final int value,
                              final String text) {
        return GatewayManager.act(helper.getLevel(), fleet.host(), helper.absolutePos(GATEWAY).asLong(), action, value, text);
    }

    @GameTest(template = ARENA)
    public static void manager_listsRenamesAndSetsPermissions(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> {
                    final GatewayManagerStatePayload before = state(helper, fleet);
                    helper.assertTrue(before.gateways().size() == 1 && before.gateways().get(0).name().equals("gateway-1"),
                            "the host lists its one Gateway; got " + before.gateways());
                    helper.assertTrue("desk".equals(before.head().hostName()), "under the host's name");
                    helper.assertTrue(before.detail().present() && before.detail().link().equals("adjacent"),
                            "with the selected one in detail; got " + before.detail().link());
                    helper.assertTrue(before.detail().servers() >= 1 && before.detail().mainframeOnline(),
                            "and the network it reaches; servers " + before.detail().servers());
                    helper.assertTrue(before.head().ccInstalled(), "CC: Tweaked is there in the dev runs");

                    final String said = act(helper, fleet, GatewayManagerActionPayload.ACTION_RENAME, 0, "CC Bridge");
                    helper.assertTrue(said.contains("cc-bridge"), "the rename says what it did; got " + said);
                    act(helper, fleet, GatewayManagerActionPayload.ACTION_SET_READ, 0, "");
                    act(helper, fleet, GatewayManagerActionPayload.ACTION_SET_CEILING, 2, "");
                    act(helper, fleet, GatewayManagerActionPayload.ACTION_SET_CAP, 2, "");
                    act(helper, fleet, GatewayManagerActionPayload.ACTION_IDENTIFY, 0, "");

                    final GatewayManagerStatePayload after = state(helper, fleet);
                    final GatewayManagerStatePayload.Detail d = after.detail();
                    helper.assertTrue("cc-bridge".equals(d.name()), "the new name shows; got " + d.name());
                    helper.assertTrue(!d.read() && d.operationsAllowed() && d.ceiling() == 2 && d.cap() == 2,
                            "every knob moved; got read=" + d.read() + " ceiling=" + d.ceiling() + " cap=" + d.cap());
                    helper.assertTrue("jsc_gateway_cc_bridge".equals(d.peripheralName()), "CC's name follows; got " + d.peripheralName());
                    helper.assertTrue(gateway(helper).identifying(), "Identify sets the lights blinking");
                    final List<String> whats = new ArrayList<>();
                    for (final GatewayManagerStatePayload.WireLog row : d.log()) {
                        whats.add(row.what());
                    }
                    helper.assertTrue(whats.contains("rename gateway-1 to cc-bridge") && whats.contains("identify"),
                            "the log has it all, signed by the host; got " + whats);
                    helper.assertTrue(d.log().stream().allMatch(row -> row.who().equals("desk")), "signed by the host");
                    helper.assertTrue(d.recent().size() == GatewayManager.RECENT, "the status tab shows the last few");
                    final String missing = act(helper, fleet, GatewayManagerActionPayload.ACTION_RENAME, 0, "");
                    helper.assertTrue("gateway-1".equals(gateway(helper).name()), "an empty name goes back to the default; " + missing);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void manager_clearsTheBufferIntoTheNetwork(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        final long[] before = new long[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> {
                    final NetworkStorage storage = NetworkStorage.of(helper.getLevel(), fleet.mainframe().networkUuid());
                    before[0] = storage.count(Items.COBBLESTONE);
                    gateway(helper).buffer().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 32));
                    gateway(helper).buffer().setStackInSlot(4, new ItemStack(Items.COBBLESTONE, 8));
                    final String said = act(helper, fleet, GatewayManagerActionPayload.ACTION_CLEAR_BUFFER, 0, "");
                    helper.assertTrue(said.contains("40 items"), "the action says how much it moved; got " + said);
                    helper.assertTrue(gateway(helper).bufferUsed() == 0, "the buffer is empty at once");
                })
                .thenWaitUntil(() -> {
                    final NetworkStorage storage = NetworkStorage.of(helper.getLevel(), fleet.mainframe().networkUuid());
                    helper.assertTrue(storage.count(Items.COBBLESTONE) >= before[0] + 40,
                            "the items are still on their way; " + storage.count(Items.COBBLESTONE));
                })
                .thenExecute(() -> {
                    final String again = act(helper, fleet, GatewayManagerActionPayload.ACTION_CLEAR_BUFFER, 0, "");
                    helper.assertTrue(again.contains("empty"), "an empty buffer says so; got " + again);
                    helper.assertTrue(state(helper, fleet).detail().log().stream()
                            .anyMatch(row -> row.what().equals("clear buffer to network") && row.result().contains("40 items")),
                            "and the log keeps the move");
                })
                .thenSucceed();
    }

    private static List<String> shell(final GameTestHelper helper, final PersonalComputerBlockEntity on, final String command) {
        final ServerCliComputer computer = new ServerCliComputer(on, helper.getLevel());
        final List<String> out = new ArrayList<>();
        for (final CliLine line : CliCommands.newShell(80).run(command, computer).lines()) {
            out.add(line.text());
        }
        return out;
    }

    private static boolean says(final List<String> lines, final String text) {
        return lines.stream().anyMatch(line -> line.contains(text));
    }

    @GameTest(template = ARENA)
    public static void gatewayCommand_printsAndSetsTheSameThings(final GameTestHelper helper) {
        final Fleet fleet = wire(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> {
                    List<String> lines = shell(helper, fleet.host(), "gateway list");
                    helper.assertTrue(says(lines, "gateway-1") && says(lines, "up"), "list names the Gateway and its link; got " + lines);
                    lines = shell(helper, fleet.host(), "gateway gateway-1 rename farm-link");
                    helper.assertTrue(says(lines, "renamed gateway-1 to farm-link"), "rename answers; got " + lines);
                    lines = shell(helper, fleet.host(), "gateway farm-link set operations off");
                    helper.assertTrue(says(lines, "denied"), "set operations answers; got " + lines);
                    lines = shell(helper, fleet.host(), "gateway farm-link set cap 16");
                    helper.assertTrue(says(lines, "16 calls"), "set cap answers; got " + lines);
                    lines = shell(helper, fleet.host(), "gateway farm-link perms");
                    helper.assertTrue(says(lines, "operations: off") && says(lines, "calls a tick: 16"),
                            "perms prints the knobs; got " + lines);
                    lines = shell(helper, fleet.host(), "gateway farm-link status");
                    helper.assertTrue(says(lines, "linked to desk, adjacent") && says(lines, "jsc_gateway_farm_link"),
                            "status prints both sides; got " + lines);
                    lines = shell(helper, fleet.host(), "gateway farm-link log");
                    helper.assertTrue(says(lines, "rename gateway-1 to farm-link"), "log prints what happened; got " + lines);
                    lines = shell(helper, fleet.host(), "gateway nowhere");
                    helper.assertTrue(says(lines, "no gateway named nowhere"), "an unknown name is refused; got " + lines);
                    lines = shell(helper, fleet.lab(), "gateway list");
                    helper.assertTrue(says(lines, "no gateways"), "a computer without one says so; got " + lines);
                })
                .thenSucceed();
    }
}
