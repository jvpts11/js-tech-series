/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.hangar.HangarBlocks;
import dev.jstech.space.hangar.HangarTerminalBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.launcher.ArmBlockEntity;
import dev.jstech.space.launcher.CraneBlockEntity;
import dev.jstech.space.launcher.LauncherBlocks;
import dev.jstech.space.launcher.LauncherDeckBlockEntity;
import dev.jstech.space.launcher.LauncherLayout;
import dev.jstech.space.launcher.LauncherPieceBlock;
import dev.jstech.space.launcher.TowerFrameBlock;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.tests.JsTests;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The mobile launcher a rocket stands on in its hangar: laid by the hangar's plan under the rocket where the bay has
 * room for it, sized by the rocket, its deck on six pedestals with the exhaust opening under the engines and three
 * masts round it, its length pointing at the bay's door and its tower past the rocket's top at the far end, its arms
 * out to the tanks and the hatch; its parts forming as their last blocks go down, each piece taking its formed shape
 * from the pieces round it and one hold-down arm measuring the deck to draw it; and left out of a bay too small.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class LauncherGameTests {

    private static final String HANGAR = "hangar";
    private static final String SMALL = "tall";
    private static final String BATCH = "jsspace_launcher";
    /* The large hangar's bay, from (2, 2, 2) to (26, 40, 26), its hoist at 39 over the middle; the small one's. */
    private static final int LOW = 2;
    private static final int HIGH = 26;
    private static final int TOP = 40;
    private static final int SMALL_HIGH = 8;
    private static final int SMALL_TOP = 13;
    /* The first bay of a corner of the tower, as TowerFrameBlock numbers them. */
    private static final int CORNER_SLOT = 48;
    /* How far the test rocket's foot, an engine three blocks across, reaches each side of its axis. */
    private static final int FOOT = 1;

    private LauncherGameTests() {
    }

    @GameTest(template = HANGAR, batch = BATCH, timeoutTicks = 200)
    public static void launcher_isLaidUnderTheRocketSizedByIt(final GameTestHelper helper) {
        final HangarTerminalBlockEntity terminal = hangar(helper, HIGH, TOP);
        helper.assertTrue(terminal.bay().isPresent(), "the large hangar forms: " + terminal.reason().english());
        terminal.design(new RocketDesign("LAUNCHER", rocket()));
        helper.assertTrue(terminal.confirm(), "a ready design is sent to the bay");
        final Direction side = terminal.launcherSide().orElseThrow();
        helper.assertTrue(side == Direction.EAST, "the deck points at the bay's door in its west wall, the tower at"
                + " its far end, so the crawler comes in under it: the tower to the " + side);
        final BlockPos pad = terminal.bay().orElseThrow().pad();
        helper.assertTrue(terminal.planAt().orElseThrow().equals(pad.above(LauncherLayout.RAISE)),
                "the rocket stands on the launcher's deck on its pedestals, six blocks over the floor");
        final PlannedBuild plan = terminal.build().orElseThrow();
        final List<PlanSpot> spots = plan.plan().spots();
        final List<PlanSpot> rocket = spots.stream().filter(spot -> spot.part() < rocket().sections().size())
                .toList();
        helper.assertTrue(plan.plan().parts() > rocket().sections().size() + 2,
                "the deck, the tower and at least one arm are parts of the plan: " + plan.plan().parts());
        for (final var piece : LauncherBlocks.all()) {
            helper.assertTrue(spots.stream().anyMatch(spot -> spot.block().equals(piece.getId())),
                    "the launcher has its " + piece.getId().getPath());
        }
        // The opening under the engines is as wide as the rocket's foot, through the deck, its lining round it.
        final Set<BlockPos> laid = spots.stream().map(PlanSpot::at).collect(Collectors.toSet());
        final Direction across = side.getClockWise();
        for (int y = -LauncherLayout.DECK; y < 0; y++) {
            for (int k = -FOOT - 1; k <= FOOT + 1; k++) {
                final BlockPos at = new BlockPos(0, y, 0).relative(across, k);
                helper.assertTrue(laid.contains(at) == (Math.abs(k) > FOOT), "the opening reaches the rocket's"
                        + " foot and no further: " + at);
            }
        }
        // Six pedestals three blocks tall, in two rows the crawler comes between; three masts a third of a turn apart.
        final List<PlanSpot> pedestals = only(spots, LauncherBlocks.PAD_PEDESTAL.getId());
        helper.assertTrue(pedestals.size() == 6 * LauncherLayout.PEDESTAL && pedestals.stream().allMatch(spot ->
                spot.at().getY() < -LauncherLayout.DECK), "the deck stands on six pedestals under it: "
                + pedestals.size());
        final List<PlanSpot> masts = only(spots, LauncherBlocks.TAIL_SERVICE_MAST.getId());
        for (final double degrees : LauncherLayout.MAST_ANGLES) {
            final int[] mast = LauncherLayout.mast(FOOT, degrees);
            final BlockPos foot = BlockPos.ZERO.relative(side, mast[0]).relative(across, mast[1]);
            helper.assertTrue(masts.stream().anyMatch(spot -> spot.at().equals(foot)), "a mast stands at "
                    + degrees + " degrees round the opening: " + foot);
        }
        final int rocketTop = rocket.stream().mapToInt(spot -> spot.at().getY()).max().orElseThrow();
        final int towerTop = top(spots, LauncherBlocks.TOWER_FRAME.getId());
        helper.assertTrue(towerTop > rocketTop, "the tower stands past the rocket's top: " + towerTop + " over "
                + rocketTop);
        final BlockPos hatch = rocket.stream().filter(spot -> spot.block().equals(RocketKitBlocks.CAPSULE_HATCH
                .getId())).findFirst().orElseThrow().at();
        final List<PlanSpot> crew = only(spots, LauncherBlocks.CREW_ACCESS_ARM.getId());
        helper.assertTrue(!crew.isEmpty() && crew.stream().allMatch(spot -> spot.at().getY() == hatch.getY()),
                "the crew access arm runs out at the hatch's height");
        helper.assertTrue(!only(spots, LauncherBlocks.SWING_ARM.getId()).isEmpty(), "a swing arm goes to the tank");
        terminal.undo();
        helper.succeed();
    }

    @GameTest(template = HANGAR, batch = BATCH, timeoutTicks = 200)
    public static void launcher_formsAndEachPieceTakesItsShape(final GameTestHelper helper) {
        final HangarTerminalBlockEntity terminal = hangar(helper, HIGH, TOP);
        terminal.design(new RocketDesign("FORM", rocket()));
        helper.assertTrue(terminal.confirm(), "a ready design is sent to the bay");
        final Direction side = terminal.launcherSide().orElseThrow();
        final PlannedBuild plan = terminal.build().orElseThrow();
        final int first = rocket().sections().size();
        final List<PlanSpot> launcher = plan.plan().spots().stream().filter(spot -> spot.part() >= first).toList();
        for (final PlanSpot spot : launcher) {
            helper.getLevel().setBlockAndUpdate(plan.where(spot), BuiltInRegistries.BLOCK.get(spot.block())
                    .defaultBlockState());
        }
        for (int part = first; part < plan.plan().parts(); part++) {
            helper.assertTrue(plan.formed(part), "the launcher's part " + part + " formed with its last block");
        }
        helper.assertTrue(plan.missing() > 0 && !plan.formed(0), "the rocket's parts wait for their blocks");
        final ToIntFunction<PlanSpot> out = spot -> spot.at().getX() * side.getStepX()
                + spot.at().getZ() * side.getStepZ();
        final BlockState deck = at(helper, plan, first(launcher, LauncherBlocks.LAUNCHER_DECK.getId()));
        helper.assertTrue(deck.getValue(LauncherPieceBlock.FORMED), "the deck takes its formed shape");
        // The crane's boom is the row of frames on the house, its tip out over the rocket drawing the whole crane.
        final int boomHeight = top(launcher, LauncherBlocks.TOWER_FRAME.getId());
        final List<PlanSpot> boomRow = only(launcher, LauncherBlocks.TOWER_FRAME.getId()).stream()
                .filter(spot -> spot.at().getY() == boomHeight).toList();
        final PlanSpot boom = boomRow.stream().min(Comparator.comparingInt(out)).orElseThrow();
        helper.assertTrue(boomRow.stream().allMatch(spot -> role(helper, plan, spot) == LauncherPieceBlock.Role.END
                        && at(helper, plan, spot).getValue(HorizontalDirectionalBlock.FACING) == side.getOpposite())
                        && TowerFrameBlock.tip(at(helper, plan, boom)),
                "the boom runs from past the tower's back out over the rocket, its tip the far end: "
                        + at(helper, plan, boom) + " at " + boom.at() + ", the tower to the " + side);
        final List<PlanSpot> houseRow = only(launcher, LauncherBlocks.TOWER_FRAME.getId()).stream()
                .filter(spot -> spot.at().getY() == boomHeight - 1).toList();
        helper.assertTrue(houseRow.size() == 16 && houseRow.stream().allMatch(spot -> role(helper, plan, spot)
                == LauncherPieceBlock.Role.TOP), "the tower's top is the crane's house, under the boom too");
        final List<PlanSpot> under = only(launcher, LauncherBlocks.TOWER_FRAME.getId()).stream()
                .filter(spot -> spot.at().getY() == boomHeight - 2).toList();
        helper.assertTrue(under.stream().allMatch(spot -> role(helper, plan, spot) == LauncherPieceBlock.Role.BODY),
                "under the house the truss goes on");
        final PlanSpot house = houseRow.stream().max(Comparator.comparingInt(out)).orElseThrow();
        final int tipAt = out.applyAsInt(boom);
        final CraneBlockEntity.Crane crane = helper.getLevel().getBlockEntity(plan.where(boom),
                LauncherBlocks.CRANE_ENTITY.get()).flatMap(CraneBlockEntity::crane).orElseThrow();
        helper.assertTrue(crane.tower() == side
                && crane.near() == houseRow.stream().mapToInt(out).min().orElseThrow() - tipAt
                && crane.far() == houseRow.stream().mapToInt(out).max().orElseThrow() - tipAt
                && crane.back() == boomRow.stream().mapToInt(out).max().orElseThrow() - tipAt,
                "the tip measures the crane as it was laid: " + crane);
        // A layer of the tower is one great truss: its four corners are the columns, the rest stretches of its faces.
        final int layer = LauncherLayout.DECK + 1;
        final List<PlanSpot> ring = only(launcher, LauncherBlocks.TOWER_FRAME.getId()).stream()
                .filter(spot -> spot.at().getY() == layer).toList();
        final long corners = ring.stream().filter(spot -> at(helper, plan, spot).getValue(TowerFrameBlock.BAY)
                / CORNER_SLOT >= 1).count();
        helper.assertTrue(ring.size() == 16 && corners == 4, "a layer of the tower has its four corner columns among"
                + " its sixteen frames: " + corners + " of " + ring.size());
        final PlanSpot hatchEnd = only(launcher, LauncherBlocks.CREW_ACCESS_ARM.getId()).stream()
                .min(Comparator.comparingInt(out)).orElseThrow();
        helper.assertTrue(role(helper, plan, hatchEnd) == LauncherPieceBlock.Role.END
                        && at(helper, plan, hatchEnd).getValue(HorizontalDirectionalBlock.FACING) == side.getOpposite(),
                "the white room stands at the crew access arm's end, by the hatch");
        final PlanSpot plug = only(launcher, LauncherBlocks.SWING_ARM.getId()).stream()
                .min(Comparator.comparingInt(out)).orElseThrow();
        helper.assertTrue(role(helper, plan, plug) == LauncherPieceBlock.Role.END, "a swing arm ends in its plug");
        // Each arm's end at the rocket measures its arm, to draw it whole.
        for (final PlanSpot end : List.of(plug, hatchEnd)) {
            final int lengths = (int) launcher.stream().filter(spot -> spot.block().equals(end.block())
                    && spot.at().getY() == end.at().getY()).count();
            final ArmBlockEntity.Arm arm = helper.getLevel().getBlockEntity(plan.where(end),
                    LauncherBlocks.ARM_ENTITY.get()).flatMap(ArmBlockEntity::arm).orElseThrow();
            helper.assertTrue(arm.out() == side.getOpposite() && arm.length() == lengths, "an arm's end measures "
                    + "it, out from the tower and " + lengths + " blocks long: " + arm);
        }
        final List<PlanSpot> lift = only(launcher, LauncherBlocks.TOWER_ELEVATOR.getId());
        final PlanSpot car = lift.stream().min(Comparator.comparingInt(spot -> spot.at().getY())).orElseThrow();
        helper.assertTrue(role(helper, plan, car) == LauncherPieceBlock.Role.END
                        && at(helper, plan, car).is(BlockTags.CLIMBABLE),
                "the elevator's car stands at the shaft's foot, its rails climbed");
        final PlanSpot mastTop = only(launcher, LauncherBlocks.TAIL_SERVICE_MAST.getId()).stream()
                .max(Comparator.comparingInt(spot -> spot.at().getY())).orElseThrow();
        helper.assertTrue(role(helper, plan, mastTop) == LauncherPieceBlock.Role.TOP, "a mast carries its hood on top");
        // The hold-down arm behind the opening and left of the tower measures the deck, to draw it whole.
        final ToIntFunction<PlanSpot> across = spot -> spot.at().getX() * side.getClockWise().getStepX()
                + spot.at().getZ() * side.getClockWise().getStepZ();
        final List<PlanSpot> deckSpots = launcher.stream().filter(spot -> spot.block().equals(LauncherBlocks
                .LAUNCHER_DECK.getId())).toList();
        for (final PlanSpot arm : only(launcher, LauncherBlocks.HOLD_DOWN_ARM.getId())) {
            final LauncherDeckBlockEntity entity = helper.getLevel().getBlockEntity(plan.where(arm),
                    LauncherBlocks.HOLD_DOWN_ENTITY.get()).orElseThrow();
            final boolean draws = out.applyAsInt(arm) < 0 && across.applyAsInt(arm) < 0;
            helper.assertTrue(entity.deck().isPresent() == draws, "only the arm behind and left of the opening draws"
                    + " the deck: " + arm.at() + " " + entity.deck());
            entity.deck().ifPresent(measured -> helper.assertTrue(measured.side() == side && measured.open() == FOOT
                    && measured.back() == -deckSpots.stream().mapToInt(out).min().orElseThrow()
                    && measured.front() == deckSpots.stream().mapToInt(out).max().orElseThrow()
                    && measured.across() == deckSpots.stream().mapToInt(across).max().orElseThrow()
                    && plan.where(arm).offset(measured.axis()).equals(plan.origin()), "the deck is measured as it"
                    + " was laid: " + measured + " from " + plan.where(arm) + ", the rocket's foot at "
                    + plan.origin()));
        }
        helper.getLevel().setBlockAndUpdate(plan.where(boom), Blocks.AIR.defaultBlockState());
        helper.assertTrue(!plan.formed(first + 1) && !at(helper, plan, house).getValue(LauncherPieceBlock.FORMED),
                "a block of the tower taken, the tower is a kit again");
        terminal.undo();
        helper.succeed();
    }

    @GameTest(template = SMALL, batch = BATCH, timeoutTicks = 200)
    public static void launcher_isLeftOutOfABayTooSmallForIt(final GameTestHelper helper) {
        final HangarTerminalBlockEntity terminal = hangar(helper, SMALL_HIGH, SMALL_TOP);
        terminal.design(new RocketDesign("FLOOR", rocket()));
        helper.assertTrue(terminal.confirm(), "a ready design is sent to the bay");
        helper.assertTrue(terminal.launcherSide().isEmpty() && terminal.planAt().orElseThrow()
                .equals(terminal.bay().orElseThrow().pad()), "with no room for a launcher, the rocket stands on the"
                + " floor");
        helper.assertTrue(terminal.build().orElseThrow().plan().parts() == rocket().sections().size(),
                "and the plan is the rocket's alone");
        terminal.undo();
        helper.succeed();
    }

    /* An engine, a tank and a capsule. */
    private static RocketStack rocket() {
        return RocketStack.of(part("sea_level_engine_3", 1), part("tank_3", 2), part("capsule_3", 1));
    }

    private static RocketStack.Part part(final String path, final int layers) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), layers);
    }

    private static List<PlanSpot> only(final List<PlanSpot> spots, final ResourceLocation block) {
        return spots.stream().filter(spot -> spot.block().equals(block)).toList();
    }

    private static PlanSpot first(final List<PlanSpot> spots, final ResourceLocation block) {
        return only(spots, block).getFirst();
    }

    private static int top(final List<PlanSpot> spots, final ResourceLocation block) {
        return only(spots, block).stream().mapToInt(spot -> spot.at().getY()).max().orElseThrow();
    }

    private static BlockState at(final GameTestHelper helper, final PlannedBuild plan, final PlanSpot spot) {
        return helper.getLevel().getBlockState(plan.where(spot));
    }

    private static LauncherPieceBlock.Role role(final GameTestHelper helper, final PlannedBuild plan,
                                                final PlanSpot spot) {
        return at(helper, plan, spot).getValue(LauncherPieceBlock.ROLE);
    }

    /*
     * A hangar of stone {@code high} wide and deep from (2, 2, 2) and {@code top} high, its terminal in the south wall
     * looking north, its crane, its door; looked at once.
     */
    private static HangarTerminalBlockEntity hangar(final GameTestHelper helper, final int high, final int top) {
        for (int x = LOW - 1; x <= high + 1; x++) {
            for (int z = LOW - 1; z <= high + 1; z++) {
                for (int y = LOW - 1; y <= top + 1; y++) {
                    final boolean shell = x < LOW || x > high || z < LOW || z > high || y < LOW || y > top;
                    helper.setBlock(new BlockPos(x, y, z), shell ? Blocks.STONE : Blocks.AIR);
                }
            }
        }
        final int middle = (LOW + high) / 2;
        final BlockPos terminal = new BlockPos(middle, 3, high + 1);
        helper.setBlock(terminal, HangarBlocks.TERMINAL.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        for (int x = LOW; x <= high; x++) {
            helper.setBlock(new BlockPos(x, top - 1, LOW), HangarBlocks.CRANE_RAIL.get());
            helper.setBlock(new BlockPos(x, top - 1, high), HangarBlocks.CRANE_RAIL.get());
        }
        for (int z = LOW; z <= high; z++) {
            helper.setBlock(new BlockPos(middle, top, z), HangarBlocks.CRANE_BRIDGE.get());
        }
        helper.setBlock(new BlockPos(middle, top - 1, middle), HangarBlocks.CRANE_HOIST.get());
        helper.setBlock(new BlockPos(LOW - 1, LOW, middle), HangarBlocks.BAY_DOOR.get());
        final HangarTerminalBlockEntity found = helper.getBlockEntity(terminal);
        found.look();
        return found;
    }
}
