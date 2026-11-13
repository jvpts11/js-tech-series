/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.api.Propellants;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.hangar.HangarTerminalBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.launch.LaunchCount;
import dev.jstech.space.launch.LaunchPanelBlockEntity;
import dev.jstech.space.launcher.LauncherBlocks;
import dev.jstech.space.launcher.LauncherDeckBlockEntity;
import dev.jstech.space.launcher.LauncherPieceBlock;
import dev.jstech.space.pad.LaunchPads;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonPropellants;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.space.rocket.TankLoad;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.space.TestHangars;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * A mobile launcher built by its hangar's plan on a player's game: once its last blocks are down it takes its formed
 * shape, as the approved launcher is drawn: the tower the red truss round its shaft, seen red where the player looks at
 * it; the deck one piece round its opening, the hazard band's stripes round it; and the whole of it seen from high in
 * the bay, to be put beside the approved pictures.
 */
public final class LauncherClientTests {

    /* The hangar's terminal, its door to the north; the bay's middle at (5, 2, 11) of the test's place. */
    private static final BlockPos TERMINAL = new BlockPos(22, 2, 9);
    private static final Orientation TURN = Orientation.NORTH;
    /* Columns of lamps in the bay, each side of the launcher's tower, from its deck to past its top. */
    private static final int[] LIGHT_X = {-2, 12};
    private static final int[] LIGHT_Z = {14, 22};
    private static final int LIGHT_FROM = 8;
    private static final int LIGHT_TO = 24;
    private static final int LIGHT_EVERY = 4;
    private static final int ARRIVE = 400;
    private static final int FORM = 200;
    private static final int SETTLE = 20;
    private static final int AWAY = 9;
    private static final int WINDOW = 20;
    private static final int CROSS = 5;
    private static final int CHANNEL = 0xFF;
    private static final int REDDER = 40;
    private static final double ENOUGH_RED = 0.2;
    /*
     * On the deck: where the player stands across from the rocket's foot, and where the hazard band round the opening
     * is looked at, in blocks to the tower's left; high in the bay's corner, how far out, along and up.
     */
    private static final double ON_DECK = 3.0;
    private static final double BAND = 1.75;
    private static final double HIGH_OUT = 10.0;
    private static final double HIGH_ALONG = -4.0;
    private static final double HIGH_UP = 5.0;
    private static final int BAND_WINDOW = 8;
    private static final int YELLOWER = 70;
    private static final int DARK = 60;
    private static final double ENOUGH_STRIPES = 0.08;
    /*
     * The count: its rocket's two layers of tank full, in kilograms; how long it may take to reach T-10 from its start,
     * in ticks; and the tower's arms half swung away.
     */
    private static final double TANK_LOAD = 11_600.0;
    private static final int TO_T10 = (LaunchCount.START - LaunchCount.COVER) * LaunchCount.SECOND + 200;
    private static final float HALF_SWUNG = 0.5F;

    private LauncherClientTests() {
    }

    @ClientTest(timeoutTicks = 4200)
    public static void launcher_isDrawnAsTheApprovedOneOnceItForms(final ClientTestContext ctx) {
        final BlockPos[] tower = new BlockPos[1];
        final BlockPos[] eye = new BlockPos[1];
        final Vec3[] deck = new Vec3[4];
        // The rocket's foot, then the launcher's measuring arm found by it; and the rocket's flight computer.
        final BlockPos[] launch = new BlockPos[2];
        ctx.afterTest(() -> ctx.server().execute(() -> TestHangars.clear(ctx.serverLevel(), ctx.abs(TERMINAL),
                TURN)));
        ctx.thenServer(0, level -> build(ctx, level))
                .thenWaitUntilServer(level -> level.getBlockEntity(ctx.abs(TERMINAL))
                        instanceof HangarTerminalBlockEntity terminal && terminal.formed(), ARRIVE,
                        "the hangar to form", level -> "it stands unformed")
                .thenServer(0, level -> lay(ctx, level, tower, eye, deck, launch))
                .thenWaitUntil(() -> eye[0] != null, SETTLE, "the launcher to be laid")
                .thenServer(0, level -> {
                    // On a block no one sees, level with the deck, so the deck is not in the way.
                    level.setBlockAndUpdate(eye[0].below(), Blocks.BARRIER.defaultBlockState());
                    level.players().getFirst().teleportTo(level, eye[0].getX() + 0.5, eye[0].getY(),
                            eye[0].getZ() + 0.5, 0.0F, 0.0F);
                })
                .thenWaitUntil(() -> ctx.mc().level.getBlockState(tower[0]).getBlock() instanceof LauncherPieceBlock
                        && ctx.mc().level.getBlockState(tower[0]).getValue(LauncherPieceBlock.FORMED), ARRIVE + FORM,
                        "the formed tower to reach the player")
                .then(SETTLE, () -> ctx.lookAt(Vec3.atCenterOf(tower[0])))
                .then(SETTLE, () -> { })
                .thenScreenshot(0, "launcher-formed")
                .then(0, () -> {
                    final double red = red(ctx);
                    ctx.assertTrue(red >= ENOUGH_RED, "the formed tower's truss is seen red round the crosshair: "
                            + Math.round(red * 100) + "% of it");
                })
                // Standing on the deck, looking down at the hazard band round the opening.
                .thenServer(0, level -> level.players().getFirst().teleportTo(level, deck[0].x(), deck[0].y(),
                        deck[0].z(), 0.0F, 0.0F))
                .then(SETTLE, () -> ctx.lookAt(deck[1]))
                .then(SETTLE, () -> { })
                .thenScreenshot(0, "launcher-deck")
                .then(0, () -> {
                    final double[] band = band(ctx);
                    ctx.assertTrue(band[0] >= ENOUGH_STRIPES && band[1] >= ENOUGH_STRIPES, "the deck is drawn"
                            + " with the hazard band's yellow and black stripes round the opening: "
                            + Math.round(band[0] * 100) + "% yellow, " + Math.round(band[1] * 100) + "% black");
                })
                // From high in the bay's corner, the whole launcher as the approved pictures show it.
                .thenServer(0, level -> {
                    level.setBlockAndUpdate(BlockPos.containing(deck[2]).below(), Blocks.BARRIER.defaultBlockState());
                    level.players().getFirst().teleportTo(level, deck[2].x(), deck[2].y(), deck[2].z(), 0.0F, 0.0F);
                })
                .then(SETTLE, () -> ctx.lookAt(deck[1].add(0, 2, 0)))
                .then(SETTLE, () -> { })
                .thenScreenshot(0, "launcher-overview")
                // And up at the crane on the tower's top, its boom over the rocket.
                .then(SETTLE, () -> ctx.lookAt(deck[3]))
                .then(SETTLE, () -> { })
                .thenScreenshot(0, "launcher-crane")
                // The count run: the tower's arms swing away from the rocket from T-42, clear of it by T-10.
                .thenServer(0, level -> count(ctx, level, launch))
                .then(SETTLE, () -> ctx.lookAt(deck[1].add(0, 2, 0)))
                .thenWaitUntilServer(level -> swung(ctx, level, launch) >= HALF_SWUNG, TO_T10, "the arms half away",
                        level -> "swung " + swung(ctx, level, launch))
                .then(SETTLE, () -> { })
                .thenScreenshot(0, "launcher-arms-swinging")
                .thenWaitUntilServer(level -> swung(ctx, level, launch) >= 1, TO_T10, "the arms clear of the rocket",
                        level -> "swung " + swung(ctx, level, launch))
                .then(SETTLE, () -> { })
                .thenScreenshot(0, "launcher-arms-away")
                .thenServer(0, level -> LauncherDeckBlockEntity.countAt(level, launch[0])
                        .ifPresent(count -> count.press(LaunchCount.SCRUB)));
    }

    /*
     * Its rocket's tank filled and its count's MASTER and IGNITION armed, the launcher's measuring arm found by its
     * rocket's foot.
     */
    private static void count(final ClientTestContext ctx, final ServerLevel level, final BlockPos[] launch) {
        level.setWeatherParameters(6000, 0, false, false);
        level.setThunderLevel(0.0F);
        level.setRainLevel(0.0F);
        if (level.getBlockEntity(launch[1]) instanceof FlightComputerBlockEntity rocket) {
            // Its plan, as it takes it rolled out of its hangar, for the count to find it.
            rocket.carry(new RocketDesign("LAUNCHER", stack()), BlockPos.ZERO, List.of());
            Propellants.get(level, SkeletonPropellants.METHALOX).ifPresent(methalox -> rocket.load(1,
                    TankLoad.filled(SkeletonPropellants.METHALOX, methalox, TANK_LOAD)));
        }
        launch[0] = LaunchPads.launcherBy(level, launch[0], LaunchPanelBlockEntity.REACH,
                LaunchPanelBlockEntity.BELOW, LaunchPanelBlockEntity.ABOVE).map(LauncherDeckBlockEntity::getBlockPos)
                .orElse(launch[0]);
        final LaunchCount count = LauncherDeckBlockEntity.countAt(level, launch[0]).orElse(null);
        ctx.assertTrue(count != null, "the launcher keeps its count");
        count.press(LaunchCount.MASTER);
        count.press(LaunchCount.SWITCH);
    }

    /* How far the launcher's count has its tower's arms swung, the count started once it is all go. */
    private static float swung(final ClientTestContext ctx, final ServerLevel level, final BlockPos[] launch) {
        final LaunchCount count = LauncherDeckBlockEntity.countAt(level, launch[0]).orElse(null);
        if (count == null) {
            return 0;
        }
        if (count.phase() != LaunchCount.Phase.COUNTING && count.shown().go()) {
            count.press(LaunchCount.RUN);
        }
        return count.armsAway();
    }

    /*
     * The hangar built whole, its door to the north; and columns of lamps standing in the bay each side of where the
     * launcher goes, so its tower stands in light all the way up, away from the bay's walls.
     */
    private static void build(final ClientTestContext ctx, final ServerLevel level) {
        TestHangars.build(level, ctx.abs(TERMINAL), TURN);
        for (final int x : LIGHT_X) {
            for (final int z : LIGHT_Z) {
                for (int y = LIGHT_FROM; y <= LIGHT_TO; y += LIGHT_EVERY) {
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, z)), Blocks.GLOWSTONE.defaultBlockState());
                }
            }
        }
    }

    /*
     * Its plan sent, an engine, a tank and a capsule on a launcher, and every block of it laid at once; the player to
     * stand a few blocks off the tower's side and look at its middle; then on the deck beside the opening, looking at
     * the hazard band round it; then high in the bay's corner over the deck's left, looking at it whole.
     */
    private static void lay(final ClientTestContext ctx, final ServerLevel level, final BlockPos[] tower,
                            final BlockPos[] eye, final Vec3[] deck, final BlockPos[] launch) {
        final HangarTerminalBlockEntity terminal = (HangarTerminalBlockEntity) level.getBlockEntity(ctx.abs(TERMINAL));
        terminal.design(new RocketDesign("LAUNCHER", stack()));
        terminal.confirm();
        final Direction side = terminal.launcherSide().orElseThrow();
        final PlannedBuild plan = terminal.build().orElseThrow();
        // The rocket too, so its hologram is gone and the tower is seen as it is.
        for (final PlanSpot spot : plan.plan().spots()) {
            level.setBlockAndUpdate(plan.where(spot), BuiltInRegistries.BLOCK.get(spot.block()).defaultBlockState());
        }
        final int middle = plan.plan().spots().stream().filter(spot -> spot.block().equals(LauncherBlocks
                .TOWER_FRAME.getId())).mapToInt(spot -> spot.at().getY()).max().orElseThrow() / 2;
        // The frame of the tower's side across the rocket's line, halfway up, seen from that side.
        final Direction across = side.getClockWise();
        final PlanSpot seen = plan.plan().spots().stream().filter(spot -> spot.block().equals(LauncherBlocks
                .TOWER_FRAME.getId()) && spot.at().getY() == middle).max(Comparator.comparingInt(spot ->
                spot.at().getX() * across.getStepX() + spot.at().getZ() * across.getStepZ())).orElseThrow();
        tower[0] = plan.where(seen);
        final Vec3 foot = Vec3.atBottomCenterOf(plan.origin());
        final Vec3 left = new Vec3(-across.getStepX(), 0, -across.getStepZ());
        final Vec3 along = new Vec3(side.getStepX(), 0, side.getStepZ());
        deck[0] = foot.add(left.scale(ON_DECK));
        deck[1] = foot.add(left.scale(BAND));
        deck[2] = foot.add(left.scale(HIGH_OUT)).add(along.scale(HIGH_ALONG)).add(0, HIGH_UP, 0);
        final int boom = plan.plan().spots().stream().filter(spot -> spot.block().equals(LauncherBlocks.TOWER_FRAME
                .getId())).mapToInt(spot -> spot.at().getY()).max().orElseThrow();
        deck[3] = foot.add(along.scale(HIGH_ALONG / 2)).add(0, boom, 0);
        eye[0] = tower[0].relative(across, AWAY).atY(plan.origin().getY());
        launch[0] = plan.origin();
        launch[1] = plan.plan().spots().stream().filter(spot -> spot.block().equals(RocketKitBlocks.FLIGHT_COMPUTER
                .getId())).map(plan::where).findFirst().orElse(plan.origin());
    }

    /* The rocket on the launcher: an engine, two layers of tank and a capsule. */
    private static RocketStack stack() {
        return RocketStack.of(part("sea_level_engine_3", 1), part("tank_3", 2), part("capsule_3", 1));
    }

    private static RocketStack.Part part(final String path, final int layers) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), layers);
    }

    /*
     * How much of a window round the crosshair, its own white cross left out, is the hazard band's yellow, and how
     * much its black.
     */
    private static double[] band(final ClientTestContext ctx) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2;
        int yellow = 0;
        int black = 0;
        int count = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int dx = -WINDOW; dx <= WINDOW; dx++) {
                for (int dy = -BAND_WINDOW; dy <= BAND_WINDOW; dy++) {
                    if (Math.abs(dx) <= CROSS && Math.abs(dy) <= CROSS) {
                        continue;
                    }
                    final int argb = frame.pixel(x + dx, y + dy);
                    final int r = argb >> 16 & CHANNEL;
                    final int g = argb >> 8 & CHANNEL;
                    final int b = argb & CHANNEL;
                    if (r > b + YELLOWER && g > b + YELLOWER / 2) {
                        yellow++;
                    } else if (r < DARK && g < DARK && b < DARK) {
                        black++;
                    }
                    count++;
                }
            }
        }
        return new double[] {(double) yellow / count, (double) black / count};
    }

    /*
     * How much of a window round the crosshair, its own white cross left out, is red: red well over green and blue, as
     * the truss's members are between the shaft's dark showing through them.
     */
    private static double red(final ClientTestContext ctx) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2;
        int red = 0;
        int count = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int dx = -WINDOW; dx <= WINDOW; dx++) {
                for (int dy = -WINDOW; dy <= WINDOW; dy++) {
                    if (Math.abs(dx) <= CROSS && Math.abs(dy) <= CROSS) {
                        continue;
                    }
                    final int argb = frame.pixel(x + dx, y + dy);
                    final int r = argb >> 16 & CHANNEL;
                    if (r > (argb >> 8 & CHANNEL) + REDDER && r > (argb & CHANNEL) + REDDER) {
                        red++;
                    }
                    count++;
                }
            }
        }
        return (double) red / count;
    }
}
