/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.region.Box;
import dev.jstech.space.api.Propellants;
import dev.jstech.space.config.SpaceServerConfig;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.flight.FlightPage;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.launch.LaunchCount;
import dev.jstech.space.launch.LaunchPanel;
import dev.jstech.space.launch.LaunchPanelBlockEntity;
import dev.jstech.space.launcher.LauncherBlocks;
import dev.jstech.space.launcher.LauncherDeckBlockEntity;
import dev.jstech.space.launcher.LauncherLayout;
import dev.jstech.space.pad.LaunchPads;
import dev.jstech.space.pad.PadBlocks;
import dev.jstech.space.pad.PadServicePortBlockEntity;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.Rockets;
import dev.jstech.space.rocket.SkeletonPropellants;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.space.rocket.TankLoad;
import dev.jstech.tests.JsTests;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A pad's launch count, kept by the launcher on it and worked from a Launch Panel, from the rocket's own flight
 * computer, or both: with no rocket on the pad, or in a thunderstorm, it will not start; with its key and its ignition
 * armed and all go, it runs from T-60 on the panel and on the rocket's clock, holds at T-6 while LAUNCH has not been
 * pressed, runs on once it is, and releases the rocket at T-0, standing down once the rocket is gone, free for the
 * next. A pad with no panel counts from the capsule alone, its computer open on the LAUNCH page and the rocket its who
 * pressed LAUNCH there; with a panel, what is done on either shows on both and either holds it. A rocket too heavy for
 * its engines, with the server keeping such engines unlit, is scrubbed at ignition with its tank still full.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class LaunchPanelGameTests {

    /* A wide arena, the panel in its middle, so what it looks for round it is all its own test's. */
    private static final String WIDE = "bench";
    private static final String BATCH = "jsspace_launch_panel";
    private static final BlockPos PANEL = new BlockPos(24, 1, 24);
    /* The launcher's measuring arm, clear of the rocket's foot, which its axis meets three blocks on each way. */
    private static final BlockPos ARM = new BlockPos(25, 3, 25);
    private static final BlockPos TO_AXIS = new BlockPos(3, 0, 3);
    private static final BlockPos COMPUTER = new BlockPos(28, 7, 28);
    private static final int SECOND = LaunchCount.SECOND;
    /* The flight computer's keys: down the left from the top, then down the right; and its launch page's key. */
    private static final int L1 = FlightComputerBlockEntity.LEFT;
    private static final int L2 = FlightComputerBlockEntity.LEFT + 1;
    private static final int L5 = FlightComputerBlockEntity.LEFT + 4;
    private static final int R1 = FlightComputerBlockEntity.RIGHT;
    private static final int R5 = FlightComputerBlockEntity.RIGHT + 4;
    private static final int LAUNCH_PAGE = FlightComputerBlockEntity.TOP + FlightPage.KEYS.indexOf(FlightPage.LAUNCH);
    /*
     * What a layer of the small tank holds, in kilograms; and a tank so tall that, full, the rocket outweighs what its
     * one sea-level engine pushes.
     */
    private static final double FULL_LAYER = 5_800.0;
    private static final int HEAVY_LAYERS = 40;
    /* Of what a tank held at LAUNCH, the least it holds a few ticks on with nothing burnt, its boil-off aside. */
    private static final double KEPT = 0.999;
    /* The batch whose test keeps a too heavy rocket's engines off, a server setting put back after it. */
    private static final String HEAVY = BATCH + "_heavy";
    /* Whether the server kept a too heavy rocket's engines off before that batch began. */
    private static volatile boolean tooHeavyToLift = true;

    private LaunchPanelGameTests() {
    }

    @BeforeBatch(batch = HEAVY)
    public static void heavyStart(final ServerLevel level) {
        tooHeavyToLift = SpaceServerConfig.FILE.get(SpaceServerConfig.TOO_HEAVY_TO_LIFT);
    }

    @AfterBatch(batch = HEAVY)
    public static void heavyEnd(final ServerLevel level) {
        SpaceServerConfig.FILE.set(SpaceServerConfig.TOO_HEAVY_TO_LIFT, tooHeavyToLift);
    }

    /**
     * A pad goes by the number its world gave its service port, and by a name once it is given one from the panel's
     * plate: kept on the port, spaces run into one, never longer than a plate's line; no name gives it its number back.
     */
    @GameTest(template = WIDE, batch = BATCH + "_safe", timeoutTicks = 200)
    public static void pad_isNumberedByItsWorldAndNamedFromThePanel(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(PANEL, LaunchPanel.BLOCK.get());
        rocket(helper);
        final BlockPos panel = helper.absolutePos(PANEL);
        final BlockPos axis = LaunchPads.standBy(level, panel, LaunchPanelBlockEntity.REACH,
                LaunchPanelBlockEntity.BELOW, LaunchPanelBlockEntity.ABOVE).orElseThrow().axis();
        // On the ground by the pad, as the launch pad's own port stands.
        final BlockPos port = axis.below(LauncherLayout.RAISE).offset(3, 0, 3);
        level.setBlockAndUpdate(port, PadBlocks.PAD_SERVICE_PORT.get().defaultBlockState());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(shown(level, panel).padNumber() > 0,
                        "the panel reads its pad's number"))
                .thenExecute(() -> {
                    final PadServicePortBlockEntity pad = (PadServicePortBlockEntity) level.getBlockEntity(port);
                    final LaunchPanelBlockEntity console = panel(level, panel);
                    helper.assertTrue(pad != null && console.shown().padNumber() == pad.number()
                            && console.shown().padName().isEmpty(), "the number its world gave the pad's port, no"
                            + " name yet");
                    helper.assertTrue(console.namePad("  Pad   39A "), "the plate names the pad");
                    helper.assertValueEqual(pad.name(), "Pad 39A", "kept on its port, its spaces run into one");
                    helper.assertValueEqual(console.shown().padName(), "Pad 39A", "and shown on the plate");
                    final String wordy = "A name far too long for any pad's plate to hold";
                    console.namePad(wordy);
                    helper.assertTrue(pad.name().length() <= PadServicePortBlockEntity.MOST_NAME
                            && pad.name().length() > PadServicePortBlockEntity.MOST_NAME / 2
                            && wordy.startsWith(pad.name()), "never longer than a plate's line, cut from its"
                            + " end: " + pad.name());
                    console.namePad("");
                    helper.assertTrue(pad.name().isEmpty() && console.shown().padName().isEmpty(),
                            "and no name gives the pad its number back");
                })
                .thenSucceed();
    }

    /**
     * The ignition is armed only under its guard lifted, and shutting the guard pushes it back down; the flight
     * computer's one key for both lifts the guard and arms, and makes both safe again.
     */
    @GameTest(template = WIDE, batch = BATCH + "_safe")
    public static void guard_shutDisarmsTheIgnitionUnderIt(final GameTestHelper helper) {
        final LaunchCount count = launcher(helper);
        count.press(LaunchCount.IGNITION);
        helper.assertFalse(count.shown().ignition(), "with its guard down the ignition cannot be armed");
        count.press(LaunchCount.GUARD);
        count.press(LaunchCount.IGNITION);
        helper.assertTrue(count.shown().guard() && count.shown().ignition(), "the guard lifted, the ignition arms");
        count.press(LaunchCount.GUARD);
        helper.assertTrue(!count.shown().guard() && !count.shown().ignition(),
                "shutting the guard disarms the ignition");
        count.press(LaunchCount.SWITCH);
        helper.assertTrue(count.shown().guard() && count.shown().ignition(), "the one key lifts the guard and arms");
        count.press(LaunchCount.SWITCH);
        helper.assertTrue(!count.shown().guard() && !count.shown().ignition(), "and makes both safe again");
        helper.succeed();
    }

    @GameTest(template = WIDE, batch = BATCH + "_safe", timeoutTicks = 200)
    public static void panel_willNotCountWithoutARocketOrInAStorm(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(PANEL, LaunchPanel.BLOCK.get());
        launcher(helper);
        final BlockPos panel = helper.absolutePos(PANEL);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(panel(level, panel).count().isPresent(),
                        "the panel finds the launcher on its pad"))
                .thenExecute(() -> {
                    final LaunchPanelBlockEntity console = panel(level, panel);
                    arm(console);
                    console.press(LaunchCount.RUN);
                    helper.assertTrue(!console.shown().vehicle() && console.shown().phase() == LaunchCount.Phase.SAFE,
                            "with no rocket on the pad the count will not start");
                    rocket(helper);
                })
                .thenWaitUntil(() -> helper.assertTrue(shown(level, panel).go(), "a fuelled rocket on the pad is go"))
                .thenExecute(() -> {
                    level.setWeatherParameters(0, 6000, true, true);
                    level.setRainLevel(1.0F);
                    level.setThunderLevel(1.0F);
                })
                .thenWaitUntil(() -> helper.assertTrue(shown(level, panel).storm(), "it sees the thunderstorm"))
                .thenExecute(() -> {
                    final LaunchPanelBlockEntity console = panel(level, panel);
                    console.press(LaunchCount.RUN);
                    level.setWeatherParameters(6000, 0, false, false);
                    level.setThunderLevel(0.0F);
                    level.setRainLevel(0.0F);
                    helper.assertTrue(console.shown().phase() == LaunchCount.Phase.SAFE,
                            "and in a thunderstorm the count will not start");
                })
                .thenSucceed();
    }

    @GameTest(template = WIDE, batch = BATCH + "_count", timeoutTicks = 2000)
    public static void panel_countsDownHoldsForLaunchAndReleases(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        fairWeather(level);
        helper.setBlock(PANEL, LaunchPanel.BLOCK.get());
        rocket(helper);
        final BlockPos panel = helper.absolutePos(PANEL);
        final BlockPos computer = helper.absolutePos(COMPUTER);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(shown(level, panel).go(), "a fuelled rocket on the pad is go"))
                .thenExecute(() -> {
                    final LaunchPanelBlockEntity console = panel(level, panel);
                    arm(console);
                    console.press(LaunchCount.RUN);
                    helper.assertTrue(console.shown().phase() == LaunchCount.Phase.COUNTING
                            && console.shown().left() == LaunchCount.START * SECOND, "the count starts at T-60");
                })
                .thenIdle(2 * SECOND)
                .thenExecute(() -> helper.assertTrue(level.getBlockEntity(computer) instanceof FlightComputerBlockEntity
                        rocket && rocket.clock() < 0 && rocket.clock() > -LaunchCount.START,
                        "the count runs on the rocket's clock too"))
                .thenWaitUntil(() -> helper.assertTrue(shown(level, panel).phase() == LaunchCount.Phase.HOLDING,
                        "without LAUNCH pressed it holds"))
                .thenExecute(() -> {
                    final LaunchPanelBlockEntity console = panel(level, panel);
                    helper.assertTrue(console.shown().left() == LaunchCount.LIGHT * SECOND
                            && console.shown().coverUp(), "at T-6, the LAUNCH button's cover up: "
                            + console.shown().left());
                    console.press(LaunchCount.LAUNCH);
                    helper.assertTrue(console.shown().phase() == LaunchCount.Phase.COUNTING,
                            "LAUNCH pressed, the count runs on");
                })
                .thenWaitUntil(() -> helper.assertTrue(shown(level, panel).phase() == LaunchCount.Phase.RELEASED,
                        "and at T-0 the rocket is released"))
                // Once the rocket has climbed off and is gone from this world, the count is free for the next launch.
                .thenIdle(2 * SECOND)
                .thenExecute(() -> takeAway(helper))
                .thenWaitUntil(() -> helper.assertTrue(shown(level, panel).phase() == LaunchCount.Phase.SAFE,
                        "its rocket gone, the count stands down"))
                .thenSucceed();
    }

    /**
     * A pad with no Launch Panel: its rocket's flight computer opens on the LAUNCH page, which works the launcher's
     * count alone, from arming to release; the rocket is the one's who pressed LAUNCH there, and once it is off the
     * pad the page closes, the transfer open in its place.
     */
    @GameTest(template = WIDE, batch = BATCH + "_count", timeoutTicks = 2000)
    public static void capsule_countsAloneWithNoPanelAndWhoeverLaunchesOwnsTheRocket(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        fairWeather(level);
        rocket(helper);
        final BlockPos computer = helper.absolutePos(COMPUTER);
        final ServerPlayer crew = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "launch_crew"));
        final MovingStructure[] released = new MovingStructure[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(computer(level, computer).page() == FlightPage.LAUNCH
                        && computer(level, computer).launch().map(count -> count.shown().go()).orElse(false),
                        "on the launcher, the computer opens on its LAUNCH page, all go"))
                .thenExecute(() -> {
                    final FlightComputerBlockEntity rocket = computer(level, computer);
                    helper.assertFalse(rocket.launch().map(count -> count.shown().panel()).orElse(true),
                            "no panel works the count");
                    rocket.press(L1, crew);
                    rocket.press(L2, crew);
                    rocket.press(R1, crew);
                    helper.assertTrue(phaseOf(rocket) == LaunchCount.Phase.COUNTING,
                            "the key and the ignition armed from the capsule, COUNT starts the count");
                })
                .thenWaitUntil(() -> helper.assertTrue(phaseOf(computer(level, computer))
                        == LaunchCount.Phase.HOLDING, "without LAUNCH pressed it holds at T-6"))
                .thenExecute(() -> {
                    computer(level, computer).press(R5, crew);
                    helper.assertTrue(phaseOf(computer(level, computer)) == LaunchCount.Phase.COUNTING,
                            "LAUNCH pressed in the capsule, the count runs on");
                })
                .thenWaitUntil(() -> {
                    released[0] = released(helper);
                    helper.assertTrue(released[0] != null && released[0].ready(),
                            "at T-0 the rocket is released, its body standing ready");
                })
                .thenExecute(() -> {
                    helper.assertValueEqual(Rockets.get(level.getServer(), released[0].id())
                            .map(rocket -> rocket.ownership().owner()).orElse(null), crew.getUUID(),
                            "the rocket is its who pressed LAUNCH inside");
                    final FlightComputerBlockEntity aboard = aboard(level, released[0]);
                    helper.assertTrue(aboard != null && aboard.page() == FlightPage.TRANSFER
                            && aboard.launch().isEmpty(), "off the pad its LAUNCH page closes for the transfer");
                    takeAway(helper);
                })
                .thenSucceed();
    }

    /**
     * A pad with a Launch Panel and a crew aboard: one count, which the LAUNCH page shows worked from the panel too;
     * the key turned at the panel and the ignition armed inside start it from either, a hold from the panel shows in
     * the capsule and COUNT inside runs it on.
     */
    @GameTest(template = WIDE, batch = BATCH + "_safe", timeoutTicks = 400)
    public static void count_isOneForThePanelAndTheCapsule(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        fairWeather(level);
        helper.setBlock(PANEL, LaunchPanel.BLOCK.get());
        rocket(helper);
        final BlockPos panel = helper.absolutePos(PANEL);
        final BlockPos computer = helper.absolutePos(COMPUTER);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(computer(level, computer).launch()
                        .map(count -> count.shown().go() && count.shown().panel()).orElse(false),
                        "the LAUNCH page shows the count all go, a panel working it too"))
                .thenExecute(() -> {
                    final LaunchPanelBlockEntity console = panel(level, panel);
                    final FlightComputerBlockEntity rocket = computer(level, computer);
                    helper.assertTrue(console.count().isPresent() && console.count().equals(rocket.launch()),
                            "the panel and the capsule work the very same count");
                    console.press(LaunchCount.MASTER);
                    rocket.press(L2);
                    helper.assertTrue(rocket.launch().map(count -> count.shown().master()).orElse(false)
                            && console.shown().ignition(), "what is armed on either shows on both");
                    rocket.press(R1);
                    helper.assertTrue(console.shown().phase() == LaunchCount.Phase.COUNTING,
                            "COUNT in the capsule starts the panel's count");
                    console.press(LaunchCount.HOLD);
                    helper.assertTrue(phaseOf(rocket) == LaunchCount.Phase.HOLDING,
                            "HOLD on the panel holds the capsule's count");
                    rocket.press(R1);
                    helper.assertTrue(console.shown().phase() == LaunchCount.Phase.COUNTING,
                            "and COUNT inside runs it on");
                    rocket.press(L5);
                    helper.assertTrue(console.shown().phase() == LaunchCount.Phase.HOLDING
                            && console.shown().holds() == 2, "HOLD inside holds it too, each hold counted once");
                })
                .thenSucceed();
    }

    /** The LAUNCH page opens only on a launcher: off one its key does nothing. */
    @GameTest(template = WIDE, batch = BATCH + "_safe")
    public static void launchPage_opensOnlyOnALauncher(final GameTestHelper helper) {
        helper.setBlock(COMPUTER, RocketKitBlocks.FLIGHT_COMPUTER.get());
        final FlightComputerBlockEntity rocket = computer(helper.getLevel(), helper.absolutePos(COMPUTER));
        rocket.press(LAUNCH_PAGE);
        helper.assertTrue(rocket.page() != FlightPage.LAUNCH && rocket.launch().isEmpty(),
                "with no launcher under it, the LAUNCH key opens nothing");
        helper.succeed();
    }

    @GameTest(template = WIDE, batch = HEAVY, timeoutTicks = 1700)
    public static void panel_scrubsATooHeavyRocketUnlitWhenTheServerKeepsItsEnginesOff(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        fairWeather(level);
        helper.setBlock(PANEL, LaunchPanel.BLOCK.get());
        rocket(helper, HEAVY_LAYERS);
        final BlockPos panel = helper.absolutePos(PANEL);
        final BlockPos computer = helper.absolutePos(COMPUTER);
        final double[] held = new double[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(shown(level, panel).go(), "a fuelled rocket on the pad is go"))
                .thenExecute(() -> {
                    final LaunchPanelBlockEntity console = panel(level, panel);
                    arm(console);
                    console.press(LaunchCount.RUN);
                })
                .thenWaitUntil(() -> helper.assertTrue(shown(level, panel).phase() == LaunchCount.Phase.HOLDING,
                        "the count holds at T-6 for LAUNCH"))
                .thenExecute(() -> {
                    held[0] = tank(level, computer);
                    SpaceServerConfig.FILE.set(SpaceServerConfig.TOO_HEAVY_TO_LIFT, false);
                    panel(level, panel).press(LaunchCount.LAUNCH);
                })
                .thenWaitUntil(() -> helper.assertTrue(shown(level, panel).phase() == LaunchCount.Phase.SCRUBBED,
                        "too heavy to lift, the launch is scrubbed"))
                .thenExecute(() -> {
                    SpaceServerConfig.FILE.set(SpaceServerConfig.TOO_HEAVY_TO_LIFT, tooHeavyToLift);
                    final double left = tank(level, computer);
                    helper.assertTrue(left >= held[0] * KEPT, "its engines never lit: the rocket stands on the pad "
                            + "and its tank holds what it held, but for what boils off: " + left + " of " + held[0]);
                })
                .thenSucceed();
    }

    /* Fair weather for the whole count, whatever the sky was doing. */
    private static void fairWeather(final ServerLevel level) {
        level.setWeatherParameters(6000, 0, false, false);
        level.setThunderLevel(0.0F);
        level.setRainLevel(0.0F);
    }

    /* Turns the key and arms the ignition under its guard. */
    private static void arm(final LaunchPanelBlockEntity console) {
        console.press(LaunchCount.MASTER);
        console.press(LaunchCount.GUARD);
        console.press(LaunchCount.IGNITION);
    }

    /* The rocket the pad's count released taken out of the world, as one gone on to orbit is. */
    private static void takeAway(final GameTestHelper helper) {
        final MovingStructure body = released(helper);
        helper.assertTrue(body != null, "the released rocket is a body over the pad");
        MovingStructures.takeAway(body);
    }

    /* The rocket the pad's count released, while it is in this world; else null. */
    private static MovingStructure released(final GameTestHelper helper) {
        return LauncherDeckBlockEntity.countAt(helper.getLevel(), helper.absolutePos(ARM))
                .flatMap(LaunchCount::flight).map(id -> MovingStructures.byId(helper.getLevel(), id)).orElse(null);
    }

    /* A launcher's measuring arm by the panel, its deck formed; its count. */
    private static LaunchCount launcher(final GameTestHelper helper) {
        helper.setBlock(ARM, LauncherBlocks.HOLD_DOWN_ARM.get());
        if (helper.getLevel().getBlockEntity(helper.absolutePos(ARM)) instanceof LauncherDeckBlockEntity arm) {
            arm.deck(new LauncherDeckBlockEntity.Deck(Direction.EAST, 1, TO_AXIS, 6, 8, 6));
            return arm.count();
        }
        throw new GameTestAssertException("No hold-down arm at " + helper.absolutePos(ARM));
    }

    /* The launcher and, up its axis, a rocket's flight computer, its tank full. */
    private static void rocket(final GameTestHelper helper) {
        rocket(helper, 1);
    }

    /* The same, its tank {@code layers} layers tall. */
    private static void rocket(final GameTestHelper helper, final int layers) {
        final ServerLevel level = helper.getLevel();
        launcher(helper);
        helper.setBlock(COMPUTER, RocketKitBlocks.FLIGHT_COMPUTER.get());
        final RocketStack stack = RocketStack.of(new RocketStack.Part(SkeletonRocketParts.id("sea_level_engine_3"), 1),
                new RocketStack.Part(SkeletonRocketParts.id("tank_3"), layers),
                new RocketStack.Part(SkeletonRocketParts.id("capsule_3"), 1));
        if (level.getBlockEntity(helper.absolutePos(COMPUTER)) instanceof FlightComputerBlockEntity rocket) {
            rocket.carry(new RocketDesign("PANEL", stack), new BlockPos(0, -4, 0), List.of());
            Propellants.get(level, SkeletonPropellants.METHALOX).ifPresent(methalox -> rocket.load(1,
                    TankLoad.filled(SkeletonPropellants.METHALOX, methalox, FULL_LAYER * layers)));
        }
    }

    /* What the rocket's tank holds, in kilograms; nought with no rocket's flight computer there. */
    private static double tank(final ServerLevel level, final BlockPos computer) {
        return level.getBlockEntity(computer) instanceof FlightComputerBlockEntity rocket
                && rocket.tanks().get(1) != null ? rocket.tanks().get(1).total() : 0;
    }

    private static LaunchPanelBlockEntity panel(final ServerLevel level, final BlockPos at) {
        if (level.getBlockEntity(at) instanceof LaunchPanelBlockEntity panel) {
            return panel;
        }
        throw new GameTestAssertException("No launch panel at " + at);
    }

    /* What the panel at {@code at} shows of its launcher's count. */
    private static LaunchCount.Shown shown(final ServerLevel level, final BlockPos at) {
        return panel(level, at).shown();
    }

    /* The flight computer at {@code at}; a failed check once it is gone. */
    private static FlightComputerBlockEntity computer(final ServerLevel level, final BlockPos at) {
        if (level.getBlockEntity(at) instanceof FlightComputerBlockEntity rocket) {
            return rocket;
        }
        throw new GameTestAssertException("No flight computer at " + at);
    }

    /* Where the count its launch page works is, or null off a launcher. */
    private static LaunchCount.Phase phaseOf(final FlightComputerBlockEntity rocket) {
        return rocket.launch().map(LaunchCount::phase).orElse(null);
    }

    /* The flight computer aboard {@code body}, in its yard; null if none is. */
    private static FlightComputerBlockEntity aboard(final ServerLevel level, final MovingStructure body) {
        final Box box = body.bounds();
        for (final BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                box.maxZ())) {
            if (level.getBlockEntity(pos) instanceof FlightComputerBlockEntity computer) {
                return computer;
            }
        }
        return null;
    }
}
