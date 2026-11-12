/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.AccessPointBlock;
import dev.jstech.computers.block.MainframeStructure;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.integration.create.CreateComputers;
import dev.jstech.computers.integration.create.KineticTable;
import dev.jstech.computers.os.install.OsInstallJob;
import dev.jstech.computers.wireless.AccessPointMount;
import dev.jstech.core.bigblock.BigBlockPartBlockEntity;
import dev.jstech.core.integration.sable.SableIntegration;
import dev.jstech.core.moving.ForeignMover;
import dev.jstech.core.moving.ForeignMovers;
import dev.jstech.core.region.Box;
import dev.jstech.core.uuid.NodeUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.NetworkFixtures;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * J's Computers with Create (computers/docs/design/implementation.md, "Create"): the work is on exactly when Create
 * is; a bearing's contraption carries a running computer and sets it down switched off, keeping its system, its parts
 * and its node, while a running computer beside it is only asked and goes on running; a computer installing a system
 * stays where it is, so the bearing does not assemble; a big machine rides whole, comes down switched off and each of
 * its parts still finds its origin, however the bearing turned it; and, with Sable, a
 * computer aboard a sub-level goes on running and joins the network by Wi-Fi from where the sub-level stands. Each
 * test with a mod absent checks only that the work is off; run them with {@code -PwithCreate}, and with
 * {@code -PwithSable} for the ship. The classes of the mods are reached only from the nested classes, which load only
 * when their mod is present.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CreateComputersGameTests {

    private static final String ARENA = "empty";
    /* Its own batch: it holds Sable's physics still while it runs. */
    private static final String SHIP = "jsc_create_ship";
    /* A bearing turned by the motor under it, the computer it carries on top, and one beside that it does not. */
    private static final BlockPos BEARING = new BlockPos(6, 3, 6);
    private static final BlockPos ON_THE_BEARING = BEARING.above();
    private static final BlockPos BESIDE = ON_THE_BEARING.east();
    /* The ground's network: its Mainframe, the line to a desk access point; the ship and the computer aboard. */
    private static final BlockPos MAINFRAME = new BlockPos(1, 2, 2);
    private static final BlockPos ACCESS_POINT = new BlockPos(5, 2, 2);
    private static final BlockPos ABOARD = new BlockPos(4, 3, 9);
    private static final int CARD_SLOT = PersonalComputerBlockEntity.GPU_SLOTS_START + 1;
    /* How long a bearing left to itself is given to assemble. */
    private static final int GIVEN = 20;

    private CreateComputersGameTests() {
    }

    @GameTest(template = ARENA)
    public static void create_isMetExactlyWhenItIsPresent(final GameTestHelper helper) {
        helper.assertTrue(CreateComputers.on() == create(), "J's Computers works with Create exactly when Create is"
                + " present: " + CreateComputers.on());
        helper.assertTrue(KineticTable.declared() == create(), "and has its create_kinetics table only then");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void contraption_carriesAComputerAndSetsItDownSwitchedOff(final GameTestHelper helper) {
        if (!create()) {
            helper.assertFalse(CreateComputers.on(), "without Create the work is off");
            helper.succeed();
            return;
        }
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity carried = EraNetwork.runningComputer(world, EraKit.STANDARD,
                ON_THE_BEARING);
        final PersonalComputerBlockEntity beside = EraNetwork.runningComputer(world, EraKit.STANDARD, BESIDE);
        final NodeUuid node = carried.nodeUuid();
        WithCreate.motor(helper, BEARING.below());
        WithCreate.bearing(helper, BEARING);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(WithCreate.isContraption(helper, BEARING),
                        "the bearing, turned, takes the computer into its contraption"))
                .thenExecute(() -> {
                    helper.assertTrue(helper.getBlockState(ON_THE_BEARING).isAir(), "the computer rides it");
                    helper.assertTrue(helper.getBlockEntity(BESIDE) == beside && beside.isManualOn(),
                            "the one beside, only asked, stays where it is and goes on running");
                    // Stopped, the bearing sets down what it turned.
                    helper.setBlock(BEARING.below(), Blocks.AIR);
                })
                .thenWaitUntil(() -> helper.assertTrue(helper.getLevel().getBlockEntity(
                                helper.absolutePos(ON_THE_BEARING)) instanceof PersonalComputerBlockEntity,
                        "the bearing, stopped, sets the computer down"))
                .thenIdle(5)
                .thenExecute(() -> {
                    final PersonalComputerBlockEntity set = helper.getBlockEntity(ON_THE_BEARING);
                    helper.assertFalse(set.isManualOn(), "it comes down switched off, and stays off");
                    helper.assertTrue(set.installedOs() != null, "with its system");
                    helper.assertFalse(set.getHardware().getStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT)
                            .isEmpty(), "its parts");
                    helper.assertTrue(node.equals(set.nodeUuid()), "and its node: " + set.nodeUuid());
                    helper.assertTrue(beside.isManualOn(), "the one beside still runs");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void contraption_leavesAComputerInstallingASystemWhereItIs(final GameTestHelper helper) {
        if (!create()) {
            helper.assertFalse(CreateComputers.on(), "without Create the work is off");
            helper.succeed();
            return;
        }
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity computer = EraNetwork.runningComputer(world, EraKit.STANDARD,
                ON_THE_BEARING);
        helper.assertTrue(WithCreate.movable(helper.getLevel(), helper.absolutePos(ON_THE_BEARING)),
                "a running computer may ride");
        helper.assertTrue(computer.isManualOn(), "and being asked leaves it running");
        computer.setInstalling(new OsInstallJob(EraKit.STANDARD.desktopOs().toString(), -1, OsInstallJob.NO_READER,
                10_000, 10_000));
        helper.assertFalse(WithCreate.movable(helper.getLevel(), helper.absolutePos(ON_THE_BEARING)),
                "one installing a system may not");
        WithCreate.motor(helper, BEARING.below());
        WithCreate.bearing(helper, BEARING);
        helper.startSequence()
                .thenIdle(GIVEN)
                .thenExecute(() -> {
                    helper.assertFalse(WithCreate.isContraption(helper, BEARING),
                            "the bearing does not assemble: Create says the computer cannot be moved");
                    helper.assertTrue(helper.getBlockEntity(ON_THE_BEARING) == computer && computer.isManualOn()
                            && computer.installing() != null, "and it goes on installing where it is");
                    computer.setInstalling(null);
                    helper.setBlock(BEARING.below(), Blocks.AIR);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void contraption_carriesABigMachineWholeAndItsPartsStayBound(final GameTestHelper helper) {
        if (!create()) {
            helper.assertFalse(CreateComputers.on(), "without Create the work is off");
            helper.succeed();
            return;
        }
        final MainframeBlockEntity mainframe = NetworkFixtures.formRunningMainframe(helper, ON_THE_BEARING,
                Direction.NORTH);
        final ServerLevel level = helper.getLevel();
        final BlockPos origin = mainframe.getBlockPos();
        helper.assertTrue(WithCreate.movable(level, origin.east()), "Create may move a part of the Mainframe");
        helper.assertTrue(WithCreate.attached(level, origin, Direction.EAST),
                "and the origin holds on to it, so the Mainframe goes whole");
        WithCreate.motor(helper, BEARING.below());
        WithCreate.bearing(helper, BEARING);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(WithCreate.isContraption(helper, BEARING),
                        "the bearing, turned, takes the Mainframe into its contraption"))
                .thenExecute(() -> {
                    for (final BlockPos cell : MainframeStructure.allPositions(origin, Direction.NORTH)) {
                        helper.assertTrue(level.getBlockState(cell).isAir(), "every block of it rides: " + cell);
                    }
                    helper.setBlock(BEARING.below(), Blocks.AIR);
                })
                .thenWaitUntil(() -> helper.assertTrue(level.getBlockEntity(origin) instanceof MainframeBlockEntity,
                        "the bearing, stopped, sets the Mainframe down"))
                .thenIdle(5)
                .thenExecute(() -> {
                    final MainframeBlockEntity set = (MainframeBlockEntity) level.getBlockEntity(origin);
                    final Direction facing = set.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
                    final List<BlockPos> parts = MainframeStructure.partPositions(origin, facing);
                    for (final BlockPos cell : parts) {
                        helper.assertTrue(level.getBlockEntity(cell) instanceof BigBlockPartBlockEntity part
                                && origin.equals(part.originPos()), "each part still finds its origin: " + cell);
                        helper.assertTrue(level.getBlockState(cell).getValue(HorizontalDirectionalBlock.FACING)
                                == facing, "and faces as it does: " + cell);
                    }
                    helper.assertFalse(set.isManualOn(), "it comes down switched off");
                    level.destroyBlock(parts.get(0), false);
                    for (final BlockPos cell : MainframeStructure.allPositions(origin, facing)) {
                        helper.assertTrue(level.getBlockState(cell).isAir(), "broken at a part, it goes whole: "
                                + cell);
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = SHIP, timeoutTicks = 800)
    public static void ship_aComputerAboardRunsAndJoinsByWiFiFromWhereItFlies(final GameTestHelper helper) {
        if (!sable()) {
            helper.assertFalse(SableIntegration.meeting(), "without Sable the ship is not met");
            helper.succeed();
            return;
        }
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(MAINFRAME);
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE);
        helper.setBlock(ACCESS_POINT, ComputingModule.STANDARD_ACCESS_POINT.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST)
                .setValue(AccessPointBlock.MOUNT, AccessPointMount.DESK));
        final List<BlockPos> ship = new ArrayList<>();
        for (int x = 3; x <= 5; x++) {
            for (int z = 8; z <= 10; z++) {
                helper.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
                ship.add(helper.absolutePos(new BlockPos(x, 2, z)));
            }
        }
        final PersonalComputerBlockEntity before = world.placeRunningPersonalComputer(ABOARD);
        before.getHardware().setStackInSlot(CARD_SLOT, new ItemStack(ComputingModule.WIFI_CARD_STANDARD.get()));
        ship.add(helper.absolutePos(ABOARD));
        WithSable.hold(helper.getLevel());
        WithSable.assemble(helper.getLevel(), helper.absolutePos(new BlockPos(4, 2, 9)), ship);
        helper.assertTrue(helper.getBlockState(ABOARD).isAir(), "Sable took the ship and the computer aboard");
        final PersonalComputerBlockEntity[] aboard = new PersonalComputerBlockEntity[1];
        helper.startSequence()
                .thenWaitUntil(() -> {
                    aboard[0] = aboard(helper);
                    helper.assertTrue(aboard[0] != null, "the computer stands on the sub-level");
                })
                .thenWaitUntil(() -> helper.assertTrue(aboard[0].isManualOn() && !aboard[0].needsPost()
                        && !aboard[0].booting(), "it goes on running aboard: its system comes up"))
                .thenWaitUntil(() -> helper.assertTrue(mainframe.networkUuid() != null
                        && mainframe.networkUuid().equals(aboard[0].networkUuid())
                        && aboard[0].wirelessLink(helper.getLevel()) != null,
                        "and joins the ground's network by Wi-Fi, reached from where the ship stands"))
                .thenExecute(() -> {
                    WithSable.removeAll(helper.getLevel(), helper.getBounds().inflate(2));
                    WithSable.release(helper.getLevel());
                })
                .thenSucceed();
    }

    private static boolean create() {
        return "present".equals(System.getProperty("jsc.gametests.create"));
    }

    private static boolean sable() {
        return "present".equals(System.getProperty("jsc.gametests.sable"));
    }

    /* The computer aboard the one sub-level of the arena, where its plot keeps it; null until it is met. */
    @Nullable
    private static PersonalComputerBlockEntity aboard(final GameTestHelper helper) {
        final List<ForeignMover> near = ForeignMovers.near(helper.getLevel(), helper.getBounds().inflate(2));
        if (near.size() != 1) {
            return null;
        }
        final Box blocks = near.get(0).blocks();
        for (final BlockPos at : BlockPos.betweenClosed(blocks.minX(), blocks.minY(), blocks.minZ(), blocks.maxX(),
                blocks.maxY(), blocks.maxZ())) {
            if (helper.getLevel().getBlockEntity(at) instanceof PersonalComputerBlockEntity computer) {
                return computer;
            }
        }
        return null;
    }

    /* What reaches Create's classes, loaded only when Create is present. */
    private static final class WithCreate {

        private static final ResourceLocation BEARING = ResourceLocation.fromNamespaceAndPath("create",
                "mechanical_bearing");
        private static final ResourceLocation MOTOR = ResourceLocation.fromNamespaceAndPath("create",
                "creative_motor");

        private WithCreate() {
        }

        static boolean movable(final Level level, final BlockPos pos) {
            return BlockMovementChecks.isMovementAllowed(level.getBlockState(pos), level, pos);
        }

        /* Whether Create takes the block beside {@code pos}, {@code towards}, with it. */
        static boolean attached(final Level level, final BlockPos pos, final Direction towards) {
            return BlockMovementChecks.isBlockAttachedTowards(level.getBlockState(pos), level, pos, towards);
        }

        /* A mechanical bearing at {@code at}, facing up. */
        static void bearing(final GameTestHelper helper, final BlockPos at) {
            helper.setBlock(at, BuiltInRegistries.BLOCK.get(BEARING).defaultBlockState()
                    .setValue(BlockStateProperties.FACING, Direction.UP));
        }

        /* A creative motor at {@code at}, turning what is above it. */
        static void motor(final GameTestHelper helper, final BlockPos at) {
            helper.setBlock(at, BuiltInRegistries.BLOCK.get(MOTOR).defaultBlockState()
                    .setValue(BlockStateProperties.FACING, Direction.UP));
        }

        /* Whether the bearing at {@code at} turns a contraption. */
        static boolean isContraption(final GameTestHelper helper, final BlockPos at) {
            final MechanicalBearingBlockEntity bearing = helper.getBlockEntity(at);
            final Entity moved = bearing.getMovedContraption();
            return moved != null;
        }
    }

    /* What reaches Sable's classes, loaded only when Sable is present. */
    private static final class WithSable {

        private WithSable() {
        }

        static void hold(final ServerLevel level) {
            SubLevelPhysicsSystem.require(level).setPaused(true);
        }

        static void release(final ServerLevel level) {
            SubLevelPhysicsSystem.require(level).setPaused(false);
        }

        /* Makes {@code blocks} a sub-level, as Sable's own assembly does about {@code anchor}. */
        static void assemble(final ServerLevel level, final BlockPos anchor, final List<BlockPos> blocks) {
            final BoundingBox3i box = new BoundingBox3i(anchor, anchor);
            for (final BlockPos at : blocks) {
                box.expandTo(at.getX(), at.getY(), at.getZ(), box);
            }
            SubLevelAssemblyHelper.assembleBlocks(level, anchor, blocks, box);
        }

        /* Takes away every sub-level in {@code box}, so the next test finds none. */
        static void removeAll(final ServerLevel level, final AABB box) {
            final List<SubLevel> found = new ArrayList<>();
            for (final SubLevelAccess sub : SableCompanion.INSTANCE.getAllIntersecting(level, new BoundingBox3d(box))) {
                if (sub instanceof SubLevel own) {
                    found.add(own);
                }
            }
            for (final SubLevel own : found) {
                SubLevelContainer.getContainer(level).removeSubLevel(own, SubLevelRemovalReason.REMOVED);
            }
        }
    }
}
