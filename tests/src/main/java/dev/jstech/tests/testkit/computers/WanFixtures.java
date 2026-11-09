/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.blockentity.WanGatewayBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.program.Programs;
import dev.jstech.tests.testkit.TestCables;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/**
 * Two places a long-distance line apart, in the {@code wan} arena: near, a WAN Gateway Computer facing north whose port
 * 1 takes the line, its Mainframe on the backbone at its east end; far, a gateway facing south whose port 4 takes the
 * other end of the line, its Mainframe and a seeded rack beside it. Each piece is laid on its own, so a test leaves out
 * what it is about (the far Mainframe, for an extension).
 */
public final class WanFixtures {

    public static final BlockPos NEAR_GATEWAY = new BlockPos(4, 2, 1);
    public static final BlockPos NEAR_MAINFRAME = new BlockPos(7, 2, 1);
    public static final BlockPos FAR_GATEWAY = new BlockPos(4, 2, 7);
    public static final BlockPos FAR_MAINFRAME = new BlockPos(8, 2, 7);
    public static final BlockPos FAR_RACK = new BlockPos(8, 2, 9);
    /** The near gateway's port the line takes, and the far one's. */
    public static final int NEAR_PORT = 1;
    public static final int FAR_PORT = 4;
    /* The line, from the back of the near port 1 round to the back of the far port 4, touching no other port. */
    private static final List<BlockPos> LINE = List.of(new BlockPos(2, 2, 3), new BlockPos(2, 2, 4),
            new BlockPos(2, 2, 5), new BlockPos(3, 2, 5));

    private WanFixtures() {
    }

    /** The near gateway, running MC-NET and BorderLink, a VLDC Card for its port 1, the backbone at its east end. */
    public static WanGatewayBlockEntity nearGateway(final GameTestHelper helper) {
        final WanGatewayBlockEntity gateway = gateway(helper, NEAR_GATEWAY, Direction.NORTH, NEAR_PORT);
        TestCables.lay(helper, new BlockPos(6, 2, 1), ComputingModule.HBW_CABLE);
        return gateway;
    }

    /** The far gateway, the same, with VLDC Cards for its four ports and the backbone at its east end. */
    public static WanGatewayBlockEntity farGateway(final GameTestHelper helper) {
        final WanGatewayBlockEntity gateway = gateway(helper, FAR_GATEWAY, Direction.SOUTH, FAR_PORT);
        TestCables.lay(helper, new BlockPos(7, 2, 7), ComputingModule.HBW_CABLE);
        return gateway;
    }

    /** The near place's Mainframe, at the end of its backbone, running a network system. */
    public static MainframeBlockEntity nearMainframe(final GameTestHelper helper) {
        return TestWorldBuilder.forGameTest(helper).placeMainframeWithSystem(NEAR_MAINFRAME,
                TestWorldBuilder.NETWORK_OS);
    }

    /** The far place's Mainframe. */
    public static MainframeBlockEntity farMainframe(final GameTestHelper helper) {
        return TestWorldBuilder.forGameTest(helper).placeMainframeWithSystem(FAR_MAINFRAME,
                TestWorldBuilder.NETWORK_OS);
    }

    /** A rack on the far place's backbone, past its Mainframe, with a server and its drives. */
    public static ServerRackBlockEntity farRack(final GameTestHelper helper) {
        TestCables.lay(helper, new BlockPos(8, 2, 8), ComputingModule.HBW_CABLE);
        return TestWorldBuilder.forGameTest(helper).placeSeededRack(FAR_RACK, Direction.SOUTH);
    }

    /** The long-distance line between the two ports, of the Standard's VLDC. */
    public static void line(final GameTestHelper helper) {
        for (final BlockPos at : LINE) {
            TestCables.lay(helper, at, ComputingModule.VLDC_CABLE);
        }
    }

    /** One of the line's cables, the one in the middle. */
    public static BlockPos middleOfLine() {
        return LINE.get(1);
    }

    /*
     * A Standard gateway at {@code origin}, formed, built with a disk, switched on with MC-NET and BorderLink, and a
     * VLDC Card in each slot up to the one for {@code port}.
     */
    private static WanGatewayBlockEntity gateway(final GameTestHelper helper, final BlockPos origin,
                                                 final Direction facing, final int port) {
        final WanGatewayBlockEntity gateway = NetworkFixtures.formRunningGateway(helper, origin, facing);
        gateway.getInventory().setStackInSlot(AbstractCabinetComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        for (int k = 0; k < port; k++) {
            gateway.getInventory().setStackInSlot(AbstractCabinetComputerBlockEntity.GPU_SLOTS_START + k,
                    new ItemStack(ComputingModule.VLDC_CARD.get()));
        }
        if (!gateway.installOs(TestWorldBuilder.NETWORK_OS)) {
            throw new IllegalStateException("could not install the network system on the gateway at " + origin);
        }
        gateway.console().install(Programs.BORDERLINK.toString());
        return gateway;
    }
}
