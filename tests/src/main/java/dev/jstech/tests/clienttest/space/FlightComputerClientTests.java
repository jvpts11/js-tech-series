/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.region.Box;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.space.api.Propellants;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.client.flight.FlightComputerScreen;
import dev.jstech.space.client.flight.LandingMapsClient;
import dev.jstech.space.client.rocket.FlightComputerFace;
import dev.jstech.space.exploration.ReliefCoverage;
import dev.jstech.space.exploration.WorldCoverage;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.flight.FlightComputerMenu;
import dev.jstech.space.flight.FlightPage;
import dev.jstech.space.flight.StageChoice;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.hangar.RocketPlans;
import dev.jstech.space.launch.LaunchCount;
import dev.jstech.space.launch.RocketLaunch;
import dev.jstech.space.launcher.LauncherBlocks;
import dev.jstech.space.launcher.LauncherDeckBlockEntity;
import dev.jstech.space.orbit.OrbitObjects;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonPropellants;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.space.rocket.TankLoad;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.space.universe.SphereMap;
import dev.jstech.space.universe.StarSystems;
import dev.jstech.space.universe.SystemLayout;
import dev.jstech.space.universe.SystemSpaces;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The Flight Computer on a player's game: used, it opens its screen on the transfer page, the Sun drawn in its yellow
 * in the middle of the system; its landing key, clicked, opens the landing page, and the map of Mars comes and is
 * drawn where the page was dark, under the grey of the unknown; under a pass the team photographed, in Mars' own
 * colours, how much of Mars is known written over its small map. On its stages page, the rocket's stack is drawn with
 * its separation ring in amber, and its ZONE key sets the chosen stage to fall in a zone round the pad. On a launcher
 * it opens on its LAUNCH page, whose keys work the pad's count. In a wide orbit its transfer page offers the capture
 * first, and its landing page asks for the capture before any landing.
 */
public final class FlightComputerClientTests {

    private static final BlockPos COMPUTER = new BlockPos(0, 2, 2);
    private static final BlockPos EYE = new BlockPos(0, 2, 0);
    /* The Sun on the transfer page, the landing key's cap, and a spot of the landing map, in the face's pixels. */
    private static final int[] SUN = {FlightComputerFace.SX0 + 96, FlightComputerFace.SY0 + 86};
    private static final int[] LANDING_KEY = {FlightComputerFace.topKeyX(FlightPage.KEYS.indexOf(FlightPage.LANDING))
        + 12, 21};
    private static final int[] ON_MAP = {FlightComputerFace.SX0 + 70, FlightComputerFace.SY0 + 70};
    private static final int SPACE = 0x0A0F17;
    private static final int OPEN = 200;
    private static final int MAP_WAIT = 600;
    private static final int SETTLE = 10;
    private static final int CHANNEL = 0xFF;
    private static final int YELLOW = 180;
    private static final int APART = 40;
    /* The fail lamp's glass, clear of its word; how bright a channel of it may be while it is not lit. */
    private static final int[] FAIL_GLASS = {FlightComputerFace.SX0, 10};
    private static final int UNLIT = 45;
    /*
     * A rocket of two stages, its first with legs: twelve blocks tall, drawn four pixels a block from the floor of the
     * stack's schematic; its first stage's ring, five blocks up, and the ZONE key's cap, in the face's pixels.
     */
    private static final RocketStack STAGED = RocketStack.of(part("sea_level_engine_3"), part("landing_legs_3"),
            part("tank_3"), part("separation_ring_3"), part("vacuum_engine_3"), part("tank_3"), part("capsule_3"));
    private static final int[] RING = {FlightComputerFace.SX0 + 42, FlightComputerFace.SY1 - 34 - 18};
    private static final int[] ZONE_KEY = {FlightComputerFace.SX1 + 19, FlightComputerFace.SY0 + 24 + 30 + 5};
    private static final int AMBER_RED = 200;
    private static final int AMBER_GREEN = 140;
    /* Where the transfer page writes its status, bottom right, in the face's pixels, and the least amber seen there. */
    private static final int[] STATUS = {FlightComputerFace.SX1 - 90, FlightComputerFace.SY1 - 11,
        FlightComputerFace.SX1 - 8, FlightComputerFace.SY1 - 2};
    private static final int LEAST_AMBER = 6;
    /* How near red and blue the grey of the unknown is; how much redder than blue Mars' ground is. */
    private static final int GREY = 24;
    private static final int RUST = 30;
    /*
     * A box of the landing map, x0, y0, x1, y1 in the face's pixels, clear of its small map and its scale, and the
     * least share of it in Mars' red: Mars' ground lies in patches, its basalt as grey as the unknown, so the ground
     * the pass saw is told by how much of it is red, never by one point that may fall on basalt.
     */
    private static final int[] MAP_BOX = {FlightComputerFace.SX0 + 10, FlightComputerFace.SY0 + 24,
        FlightComputerFace.SX0 + 130, FlightComputerFace.SY0 + 104};
    private static final int LEAST_RUST_IN = 10;
    /* Where the landing page writes how much of the world is known, over its small map, and the least green there. */
    private static final int[] KNOWN_LABEL = {FlightComputerFace.SX1 - 104, FlightComputerFace.SY1 - 28 - 38 - 11,
        FlightComputerFace.SX1 - 54, FlightComputerFace.SY1 - 28 - 38 - 2};
    private static final int GREEN = 180;
    private static final int LEAST_GREEN = 6;
    /* A pass crossing the equator going north at 90 degrees east swings furthest south at the world's middle. */
    private static final double PASS_NODE = 90;
    /*
     * A launcher's measuring arm beside the computer, the rocket's axis meeting its deck under the computer; a full
     * small tank, in kilograms. The side keys' caps, x in the face's pixels; how long a count may take to reach a
     * second it is waited for, and its T-8.
     */
    private static final BlockPos LAUNCHER = new BlockPos(-3, 2, -1);
    private static final BlockPos TO_AXIS = new BlockPos(3, 0, 3);
    private static final double FULL_TANK = 5_800.0;
    private static final int LEFT_KEY = FlightComputerFace.SX0 - 19;
    private static final int RIGHT_KEY = FlightComputerFace.SX1 + 19;
    private static final int SECOND = LaunchCount.SECOND;
    private static final int TO_COUNT = LaunchCount.START * SECOND;
    private static final int T8 = 8 * SECOND;
    /*
     * Where LAUNCH is written beside its key, x0, y0, x1, y1 in the face's pixels; how red a point of it lit is, and
     * the least of its points that are, lit.
     */
    private static final int[] LAUNCH_BOX = {FlightComputerFace.SX1 - 50, FlightComputerFace.sideKeyY(4),
        FlightComputerFace.SX1 - 3, FlightComputerFace.sideKeyY(4) + 12};
    private static final int LAUNCH_RED = 150;
    private static final int LEAST_RED = 12;
    /*
     * A rocket in a wide orbit of Mars: where it is built, how far east of its computer the player flies in space,
     * facing it; the transfer page's ORBIT row and the landing page's box saying to capture first, x0, y0, x1, y1 in
     * the face's pixels; how many amber points prove each; how long the rocket's travel may take.
     */
    private static final BlockPos ORBITER = new BlockPos(6, 2, 6);
    private static final double BESIDE = 2;
    private static final float WEST = 90F;
    private static final int[] WIDE_ROW = {FlightComputerFace.SX0 + 150, FlightComputerFace.SY0 + 29,
        FlightComputerFace.SX1 - 46, FlightComputerFace.SY0 + 40};
    private static final int[] CAPTURE_BOX = {FlightComputerFace.SX0 + 50, FlightComputerFace.SY0 + 54,
        FlightComputerFace.SX1 - 50, FlightComputerFace.SY0 + 84};
    private static final int LEAST_WIDE_AMBER = 8;
    private static final int LEAST_BANNER_AMBER = 40;
    private static final int TRAVEL = 1200;
    /* The transfer page's aerobraking key, from its first right key. */
    private static final int AEROBRAKE = 3;

    private FlightComputerClientTests() {
    }

    @ClientTest(timeoutTicks = 1500)
    public static void computer_opensOnTheTransferAndDrawsTheLandingMap(final ClientTestContext ctx) {
        final int[][] seen = new int[3][];
        ctx.afterTest(() -> ctx.server().execute(() -> ctx.serverLevel().removeBlock(ctx.abs(COMPUTER), false)));
        ctx.thenTeleport(0, EYE, Direction.SOUTH)
                .thenServer(0, level -> level.setBlockAndUpdate(ctx.abs(COMPUTER),
                        RocketKitBlocks.FLIGHT_COMPUTER.get().defaultBlockState()))
                .thenRightClick(SETTLE, COMPUTER)
                .thenAwaitScreen(FlightComputerScreen.class, OPEN)
                .then(SETTLE, () -> {
                    seen[0] = sample(ctx, SUN);
                    seen[2] = sample(ctx, FAIL_GLASS);
                })
                .then(0, () -> ctx.assertTrue(seen[2][0] <= UNLIT && seen[2][1] <= UNLIT && seen[2][2] <= UNLIT,
                        "nothing failing, the fail lamp's glass reads dark, not lit: " + str(seen[2])))
                .thenScreenshot(0, "flight-computer-transfer")
                .then(0, () -> {
                    final int[] at = screenOf(ctx, LANDING_KEY);
                    ctx.click(at[0], at[1]);
                })
                .thenWaitUntil(() -> mapOfThePage(ctx), MAP_WAIT, "the landing page and its map to come")
                .then(SETTLE, () -> seen[1] = sample(ctx, ON_MAP))
                .thenScreenshot(0, "flight-computer-landing")
                .then(0, () -> {
                    ctx.assertTrue(seen[0][0] > YELLOW && seen[0][1] > YELLOW && seen[0][2] < seen[0][1],
                            "the Sun is drawn yellow in the middle of the system: " + str(seen[0]));
                    final int dr = Math.abs(seen[1][0] - (SPACE >> 16 & CHANNEL));
                    final int dg = Math.abs(seen[1][1] - (SPACE >> 8 & CHANNEL));
                    final int db = Math.abs(seen[1][2] - (SPACE & CHANNEL));
                    ctx.assertTrue(dr + dg + db > APART, "the map of Mars is drawn where the page was dark: "
                            + str(seen[1]));
                    ctx.assertTrue(Math.abs(seen[1][0] - seen[1][2]) < GREY, "a world the team never saw lies under"
                            + " the grey of the unknown: " + str(seen[1]));
                    ctx.assertTrue(LandingMapsClient.shown().blocks() == FlightComputerBlockEntity.WIDE_BLOCKS,
                            "the wider map first");
                    ctx.mc().setScreen(null);
                });
    }

    @ClientTest(timeoutTicks = 1500)
    public static void computer_landingShowsTheGroundTheTeamKnows(final ClientTestContext ctx) {
        final int[] rust = new int[1];
        final int[] known = new int[1];
        ctx.afterTest(() -> ctx.server().execute(() -> ctx.serverLevel().removeBlock(ctx.abs(COMPUTER), false)));
        ctx.thenTeleport(0, EYE, Direction.SOUTH)
                .thenServer(0, level -> {
                    level.setBlockAndUpdate(ctx.abs(COMPUTER),
                            RocketKitBlocks.FLIGHT_COMPUTER.get().defaultBlockState());
                    // A pass photographed by the player's team, far south of Mars' middle where it swings, so the
                    // middle the other test draws stays unknown; the spot marked under the pass and set.
                    final ServerPlayer player = level.getServer().getPlayerList().getPlayers().get(0);
                    WorldCoverage.photograph(level.getServer(), CoreTeams.teamOf(level.getServer(),
                            player.getUUID()), SolarSystemWorlds.MARS, PASS_NODE);
                    final ServerLevel mars = level.getServer().getLevel(SpaceWorlds.get(level,
                            SolarSystemWorlds.MARS).orElseThrow().dimension());
                    if (mars != null && level.getBlockEntity(ctx.abs(COMPUTER)) instanceof FlightComputerBlockEntity
                            computer) {
                        computer.press(FlightComputerBlockEntity.TOP + FlightPage.KEYS.indexOf(FlightPage.LANDING));
                        computer.markAt(0, (int) SphereMap.z(-ReliefCoverage.TRACK, mars.getWorldBorder()
                                .getSize()));
                        computer.press(FlightComputerBlockEntity.RIGHT + 4);
                    }
                })
                .thenRightClick(SETTLE, COMPUTER)
                .thenAwaitScreen(FlightComputerScreen.class, OPEN)
                .thenWaitUntil(() -> mapOfThePage(ctx) && ctx.screen(FlightComputerScreen.class).getMenu().computer()
                        .mark() != null, MAP_WAIT, "the landing map round the marked spot to come")
                .then(SETTLE, () -> {
                    rust[0] = rustIn(ctx, MAP_BOX);
                    known[0] = greenIn(ctx, KNOWN_LABEL);
                })
                .thenScreenshot(0, "flight-computer-landing-known")
                .then(0, () -> {
                    final int points = (MAP_BOX[2] - MAP_BOX[0]) * (MAP_BOX[3] - MAP_BOX[1]);
                    ctx.assertTrue(rust[0] * LEAST_RUST_IN >= points, "the ground of Mars the pass saw is drawn in"
                            + " its own colours: " + rust[0] + " of " + points + " points of the map in its red");
                    ctx.assertTrue(LandingMapsClient.shown().share() > 0 && known[0] >= LEAST_GREEN,
                            "how much of Mars is known is written over its small map: " + known[0] + " green points");
                    ctx.assertTrue(ctx.screen(FlightComputerScreen.class).getMenu().computer().landing().isPresent(),
                            "the landing is set where the spot is marked");
                    ctx.mc().setScreen(null);
                });
    }

    @ClientTest(timeoutTicks = 1500)
    public static void computer_stagesPageDrawsTheStackAndSetsAFate(final ClientTestContext ctx) {
        final int[][] seen = new int[1][];
        ctx.afterTest(() -> ctx.server().execute(() -> ctx.serverLevel().removeBlock(ctx.abs(COMPUTER), false)));
        ctx.thenTeleport(0, EYE, Direction.SOUTH)
                .thenServer(0, level -> {
                    level.setBlockAndUpdate(ctx.abs(COMPUTER),
                            RocketKitBlocks.FLIGHT_COMPUTER.get().defaultBlockState());
                    if (level.getBlockEntity(ctx.abs(COMPUTER)) instanceof FlightComputerBlockEntity computer) {
                        computer.carry(new RocketDesign("STAGES", STAGED), BlockPos.ZERO, List.of());
                        computer.press(FlightComputerBlockEntity.TOP + FlightPage.KEYS.indexOf(FlightPage.STAGES));
                    }
                })
                .thenRightClick(SETTLE, COMPUTER)
                .thenAwaitScreen(FlightComputerScreen.class, OPEN)
                .then(SETTLE, () -> seen[0] = sample(ctx, RING))
                .then(0, () -> {
                    final int[] at = screenOf(ctx, ZONE_KEY);
                    ctx.click(at[0], at[1]);
                })
                .thenWaitUntil(() -> ctx.screen(FlightComputerScreen.class) != null && ctx.screen(
                        FlightComputerScreen.class).getMenu().computer().choices().containsKey(1), OPEN,
                        "the first stage set to fall in a zone")
                .then(SETTLE, () -> { })
                .thenScreenshot(0, "flight-computer-stages")
                .then(0, () -> {
                    ctx.assertTrue(seen[0][0] > AMBER_RED && seen[0][1] > AMBER_GREEN && seen[0][2] < seen[0][1],
                            "the first stage's separation ring is drawn amber in the stack: " + str(seen[0]));
                    final StageChoice set = ctx.screen(FlightComputerScreen.class).getMenu().computer().choices()
                            .get(1);
                    ctx.assertTrue(!set.lands() && set.radius() == StageChoice.FIRST_RADIUS,
                            "ZONE sets a fall zone round the pad: " + set);
                    ctx.mc().setScreen(null);
                });
    }

    @ClientTest(timeoutTicks = 1500)
    public static void computer_xferTellsAPlanShortOfDeltaV(final ClientTestContext ctx) {
        final int[] amber = new int[1];
        ctx.afterTest(() -> ctx.server().execute(() -> ctx.serverLevel().removeBlock(ctx.abs(COMPUTER), false)));
        ctx.thenTeleport(0, EYE, Direction.SOUTH)
                .thenServer(0, level -> {
                    level.setBlockAndUpdate(ctx.abs(COMPUTER),
                            RocketKitBlocks.FLIGHT_COMPUTER.get().defaultBlockState());
                    if (level.getBlockEntity(ctx.abs(COMPUTER)) instanceof FlightComputerBlockEntity computer) {
                        // A rocket with empty tanks: it has no delta-v for the transfer.
                        computer.carry(new RocketDesign("DRY", STAGED), BlockPos.ZERO, List.of());
                    }
                })
                .thenRightClick(SETTLE, COMPUTER)
                .thenAwaitScreen(FlightComputerScreen.class, OPEN)
                .then(SETTLE, () -> amber[0] = amberIn(ctx, STATUS))
                .thenScreenshot(0, "flight-computer-xfer-short")
                .then(0, () -> {
                    ctx.assertTrue(amber[0] >= LEAST_AMBER, "the plan is told short of delta-v, in amber, where its"
                            + " status is written: " + amber[0] + " amber points");
                    ctx.mc().setScreen(null);
                });
    }

    /**
     * On a launcher with no Launch Panel, the computer opens on its LAUNCH page; its keys, clicked, arm the MASTER key
     * and the ignition and start the pad's count, seen at T-42 with the tower's arms moving; at T-8, LAUNCH lit under
     * its lifted cover, it is pressed, and SCRUB calls the launch off.
     */
    @ClientTest(timeoutTicks = 1800)
    public static void computer_launchPageWorksThePadsCount(final ClientTestContext ctx) {
        final int[] red = new int[2];
        ctx.afterTest(() -> ctx.server().execute(() -> {
            ctx.serverLevel().removeBlock(ctx.abs(COMPUTER), false);
            ctx.serverLevel().removeBlock(ctx.abs(LAUNCHER), false);
        }));
        ctx.thenTeleport(0, EYE, Direction.SOUTH)
                .thenServer(0, level -> launcher(ctx, level))
                .thenServer(2 * SECOND, level -> ctx.assertTrue(level.getBlockEntity(ctx.abs(COMPUTER))
                        instanceof FlightComputerBlockEntity computer && computer.page() == FlightPage.LAUNCH
                        && computer.launch().map(count -> count.shown().go()).orElse(false),
                        "on the launcher, the computer opens on its LAUNCH page, all go"))
                .thenRightClick(SETTLE, COMPUTER)
                .thenAwaitScreen(FlightComputerScreen.class, OPEN)
                .then(SETTLE, () -> clickKey(ctx, LEFT_KEY, 0))
                .then(SETTLE, () -> clickKey(ctx, LEFT_KEY, 1))
                .then(SETTLE, () -> clickKey(ctx, RIGHT_KEY, 0))
                .thenWaitUntil(() -> launchPhase(ctx) == LaunchCount.Phase.COUNTING, OPEN,
                        "MASTER, IGNITION and COUNT clicked on the page start the count")
                .thenWaitUntil(() -> shownOf(ctx).left() <= LaunchCount.ARMS * SECOND, TO_COUNT,
                        "the count at T-42")
                .then(SETTLE, () -> red[0] = redIn(ctx, LAUNCH_BOX))
                .thenScreenshot(0, "flight-computer-launch")
                .thenWaitUntil(() -> shownOf(ctx).left() <= T8, TO_COUNT, "the count at T-8")
                .then(1, () -> red[1] = redIn(ctx, LAUNCH_BOX))
                .thenScreenshot(0, "flight-computer-launch-t8")
                .then(0, () -> clickKey(ctx, RIGHT_KEY, 4))
                .thenWaitUntil(() -> shownOf(ctx).launched(), OPEN, "LAUNCH clicked on the page")
                .then(0, () -> clickKey(ctx, RIGHT_KEY, 2))
                .thenWaitUntil(() -> launchPhase(ctx) == LaunchCount.Phase.SCRUBBED, OPEN,
                        "SCRUB clicked on the page calls the launch off")
                .then(0, () -> {
                    ctx.assertTrue(red[0] == 0, "LAUNCH stays dim under its cover at T-42: " + red[0] + " red points");
                    ctx.assertTrue(red[1] >= LEAST_RED, "and is lit red with its cover lifted at T-8: " + red[1]
                            + " red points");
                    ctx.mc().setScreen(null);
                });
    }

    /**
     * A rocket in a wide orbit of Mars, arrived short of its capture burn: its transfer page's first key is CAPT,
     * chosen, its block reading the orbit WIDE in amber, the keys of a transfer's window, delta-v and assist blank;
     * its landing page lays a box over its map of Mars saying to capture first.
     */
    @ClientTest(timeoutTicks = 4000)
    public static void computer_offersTheCaptureFromAWideOrbit(final ClientTestContext ctx) {
        final MovingStructure[] rocket = new MovingStructure[1];
        final FlightComputerBlockEntity[] computer = new FlightComputerBlockEntity[1];
        final double[] mars = SystemLayout.orbitSpot(SolarBody.MARS, 0, 0);
        final int[] seen = new int[2];
        ctx.afterTest(() -> {
            ctx.mc().setScreen(null);
            ctx.server().execute(() -> {
                final ServerLevel space = space(ctx);
                final MovingStructure there = rocket[0] == null ? null : MovingStructures.byId(space, rocket[0].id());
                if (there != null) {
                    MovingStructures.takeAway(there);
                }
                space.setChunkForced(SectionPos.blockToSectionCoord(mars[0]), SectionPos.blockToSectionCoord(mars[1]),
                        false);
                final ServerPlayer player = ctx.serverPlayer();
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                final BlockPos home = ctx.abs(EYE);
                player.teleportTo(ctx.serverLevel(), home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0F, 0F);
            });
        });
        ctx.thenTeleport(0, EYE, Direction.SOUTH)
                .thenServer(0, level -> rocket[0] = orbiter(ctx, level))
                .thenWaitUntilServer(level -> rocket[0].ready(), TRAVEL, "the rocket to stand ready",
                        level -> "arriving " + rocket[0].arriving())
                .thenServer(0, level -> {
                    final ServerLevel space = space(ctx);
                    space.setChunkForced(SectionPos.blockToSectionCoord(mars[0]), SectionPos.blockToSectionCoord(
                            mars[1]), true);
                    ctx.assertTrue(MovingStructures.travel(rocket[0], space, new Vec3(mars[0], SystemLayout.ORBIT_Y,
                            mars[1])).ok(), "the rocket is sent into Mars' orbit");
                })
                .thenWaitUntilServer(level -> inMarsOrbit(ctx, rocket[0]) != null, TRAVEL,
                        "the rocket to rest in Mars' orbit", level -> "departing " + rocket[0].departing())
                .thenServer(0, level -> {
                    final ServerLevel space = space(ctx);
                    final MovingStructure there = inMarsOrbit(ctx, rocket[0]);
                    computer[0] = computerOf(space, there);
                    OrbitObjects.arrived(space, there, SolarBody.MARS);
                    // As one arriving short of its capture burn: the capture chosen on the transfer page, burnt.
                    computer[0].wideOrbit(true);
                    computer[0].press(FlightComputerBlockEntity.TOP + FlightPage.KEYS.indexOf(FlightPage.TRANSFER));
                    computer[0].press(FlightComputerBlockEntity.LEFT);
                    if (computer[0].choice().aero()) {
                        computer[0].press(FlightComputerBlockEntity.RIGHT + AEROBRAKE);
                    }
                    final Vec3 at = MovingStructures.inWorld(space, Vec3.atCenterOf(computer[0].getBlockPos()));
                    final ServerPlayer player = ctx.serverPlayer();
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                    player.teleportTo(space, at.x + BESIDE, at.y - 1, at.z, WEST, 0F);
                })
                .thenWaitUntil(() -> ctx.mc().level != null && ctx.mc().level.getBlockEntity(computer[0]
                        .getBlockPos()) instanceof FlightComputerBlockEntity theirs && theirs.capturing().isPresent(),
                        TRAVEL, "the computer in its wide orbit to reach the player's game")
                .thenServer(0, level -> ctx.serverPlayer().openMenu(new SimpleMenuProvider((id, inventory, opener)
                        -> new FlightComputerMenu(id, inventory, computer[0]), Component.empty()),
                        buf -> buf.writeBlockPos(computer[0].getBlockPos())))
                .thenAwaitScreen(FlightComputerScreen.class, OPEN)
                .then(SETTLE, () -> seen[0] = amberIn(ctx, WIDE_ROW))
                .thenScreenshot(0, "flight-computer-capture")
                .then(0, () -> {
                    final int[] key = screenOf(ctx, LANDING_KEY);
                    ctx.click(key[0], key[1]);
                })
                .thenWaitUntil(() -> mapOfThePage(ctx), MAP_WAIT, "the landing page and its map of Mars to come")
                .then(SETTLE, () -> seen[1] = amberIn(ctx, CAPTURE_BOX))
                .thenScreenshot(0, "flight-computer-capture-land")
                .then(0, () -> {
                    ctx.assertTrue(seen[0] >= LEAST_WIDE_AMBER, "the transfer page reads the wide orbit in amber: "
                            + seen[0] + " amber points");
                    ctx.assertTrue(seen[1] >= LEAST_BANNER_AMBER, "the landing page's box says to capture first: "
                            + seen[1] + " amber points");
                    ctx.mc().setScreen(null);
                });
    }

    /* How many points of the face's box {@code box}, x0, y0, x1, y1 in its own pixels, are the screen's green. */
    private static int greenIn(final ClientTestContext ctx, final int[] box) {
        int count = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int x = box[0]; x < box[2]; x++) {
                for (int y = box[1]; y < box[3]; y++) {
                    final int[] point = screenOf(ctx, new int[] {x, y});
                    final int argb = frame.pixel(point[0], point[1]);
                    if ((argb >> 8 & CHANNEL) > GREEN && (argb >> 16 & CHANNEL) < (argb >> 8 & CHANNEL) / 2) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /* How many points of the face's box {@code box}, x0, y0, x1, y1 in its own pixels, are in Mars' red. */
    private static int rustIn(final ClientTestContext ctx, final int[] box) {
        int count = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int x = box[0]; x < box[2]; x++) {
                for (int y = box[1]; y < box[3]; y++) {
                    final int[] point = screenOf(ctx, new int[] {x, y});
                    final int argb = frame.pixel(point[0], point[1]);
                    if ((argb >> 16 & CHANNEL) > (argb & CHANNEL) + RUST) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /* How many points of the face's box {@code box}, x0, y0, x1, y1 in its own pixels, are amber. */
    private static int amberIn(final ClientTestContext ctx, final int[] box) {
        int count = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int x = box[0]; x < box[2]; x++) {
                for (int y = box[1]; y < box[3]; y++) {
                    final int[] point = screenOf(ctx, new int[] {x, y});
                    final int argb = frame.pixel(point[0], point[1]);
                    if ((argb >> 16 & CHANNEL) > AMBER_RED && (argb >> 8 & CHANNEL) > AMBER_GREEN
                            && (argb & CHANNEL) < (argb >> 8 & CHANNEL) / 2) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /* How many points of the face's box {@code box}, x0, y0, x1, y1 in its own pixels, are a lamp's red. */
    private static int redIn(final ClientTestContext ctx, final int[] box) {
        int count = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int x = box[0]; x < box[2]; x++) {
                for (int y = box[1]; y < box[3]; y++) {
                    final int[] point = screenOf(ctx, new int[] {x, y});
                    final int argb = frame.pixel(point[0], point[1]);
                    if ((argb >> 16 & CHANNEL) > LAUNCH_RED && (argb >> 8 & CHANNEL) < (argb >> 16 & CHANNEL) / 2) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /*
     * Whether the open Flight Computer is on its landing page and the landing map last come is the one drawn for what
     * the page shows now: its world, round its marked spot or the world's middle. The maps are drawn away from the
     * server's thread and the last one stays till the next comes, so a map asked for before, round another spot or of
     * another test's page, is never taken for it.
     */
    private static boolean mapOfThePage(final ClientTestContext ctx) {
        final FlightComputerScreen screen = ctx.screen(FlightComputerScreen.class);
        final LandingMapsClient.Shown map = LandingMapsClient.shown();
        if (screen == null || map == null) {
            return false;
        }
        final FlightComputerBlockEntity computer = screen.getMenu().computer();
        final BlockPos centre = computer.mark() == null ? BlockPos.ZERO : computer.mark();
        return computer.page() == FlightPage.LANDING && map.world().equals(computer.target())
                && map.centreX() == Math.floorDiv(centre.getX(), map.blocks()) * map.blocks()
                && map.centreZ() == Math.floorDiv(centre.getZ(), map.blocks()) * map.blocks();
    }

    /* A launcher's measuring arm beside the computer, its axis under it, and the computer's fuelled rocket on it. */
    private static void launcher(final ClientTestContext ctx, final ServerLevel level) {
        level.setWeatherParameters(6000, 0, false, false);
        level.setThunderLevel(0.0F);
        level.setRainLevel(0.0F);
        level.setBlockAndUpdate(ctx.abs(LAUNCHER), LauncherBlocks.HOLD_DOWN_ARM.get().defaultBlockState());
        level.setBlockAndUpdate(ctx.abs(COMPUTER), RocketKitBlocks.FLIGHT_COMPUTER.get().defaultBlockState());
        if (level.getBlockEntity(ctx.abs(LAUNCHER)) instanceof LauncherDeckBlockEntity arm) {
            arm.deck(new LauncherDeckBlockEntity.Deck(Direction.EAST, 1, TO_AXIS, 6, 8, 6));
        }
        final RocketStack stack = RocketStack.of(part("sea_level_engine_3"), part("tank_3"), part("capsule_3"));
        if (level.getBlockEntity(ctx.abs(COMPUTER)) instanceof FlightComputerBlockEntity rocket) {
            rocket.carry(new RocketDesign("Orbital I", stack), new BlockPos(0, -4, 0), List.of());
            Propellants.get(level, SkeletonPropellants.METHALOX).ifPresent(methalox -> rocket.load(1,
                    TankLoad.filled(SkeletonPropellants.METHALOX, methalox, FULL_TANK)));
        }
    }

    /* Clicks the {@code i}th key from the top down the left or the right of the face. */
    private static void clickKey(final ClientTestContext ctx, final int side, final int i) {
        final int[] at = screenOf(ctx, new int[] {side, FlightComputerFace.sideKeyY(i) + 5});
        ctx.click(at[0], at[1]);
    }

    /* What the open screen's computer shows of its launcher's count, as the player's game has it. */
    private static LaunchCount.Shown shownOf(final ClientTestContext ctx) {
        final FlightComputerScreen screen = ctx.screen(FlightComputerScreen.class);
        return screen == null ? LaunchCount.Shown.NONE : screen.getMenu().computer().launch().map(LaunchCount::shown)
                .orElse(LaunchCount.Shown.NONE);
    }

    private static LaunchCount.Phase launchPhase(final ClientTestContext ctx) {
        return shownOf(ctx).phase();
    }

    /* Where the face's pixel {@code at} is on the window, in the screen's own scaled units. */
    private static int[] screenOf(final ClientTestContext ctx, final int[] at) {
        final FlightComputerScreen screen = ctx.screen(FlightComputerScreen.class);
        final float scale = screen.getXSize() / (float) FlightComputerFace.WIDTH;
        return new int[] {Math.round(screen.getGuiLeft() + (at[0] + 0.5F) * scale),
            Math.round(screen.getGuiTop() + (at[1] + 0.5F) * scale)};
    }

    private static int[] sample(final ClientTestContext ctx, final int[] at) {
        final int[] point = screenOf(ctx, at);
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            final int argb = frame.pixel(point[0], point[1]);
            return new int[] {argb >> 16 & CHANNEL, argb >> 8 & CHANNEL, argb & CHANNEL};
        }
    }

    private static String str(final int[] colour) {
        return "(" + colour[0] + ", " + colour[1] + ", " + colour[2] + ")";
    }

    private static RocketStack.Part part(final String path) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), 1);
    }

    /*
     * An engine, a tank full of methalox and a capsule, built whole at ORBITER, every part formed, and let go as one
     * moving structure: its body.
     */
    private static MovingStructure orbiter(final ClientTestContext ctx, final ServerLevel level) {
        final RocketStack stack = RocketStack.of(part("sea_level_engine_3"), part("tank_3"), part("capsule_3"));
        final BlockPos at = ctx.abs(ORBITER);
        final PlannedBuild build = new PlannedBuild(RocketPlans.of(level, stack), at);
        build.start(level, ctx.abs(EYE), () -> { });
        BlockPos computerAt = at;
        for (final PlanSpot spot : build.plan().spots()) {
            level.setBlockAndUpdate(build.where(spot), BuiltInRegistries.BLOCK.get(spot.block()).defaultBlockState());
            if (spot.block().equals(RocketKitBlocks.FLIGHT_COMPUTER.getId())) {
                computerAt = build.where(spot);
            }
        }
        ctx.assertTrue(build.complete(), "the rocket is built whole");
        build.stop(false);
        if (!(level.getBlockEntity(computerAt) instanceof FlightComputerBlockEntity computer)) {
            throw new IllegalStateException("the rocket holds no flight computer");
        }
        computer.carry(new RocketDesign("Orbital I", stack), at.subtract(computerAt), IntStream.range(0,
                stack.sections().size()).boxed().toList());
        Propellants.get(level, SkeletonPropellants.METHALOX).ifPresent(methalox -> computer.load(1,
                TankLoad.filled(SkeletonPropellants.METHALOX, methalox, FULL_TANK)));
        return RocketLaunch.release(level, computerAt, null).orElseThrow();
    }

    /* The body of {@code rocket} resting in Mars' orbit, in the Solar System's space; none while it is not there. */
    private static @Nullable MovingStructure inMarsOrbit(final ClientTestContext ctx, final MovingStructure rocket) {
        final MovingStructure there = MovingStructures.byId(space(ctx), rocket.id());
        return there != null && there.ready() ? there : null;
    }

    /* The flight computer aboard {@code body}, in its yard. */
    private static FlightComputerBlockEntity computerOf(final ServerLevel level, final MovingStructure body) {
        final Box box = body.bounds();
        for (final BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                box.maxZ())) {
            if (level.getBlockEntity(pos) instanceof FlightComputerBlockEntity computer) {
                return computer;
            }
        }
        throw new IllegalStateException("no flight computer aboard");
    }

    private static ServerLevel space(final ClientTestContext ctx) {
        return SystemSpaces.spaceOf(ctx.server(), StarSystems.SOLAR_SYSTEM);
    }
}
