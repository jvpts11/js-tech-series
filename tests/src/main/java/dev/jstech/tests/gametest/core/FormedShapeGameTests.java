/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.machine.MachineStructure;
import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.shape.CellSpace;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.multiblock.shape.FormedShape;
import dev.jstech.core.multiblock.shape.FormedShapes;
import dev.jstech.core.multiblock.shape.MeshVoxels;
import dev.jstech.industrial.crc.CrcContent;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestModels;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The blocks of a formed structure shaped as their part of its formed model: each merged block of the test turbine
 * knows which block of the model it is and stands as the model's shape file says, turned as the turbine stands; a ray
 * stops at that shape, not at the whole block, and a thing dropped on it rests on it; and the shape files of every
 * formed model are there.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class FormedShapeGameTests {

    private static final String ARENA = "empty";
    /* The terminal, in the middle of the south side of a turbine built facing north. */
    private static final BlockPos TERMINAL = new BlockPos(4, 1, 5);
    private static final int WAIT = 200;
    private static final int SIXTEENTHS = MeshVoxels.FINE;
    private static final double CLOSE = 1.0e-6;
    /* How far a stand set on a shape may rest from its top: it settles within a hair. */
    private static final double RESTS = 0.01;
    /* Half an armour stand's width, in sixteenths. */
    private static final int HALF_STAND = 4;

    private FormedShapeGameTests() {
    }

    @GameTest(template = ARENA)
    public static void shapeFiles_ofEveryFormedModelAreThere(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        for (final ResourceLocation model : List.of(TestModels.TURBINE.id(), CrcContent.MODEL.id())) {
            final Optional<FormedShape> shape = FormedShapes.get(level, model);
            helper.assertTrue(shape.isPresent() && !shape.get().cells().isEmpty(),
                    "the shape file of " + model + " is read");
        }
        helper.assertTrue(FormedShapes.get(level, CrcContent.MODEL.id()).orElseThrow().cells().size() == 80,
                "a block for each place of the CRC's pattern but its terminal");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = WAIT)
    public static void turbine_mergedBlocksTakeTheShapesOfTheirPartOfTheModel(final GameTestHelper helper) {
        final List<BlockPos> iron = build(helper, false);
        helper.succeedWhen(() -> {
            helper.assertTrue(structure(helper).merged(), "the turbine forms and merges");
            final ServerLevel level = helper.getLevel();
            for (final BlockPos at : iron) {
                final BlockPos pos = helper.absolutePos(at);
                helper.assertTrue(level.getBlockEntity(pos) instanceof FormedPartBlockEntity part
                        && part.place() != null && part.place().model().equals(TestModels.TURBINE.id()),
                        "each merged block knows its model: " + at);
                final FormedPlace place = ((FormedPartBlockEntity) level.getBlockEntity(pos)).place();
                final BlockPos apart = at.subtract(TERMINAL);
                helper.assertTrue(place.x() == apart.getX() && place.y() == apart.getY()
                        && place.z() == apart.getZ(), "and which of its blocks it is, as the pattern stands it: "
                        + place + " at " + apart);
                helper.assertTrue(shapedAsItsModel(level, pos, Orientation.NORTH),
                        "and stands as the shape file says that block does: " + at);
            }
        });
    }

    @GameTest(template = ARENA, timeoutTicks = WAIT)
    public static void turbine_builtTurnedTurnsItsShapes(final GameTestHelper helper) {
        final List<BlockPos> iron = build(helper, true);
        helper.succeedWhen(() -> {
            final MachineStructure structure = structure(helper);
            helper.assertTrue(structure.merged(), "the turbine built facing east forms and merges");
            final Orientation turn = structure.orientation().orElseThrow();
            for (final BlockPos at : iron) {
                helper.assertTrue(shapedAsItsModel(helper.getLevel(), helper.absolutePos(at), turn),
                        "each block stands turned as the turbine is: " + at);
            }
        });
    }

    @GameTest(template = ARENA, timeoutTicks = WAIT)
    public static void turbine_aRayStopsAtItsShapeNotTheWholeBlock(final GameTestHelper helper) {
        final List<BlockPos> iron = build(helper, false);
        helper.succeedWhen(() -> {
            helper.assertTrue(structure(helper).merged(), "the turbine forms and merges");
            final ServerLevel level = helper.getLevel();
            final Optional<Column> low = lowColumn(level, helper, iron, 0);
            helper.assertTrue(low.isPresent(), "a block of its top whose shape is lower than the block somewhere");
            final Column column = low.get();
            final Vec3 from = new Vec3(column.x(), column.pos().getY() + 3, column.z());
            final Vec3 to = new Vec3(column.x(), column.pos().getY() - 1, column.z());
            final BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, CollisionContext.empty()));
            helper.assertTrue(hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(column.pos()),
                    "the ray hits the merged block: " + hit.getBlockPos() + " for " + column.pos());
            helper.assertTrue(Math.abs(hit.getLocation().y - column.top()) < CLOSE,
                    "at the top of its shape there, " + column.top() + ", not of the block: " + hit.getLocation());
        });
    }

    @GameTest(template = ARENA, timeoutTicks = WAIT * 2)
    public static void turbine_aThingDroppedOnItRestsOnItsShape(final GameTestHelper helper) {
        final List<BlockPos> iron = build(helper, false);
        final List<ArmorStand> stands = new ArrayList<>();
        final List<Column> columns = new ArrayList<>();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(structure(helper).merged(), "the turbine merges first"))
                .thenExecute(() -> {
                    final Optional<Column> low = lowColumn(helper.getLevel(), helper, iron, HALF_STAND);
                    helper.assertTrue(low.isPresent(), "a block of its top lower than the block under a stand");
                    columns.add(low.get());
                    final ArmorStand stand = EntityType.ARMOR_STAND.create(helper.getLevel());
                    helper.assertTrue(stand != null, "a stand is made");
                    stand.moveTo(low.get().x(), low.get().pos().getY() + 2, low.get().z(), 0, 0);
                    helper.getLevel().addFreshEntity(stand);
                    stands.add(stand);
                })
                .thenWaitUntil(() -> {
                    final ArmorStand stand = stands.getFirst();
                    helper.assertTrue(stand.onGround(), "the stand comes to rest");
                    helper.assertTrue(Math.abs(stand.getY() - columns.getFirst().top()) < RESTS,
                            "on the top of the shape under it, " + columns.getFirst().top() + ": " + stand.getY());
                })
                .thenExecute(() -> stands.getFirst().discard())
                .thenSucceed();
    }

    /*
     * Whether the merged block at {@code pos} collides as the shape file says its block of the model does, turned
     * {@code turn}.
     */
    private static boolean shapedAsItsModel(final ServerLevel level, final BlockPos pos, final Orientation turn) {
        if (!(level.getBlockEntity(pos) instanceof FormedPartBlockEntity part) || part.place() == null
                || !part.place().turn().equals(turn)) {
            return false;
        }
        final FormedPlace place = part.place();
        final Optional<int[]> boxes = FormedShapes.get(level, place.model()).flatMap(file -> file.boxes(
                place.cell()));
        if (boxes.isEmpty()) {
            return false;
        }
        final VoxelShape wanted = voxels(CellSpace.boxes(boxes.get(), turn));
        final VoxelShape stands = level.getBlockState(pos).getCollisionShape(level, pos);
        return !wanted.isEmpty() && !Shapes.joinIsNotEmpty(wanted, stands, BooleanOp.NOT_SAME);
    }

    /*
     * A place on the top of one of the turbine's upper blocks where its shape is lower than the block, as high across
     * {@code reach} sixteenths round it as at it, and how high it is there, in the world.
     */
    private static Optional<Column> lowColumn(final ServerLevel level, final GameTestHelper helper,
                                              final List<BlockPos> iron, final int reach) {
        for (final BlockPos at : iron) {
            if (at.getY() != TERMINAL.getY() + 1) {
                continue;
            }
            final BlockPos pos = helper.absolutePos(at);
            if (!(level.getBlockEntity(pos) instanceof FormedPartBlockEntity part) || part.place() == null) {
                continue;
            }
            final FormedPlace place = part.place();
            final Optional<int[]> file = FormedShapes.get(level, place.model()).flatMap(shape -> shape.boxes(
                    place.cell()));
            if (file.isEmpty()) {
                continue;
            }
            final int[] boxes = CellSpace.boxes(file.get(), place.turn());
            for (int x = 1 + reach; x < SIXTEENTHS - 1 - reach; x++) {
                for (int z = 1 + reach; z < SIXTEENTHS - 1 - reach; z++) {
                    final int top = top(boxes, x - reach, x + reach, z - reach, z + reach);
                    if (top > 0 && top < SIXTEENTHS) {
                        return Optional.of(new Column(pos, pos.getX() + (x + 0.5) / SIXTEENTHS,
                                pos.getZ() + (z + 0.5) / SIXTEENTHS, pos.getY() + (double) top / SIXTEENTHS));
                    }
                }
            }
        }
        return Optional.empty();
    }

    /* The top of the highest box over any sixteenth from x0 to x1 and z0 to z1, in sixteenths; 0 for none. */
    private static int top(final int[] boxes, final int x0, final int x1, final int z0, final int z1) {
        int top = 0;
        for (int b = 0; b + MeshVoxels.BOX <= boxes.length; b += MeshVoxels.BOX) {
            if (boxes[b] <= x1 && boxes[b + 3] > x0 && boxes[b + 2] <= z1 && boxes[b + 5] > z0) {
                top = Math.max(top, boxes[b + 4]);
            }
        }
        return top;
    }

    private static VoxelShape voxels(final int[] boxes) {
        VoxelShape out = Shapes.empty();
        for (int b = 0; b + MeshVoxels.BOX <= boxes.length; b += MeshVoxels.BOX) {
            out = Shapes.or(out, Shapes.box((double) boxes[b] / SIXTEENTHS, (double) boxes[b + 1] / SIXTEENTHS,
                    (double) boxes[b + 2] / SIXTEENTHS, (double) boxes[b + 3] / SIXTEENTHS,
                    (double) boxes[b + 4] / SIXTEENTHS, (double) boxes[b + 5] / SIXTEENTHS));
        }
        return out;
    }

    /* The turbine, as the formed model tests build it: its iron's places. */
    private static List<BlockPos> build(final GameTestHelper helper, final boolean turned) {
        final List<BlockPos> iron = new ArrayList<>();
        for (int y = 0; y < 2; y++) {
            for (int a = -1; a <= 1; a++) {
                for (int b = 0; b <= 2; b++) {
                    final BlockPos at = turned ? TERMINAL.offset(b, y, a) : TERMINAL.offset(a, y, -b);
                    if (!at.equals(TERMINAL)) {
                        helper.setBlock(at, Blocks.IRON_BLOCK);
                        iron.add(at);
                    }
                }
            }
        }
        helper.setBlock(TERMINAL, TestModels.TURBINE_BLOCK.get());
        return iron;
    }

    private static MachineStructure structure(final GameTestHelper helper) {
        final TestModels.TurbineEntity turbine = helper.getBlockEntity(TERMINAL);
        return turbine.structure();
    }

    /* A place on a merged block's top: the block, where across it, and how high its shape is there. */
    private record Column(BlockPos pos, double x, double z, double top) {
    }
}
