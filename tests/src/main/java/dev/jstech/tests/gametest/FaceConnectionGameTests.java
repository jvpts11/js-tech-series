/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.ServerRackPartBlock;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.connect.ConnectedFaces;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.FaceRule;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.connect.IJoinRule;
import dev.jstech.core.connect.Neighbours;
import dev.jstech.core.connect.RelativeFace;
import dev.jstech.core.network.DataLine;
import dev.jstech.core.network.DataLines;
import dev.jstech.core.network.DataLink;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Blocks that know their neighbours: a block's faces named from its own point of view, ports that take a line on
 * their faces and in their generations only, the series' devices taking their cables where they always did, a block
 * entity hearing which face saw its neighbour change, and which neighbours a face's texture runs into.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class FaceConnectionGameTests {

    private static final String ARENA = "empty";
    private static final ResourceLocation ACCESS = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "access");
    private static final ResourceLocation CRAFTING = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "crafting");

    private FaceConnectionGameTests() {
    }

    @GameTest(template = ARENA)
    public static void relativeFace_namesEachFaceOfABlockOnce(final GameTestHelper helper) {
        for (final Direction facing : Direction.values()) {
            final Set<Direction> seen = EnumSet.noneOf(Direction.class);
            for (final RelativeFace face : RelativeFace.values()) {
                final Direction world = face.toWorld(facing);
                helper.assertTrue(seen.add(world), "two faces of a block facing " + facing + " are " + world);
                same(helper, face, RelativeFace.of(facing, world), "the face of " + world + " facing " + facing);
            }
        }
        same(helper, Direction.SOUTH, RelativeFace.BACK.toWorld(Direction.NORTH), "the back of a block facing north");
        same(helper, Direction.WEST, RelativeFace.LEFT.toWorld(Direction.NORTH), "its left, facing out");
        same(helper, Direction.EAST, RelativeFace.RIGHT.toWorld(Direction.NORTH), "its right");
        same(helper, Direction.UP, RelativeFace.TOP.toWorld(Direction.NORTH), "its top");
        same(helper, Direction.NORTH, RelativeFace.TOP.toWorld(Direction.UP), "the top of a block facing up");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void facePorts_takeALineOnTheirFacesOnly(final GameTestHelper helper) {
        final FacePorts ports = FacePorts.builder()
                .port(FaceRule.BACK, new Connection(ACCESS, 1))
                .port(FaceRule.EVERY, Connection.of(CRAFTING))
                .build();
        final BlockState north = Blocks.FURNACE.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);

        helper.assertTrue(ports.accepts(north, Direction.SOUTH, Connection.of(ACCESS)), "access on the back");
        helper.assertTrue(!ports.accepts(north, Direction.NORTH, Connection.of(ACCESS)), "no access on the front");
        helper.assertTrue(ports.accepts(north, Direction.UP, Connection.of(CRAFTING)), "crafting on the top");
        same(helper, Set.of(ACCESS, CRAFTING), ports.lines(), "the lines the ports take");
        helper.assertTrue(!ports.accepts(Blocks.STONE.defaultBlockState(), Direction.SOUTH, Connection.of(ACCESS)),
                "a back rule on a block with no facing");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void connection_takesItsGenerationAndEarlierOnes(final GameTestHelper helper) {
        final Connection port = new Connection(ACCESS, 1);

        helper.assertTrue(port.takes(new Connection(ACCESS, 0)), "an earlier generation");
        helper.assertTrue(port.takes(new Connection(ACCESS, 1)), "its own generation");
        helper.assertTrue(!port.takes(new Connection(ACCESS, 2)), "a later generation");
        helper.assertTrue(!port.takes(new Connection(CRAFTING, 0)), "another line");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void devices_takeTheirCablesWhereTheyAlwaysDid(final GameTestHelper helper) {
        final Connection ethernet = DataLines.of(new DataLink(DataLine.ACCESS, HardwareEra.LEGACY));
        final Connection crafting = DataLines.of(new DataLink(DataLine.CRAFTING, HardwareEra.VINTAGE));
        final Connection hpc = DataLines.of(new DataLink(DataLine.HPC, HardwareEra.STANDARD));
        final BlockState crafter = ComputingModule.CRAFTING_COMPUTER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        final IFaceConnector crafterPorts = (IFaceConnector) crafter.getBlock();

        helper.assertTrue(crafterPorts.accepts(crafter, Direction.SOUTH, ethernet), "Ethernet on its back");
        helper.assertTrue(!crafterPorts.accepts(crafter, Direction.EAST, ethernet), "no Ethernet on its side");
        helper.assertTrue(crafterPorts.accepts(crafter, Direction.EAST, crafting), "the crafting cable on its side");

        final BlockState part = ComputingModule.SERVER_RACK_PART.get().defaultBlockState()
                .setValue(ServerRackPartBlock.FACING, Direction.NORTH);
        final IFaceConnector partPorts = (IFaceConnector) part.getBlock();
        helper.assertTrue(partPorts.accepts(part, Direction.SOUTH, ethernet), "a server cabinet's back");
        helper.assertTrue(!partPorts.accepts(part, Direction.EAST, ethernet), "not a server cabinet's side");
        helper.assertTrue(!partPorts.accepts(part, Direction.SOUTH, hpc), "no fabric on a server cabinet");
        final BlockState compute = part.setValue(ServerRackPartBlock.COMPUTE, true);
        helper.assertTrue(partPorts.accepts(compute, Direction.SOUTH, hpc), "the fabric on a compute cabinet");
        helper.assertTrue(!partPorts.accepts(compute, Direction.SOUTH, ethernet), "no data on a compute cabinet");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void neighbours_tellTheFaceThatChanged(final GameTestHelper helper) {
        final BlockPos speakerAt = new BlockPos(2, 2, 2);
        helper.setBlock(speakerAt, ComputingModule.SPEAKER.get().defaultBlockState());
        final SyncedBlockEntity speaker = Objects.requireNonNull(
                (SyncedBlockEntity) helper.getBlockEntity(speakerAt), "the speaker");
        final List<Direction> heard = new ArrayList<>();
        speaker.fields().whenNeighbourChanges((level, face) -> heard.add(face));

        helper.setBlock(speakerAt.east(), Blocks.STONE);

        same(helper, List.of(Direction.EAST), heard, "the faces the speaker heard change");
        same(helper, Direction.EAST, Neighbours.faceTowards(helper.absolutePos(speakerAt),
                helper.absolutePos(speakerAt.east())), "the face towards the block beside");
        same(helper, null, Neighbours.faceTowards(helper.absolutePos(speakerAt),
                helper.absolutePos(speakerAt.east(2))), "a block that does not touch");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void connectedFaces_maskTheNeighboursThatJoin(final GameTestHelper helper) {
        // A wall of glass three wide and three tall, standing on the x and y axes, seen from the north.
        for (int x = 1; x <= 3; x++) {
            for (int y = 2; y <= 4; y++) {
                helper.setBlock(new BlockPos(x, y, 2), Blocks.GLASS);
            }
        }
        final BlockPos centre = helper.absolutePos(new BlockPos(2, 3, 2));
        final BlockPos corner = helper.absolutePos(new BlockPos(1, 4, 2));
        final IJoinRule glass = IJoinRule.sameBlock();
        final BlockState state = Blocks.GLASS.defaultBlockState();

        same(helper, 0xFF, ConnectedFaces.mask(helper.getLevel(), centre, state, Direction.NORTH, glass),
                "the middle of the wall joins all eight");
        // From the north the block at the lowest x is on the right, so this top corner joins left, down and between.
        final int expected = ConnectedFaces.LEFT | ConnectedFaces.DOWN | ConnectedFaces.DOWN_LEFT;
        same(helper, expected, ConnectedFaces.mask(helper.getLevel(), corner, state, Direction.NORTH, glass),
                "a top corner of the wall");
        same(helper, expected, ConnectedFaces.of(helper.getLevel(), corner, state, glass).of(Direction.NORTH),
                "the same mask among the six");
        helper.succeed();
    }

    private static void same(final GameTestHelper helper, final Object expected, final Object actual,
                             final String what) {
        helper.assertTrue(Objects.equals(expected, actual), what + ": expected " + expected + ", got " + actual);
    }
}
