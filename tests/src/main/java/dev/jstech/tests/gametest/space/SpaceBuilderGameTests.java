/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.jstechseries.builder.BuildJobs;
import dev.jstech.core.jstechseries.builder.BuilderPlan;
import dev.jstech.core.jstechseries.builder.BuilderState;
import dev.jstech.core.jstechseries.projector.ProjectorSettings;
import dev.jstech.core.jstechseries.projector.ProjectorView;
import dev.jstech.core.jstechseries.projector.StructureProjector;
import dev.jstech.core.jstechseries.projector.StructureProjectorBlockEntity;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.multiblock.BuildBalance;
import dev.jstech.core.multiblock.BuildOrder;
import dev.jstech.core.text.Text;
import dev.jstech.space.crawler.CrawlerBlocks;
import dev.jstech.space.crawler.CrawlerEngineBlockEntity;
import dev.jstech.space.crawler.CrawlerLayout;
import dev.jstech.space.depot.DepotBlocks;
import dev.jstech.space.depot.DepotLayout;
import dev.jstech.space.depot.TransferPumpBlockEntity;
import dev.jstech.space.landing.ShipLandingBlocks;
import dev.jstech.space.launcher.LauncherBlocks;
import dev.jstech.space.launcher.LauncherDeckBlockEntity;
import dev.jstech.space.launcher.LauncherLayout;
import dev.jstech.space.pad.LaunchPadStructure;
import dev.jstech.space.pad.LaunchPads;
import dev.jstech.space.pad.PadBlocks;
import dev.jstech.space.platform.LandingPlatform;
import dev.jstech.tests.JsTests;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Every multiblock of J's Space, shown by a Structure Projector and built by the Multiblock Builder from it, forms: the
 * crawler-transporter, either way it is turned; the propellant depot at the approved one's size; both landing
 * platforms; the ship landing pad; and the launch pad, its trench dug and lined, whose deflectors a crawler coming up
 * its lanes sets a launcher's opening over, so the trench protects the rocket on it. Each test turns the build's pace
 * up while it runs, so each runs in a batch of its own.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SpaceBuilderGameTests {

    private static final String ARENA = "space_build";
    private static final String PAD_ARENA = "launch_pad";
    /* The top of the earth laid over the arena's floor, deep enough for the launch pad's trench under it. */
    private static final int EARTH_TOP = 8;
    /* The projector, facing south, what it shows north of it; the earth's reach; the same in the pad's arena. */
    private static final BlockPos PROJECTOR = new BlockPos(20, EARTH_TOP + 1, 34);
    private static final int WIDE = 41;
    private static final int DEEP = 36;
    private static final BlockPos PAD_PROJECTOR = new BlockPos(30, EARTH_TOP + 1, 34);
    private static final int PAD_WIDE = 51;
    /* How fast the builder lays blocks in these tests, and how long a build may take. */
    private static final int PACE = 1_000;
    private static final int BUILD_TIME = 1_200;
    private static final int PAD_TIME = 3_600;
    /* How many ticks before a build test runs out a build that never formed is let go of. */
    private static final int LET_GO_BEFORE = 5;
    /* How many blocks of the crawlerway lie before the pad's lanes, the crawler standing on them at first. */
    private static final int RUN_UP = 11;
    /*
     * The launcher the crawler carries up to the pad, as one rolled out of the hangar stands on it: its tower behind,
     * its rocket's axis two blocks ahead of the engine, its deck six blocks past the axis and eleven before it, toward
     * and past the tower, and six across each way; its opening a block round the axis; its hold-down arm, measuring
     * it, on its top.
     */
    private static final int AXIS_AHEAD = 2;
    private static final int DECK_BACK = 6;
    private static final int DECK_FRONT = 11;
    private static final int DECK_ACROSS = 6;
    private static final int OPENING = 1;
    private static final BlockPos ARM_FROM_ENGINE = new BlockPos(-2, 1 + LauncherLayout.DECK, -2);

    private SpaceBuilderGameTests() {
    }

    @GameTest(template = ARENA, batch = "jsspace_build_crawler", timeoutTicks = BUILD_TIME)
    public static void builder_buildsTheCrawlerAProjectorShowsAndItForms(final GameTestHelper helper) {
        buildsAndForms(helper, CrawlerBlocks.ID, 0, "crawler-0", entity -> entity instanceof CrawlerEngineBlockEntity
                engine && engine.along().equals(Optional.of(Direction.EAST)));
    }

    @GameTest(template = ARENA, batch = "jsspace_build_crawler_turned", timeoutTicks = BUILD_TIME)
    public static void builder_buildsTheCrawlerAProjectorShowsTurnedAndItFormsAlongItsLength(
            final GameTestHelper helper) {
        buildsAndForms(helper, CrawlerBlocks.ID, 1, "crawler-1", entity -> entity instanceof CrawlerEngineBlockEntity
                engine && engine.along().equals(Optional.of(Direction.SOUTH)));
    }

    @GameTest(template = ARENA, batch = "jsspace_build_depot", timeoutTicks = BUILD_TIME)
    public static void builder_buildsThePropellantDepotAtTheApprovedSizeAndItForms(final GameTestHelper helper) {
        buildsAndForms(helper, DepotBlocks.ID, 0, "depot", entity -> entity instanceof TransferPumpBlockEntity pump
                && pump.depot().map(depot -> depot.edge() == DepotLayout.LARGEST).orElse(false));
    }

    @GameTest(template = ARENA, batch = "jsspace_build_platform_small", timeoutTicks = BUILD_TIME)
    public static void builder_buildsTheSmallLandingPlatformAndItForms(final GameTestHelper helper) {
        buildsAndForms(helper, LandingPlatform.SMALL_ID, 0, "platform-9", SpaceBuilderGameTests::formed);
    }

    @GameTest(template = ARENA, batch = "jsspace_build_platform_large", timeoutTicks = BUILD_TIME)
    public static void builder_buildsTheLargeLandingPlatformAndItForms(final GameTestHelper helper) {
        buildsAndForms(helper, LandingPlatform.LARGE_ID, 0, "platform-15", SpaceBuilderGameTests::formed);
    }

    @GameTest(template = ARENA, batch = "jsspace_build_ship_pad", timeoutTicks = BUILD_TIME)
    public static void builder_buildsTheShipLandingPadAndItForms(final GameTestHelper helper) {
        buildsAndForms(helper, ShipLandingBlocks.PAD_ID, 0, "ship-pad", SpaceBuilderGameTests::formed);
    }

    /*
     * The launch pad built whole, its trench dug and lined; a crawler then comes up its lanes from the crawlerway
     * before it with a launcher on it, stops where its lanes end and sets the launcher down, and goes back: the
     * launcher's opening stands over the deflectors, and the trench protects the rocket on it.
     */
    @GameTest(template = PAD_ARENA, batch = "jsspace_build_launch_pad", timeoutTicks = PAD_TIME)
    public static void builder_buildsTheLaunchPadAndACrawlerSetsALauncherDownOverItsTrench(
            final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Build build = build(helper, LaunchPadStructure.ID, 0, PAD_PROJECTOR, PAD_WIDE, "launch-pad", PAD_TIME);
        final BlockPos middle = build.terminal().offset(-LaunchPadStructure.PORT_X, 0, -LaunchPadStructure.PORT_Z);
        final BlockPos engine = middle.offset(LaunchPadStructure.LANES_FROM - RUN_UP + CrawlerLayout.HALF_LENGTH,
                CrawlerLayout.TRACKS, 0);
        final int steps = LaunchPadStructure.CRAWLER_STOPS - (engine.getX() - middle.getX());
        final BlockPos arm = engine.offset(ARM_FROM_ENGINE);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(BuildJobs.building(build.player()).isEmpty(),
                        "the builder builds the pad"))
                .thenExecute(() -> {
                    build.finish(helper);
                    helper.assertTrue(level.getBlockState(middle.below(LaunchPadStructure.TRENCH_DEPTH))
                            .is(PadBlocks.FLAME_DEFLECTOR.get()), "a deflector lies under the pad's middle");
                    helper.assertTrue(level.getBlockState(middle.below()).isAir(), "its trench open above it");
                    runUp(level, middle);
                    for (final CrawlerLayout.Spot spot : CrawlerLayout.spots(engine, Direction.EAST)) {
                        level.setBlockAndUpdate(spot.at(), spot.kind().block().defaultBlockState());
                    }
                    launcher(level, engine, arm);
                })
                .thenWaitUntil(() -> helper.assertTrue(crawler(level, engine).along().isPresent(),
                        "the crawler forms"))
                .thenExecute(() -> {
                    final Optional<Text> why = crawler(level, engine).start();
                    helper.assertTrue(why.isEmpty(), "with the launcher over it, it sets off: " + why);
                })
                .thenWaitUntil(() -> helper.assertTrue(deck(level, arm.east(steps)).isPresent(),
                        "it sets the launcher down where the pad's lanes end"))
                .thenWaitUntil(() -> helper.assertTrue(level.getBlockEntity(engine) instanceof CrawlerEngineBlockEntity
                        back && back.phase() == CrawlerEngineBlockEntity.Phase.IDLE, "and goes back alone"))
                .thenExecute(() -> {
                    final LauncherDeckBlockEntity.Deck deck = deck(level, arm.east(steps)).orElseThrow();
                    final BlockPos axis = arm.east(steps).offset(deck.axis());
                    helper.assertTrue(LaunchPads.protection(level, axis, deck.side()).trench(),
                            "the launcher's opening stands over the deflectors, and the trench protects its rocket: "
                                    + axis.subtract(middle));
                })
                .thenSucceed();
    }

    /*
     * Builds {@code id} turned {@code turn} quarters from a projector, as {@link #build} does, and waits until
     * {@code formed} says its terminal's entity is formed and the projector sees it whole.
     */
    private static void buildsAndForms(final GameTestHelper helper, final ResourceLocation id, final int turn,
                                       final String who, final Predicate<BlockEntity> formed) {
        final Build build = build(helper, id, turn, PROJECTOR, WIDE, who, BUILD_TIME);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(formed.test(helper.getLevel().getBlockEntity(build.terminal())),
                        "the " + id.getPath() + " the builder built forms"))
                .thenExecute(() -> build.finish(helper))
                .thenSucceed();
    }

    /*
     * Lays a ground of earth over the arena's floor {@code wide} blocks across, a projector on it at {@code at} facing
     * south and showing {@code id} turned {@code turn} quarters, and a player in creative, {@code who}, beside it with
     * the Multiblock Builder aimed at the projector, building at the tests' pace: its terminal is the last block it
     * places. The test runs out at {@code runsOut} ticks.
     */
    private static Build build(final GameTestHelper helper, final ResourceLocation id, final int turn,
                               final BlockPos at, final int wide, final String who, final int runsOut) {
        final ServerLevel level = helper.getLevel();
        for (int x = 0; x < wide; x++) {
            for (int z = 0; z < DEEP; z++) {
                for (int y = 1; y <= EARTH_TOP; y++) {
                    level.setBlock(helper.absolutePos(new BlockPos(x, y, z)), (y == EARTH_TOP ? Blocks.GRASS_BLOCK
                            : Blocks.DIRT).defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        helper.setBlock(at, StructureProjector.BLOCK.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        if (!(helper.getBlockEntity(at) instanceof StructureProjectorBlockEntity projector)) {
            throw new IllegalStateException("no projector where the test put it");
        }
        projector.apply(new ProjectorSettings(Optional.of(id), turn, false, 0, false, true));
        final BlockPos terminal = projector.terminalOf(projector.preview().orElseThrow());
        final FakePlayer player = FakePlayerFactory.get(level, new GameProfile(UUID.nameUUIDFromBytes(
                ("space-builder-" + who).getBytes(StandardCharsets.UTF_8)), who));
        player.getAbilities().instabuild = true;
        final BlockPos stand = helper.absolutePos(at.south());
        player.setPos(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
        final BuilderPlan plan = BuilderPlan.of(level, helper.absolutePos(at), player, BuilderState.DEFAULT)
                .orElseThrow();
        final BuildOrder.Want last = plan.wants().getLast();
        helper.assertTrue(new BlockPos(last.x(), last.y(), last.z()).equals(terminal),
                "the " + id.getPath() + "'s terminal is the last block the builder places");
        final int pace = BuildBalance.blocksPerSecond();
        BuildBalance.setBlocksPerSecond(PACE);
        BuildJobs.start(player, helper.absolutePos(at), BuilderState.DEFAULT);
        // Should it never form, the pace and the job are let go just before the test runs out, not left to the next.
        helper.runAfterDelay(runsOut - LET_GO_BEFORE, () -> {
            BuildBalance.setBlocksPerSecond(pace);
            BuildJobs.stop(player);
        });
        return new Build(terminal, player, projector, pace);
    }

    /* The crawlerway before the pad: its two lanes of gravel, west of the pad's own, under the crawler's tracks. */
    private static void runUp(final ServerLevel level, final BlockPos middle) {
        for (int x = LaunchPadStructure.LANES_FROM - RUN_UP; x < LaunchPadStructure.LANES_FROM; x++) {
            for (int across = LaunchPadStructure.LANE_INSIDE; across <= LaunchPadStructure.LANE_OUTSIDE; across++) {
                for (final int side : new int[] {-1, 1}) {
                    level.setBlockAndUpdate(middle.offset(x, -1, across * side), Blocks.GRAVEL.defaultBlockState());
                }
            }
        }
    }

    /*
     * The launcher over the crawler round {@code engine}, as one rolled out of the hangar: its deck three blocks thick
     * round its opening, on two rows of pedestals the crawler stands between, its hold-down arm at {@code arm}
     * measuring it, its tower to the west.
     */
    private static void launcher(final ServerLevel level, final BlockPos engine, final BlockPos arm) {
        final BlockPos axis = engine.offset(AXIS_AHEAD, 0, 0);
        for (int x = -DECK_FRONT; x <= DECK_BACK; x++) {
            for (int z = -DECK_ACROSS; z <= DECK_ACROSS; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) <= OPENING) {
                    continue;
                }
                for (int y = 1; y <= LauncherLayout.DECK; y++) {
                    level.setBlockAndUpdate(axis.offset(x, y, z), LauncherBlocks.LAUNCHER_DECK.get()
                            .defaultBlockState());
                }
            }
        }
        for (final int x : new int[] {-DECK_FRONT + 1, 0, DECK_BACK - 1}) {
            for (final int z : new int[] {-DECK_ACROSS, DECK_ACROSS}) {
                for (int y = 1; y <= LauncherLayout.PEDESTAL; y++) {
                    level.setBlockAndUpdate(axis.offset(x, y - LauncherLayout.PEDESTAL, z), LauncherBlocks.PAD_PEDESTAL
                            .get().defaultBlockState());
                }
            }
        }
        level.setBlockAndUpdate(arm, LauncherBlocks.HOLD_DOWN_ARM.get().defaultBlockState());
        if (level.getBlockEntity(arm) instanceof LauncherDeckBlockEntity measuring) {
            measuring.deck(new LauncherDeckBlockEntity.Deck(Direction.WEST, OPENING, axis.atY(arm.getY()).subtract(arm),
                    DECK_BACK, DECK_FRONT, DECK_ACROSS));
        }
    }

    /* The deck the hold-down arm at {@code arm} measures, if one stands there and measures it. */
    private static Optional<LauncherDeckBlockEntity.Deck> deck(final ServerLevel level, final BlockPos arm) {
        return level.getBlockEntity(arm) instanceof LauncherDeckBlockEntity measuring ? measuring.deck()
                : Optional.empty();
    }

    /* Whether the machine whose terminal's entity is {@code entity} stands formed. */
    private static boolean formed(final BlockEntity entity) {
        return entity instanceof MachineBlockEntity machine && machine.structure() != null
                && machine.structure().formed();
    }

    private static CrawlerEngineBlockEntity crawler(final ServerLevel level, final BlockPos at) {
        if (level.getBlockEntity(at) instanceof CrawlerEngineBlockEntity engine) {
            return engine;
        }
        throw new IllegalStateException("No crawler engine at " + at);
    }

    /* A build under way: what it builds, its terminal; who builds it; the projector it builds from; the pace before. */
    private record Build(BlockPos terminal, FakePlayer player, StructureProjectorBlockEntity projector, int pace) {

        /* The builder done with it and the pace given back; the projector sees it whole, nothing missing or wrong. */
        void finish(final GameTestHelper helper) {
            BuildBalance.setBlocksPerSecond(this.pace);
            helper.assertTrue(BuildJobs.building(this.player).isEmpty(), "the builder is done with it");
            final ProjectorView seen = ProjectorView.of(helper.getLevel(), this.projector).orElseThrow();
            helper.assertTrue(seen.missing() == 0 && seen.wrong() == 0 && seen.toClear() == 0,
                    "the projector sees it whole: " + seen.missing() + " missing, " + seen.wrong() + " wrong, "
                            + seen.toClear() + " to clear");
        }
    }
}
