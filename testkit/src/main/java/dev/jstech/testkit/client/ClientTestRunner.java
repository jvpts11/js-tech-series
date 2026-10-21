/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.client;

import com.mojang.logging.LogUtils;
import dev.jstech.core.client.motion.MotionClock;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

/**
 * Runs the client tests when the game is started with {@code -Djstestkit.clienttests=true}: waits for the title
 * screen, makes a fresh flat creative world, runs this shard's slice of the tests every mod registered
 * ({@link ClientTests}) one at a time on the client's thread, writes the report and stops the game, with a code that
 * fails whatever started it when a test failed.
 *
 * <p>The run is told what to do by system properties: {@code jstestkit.clienttests.shard} and {@code .shards}, which
 * slice it runs of how many; {@code .only}, parts of names separated by commas, a test run when its name holds any
 * of them; and {@code .suites}, the suites to run. Each test owns a strip of the world 64 blocks from its neighbours,
 * by its number among every test, so shards never overlap; the player is moved there before its steps run.
 */
public final class ClientTestRunner {

    private State state = State.BOOT;
    private int stateTicks;
    /* Whether a name or a suite was asked for and matched nothing, which is a run that proves nothing. */
    private boolean askedForNothing;
    private final ClientTestReport report = new ClientTestReport();
    private List<ClientTests.Planned> tests;
    private int nextTest;
    private int groundY;
    private BlockPos spawn;
    private ClientTests.Planned current;
    private ClientTestHelper helper;
    private int stepIndex;
    private int stepDelayLeft;
    private int testTicks;
    private CompletableFuture<Void> pendingMove;
    /* The server's answer to whether the test's area is loaded, asked on the server's thread and read here. */
    private CompletableFuture<Boolean> areaProbe;

    /** The system property that runs the client tests, and the start of the others' names. */
    public static final String PROPERTY = "jstestkit.clienttests";
    /** The world the tests run in, made again for every run. */
    public static final String WORLD_NAME = "jstestkit-clienttests";

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean(PROPERTY);
    private static final int SHARD = Integer.getInteger(PROPERTY + ".shard", 0);
    private static final int SHARDS = Math.max(1, Integer.getInteger(PROPERTY + ".shards", 1));
    private static final String ONLY = System.getProperty(PROPERTY + ".only", "").trim();
    private static final String SUITES = System.getProperty(PROPERTY + ".suites", "").trim();
    private static final int AREA_SPACING = 64;
    private static final int SETTLE_TICKS = 40;
    private static final int AREA_TICKS = 600;
    /* How many frames of each thread's stack a timed-out wait writes to the log. */
    private static final int DUMPED_FRAMES = 40;
    private static final int GUI_SCALE = 2;
    private static final long NOON = 6000L;
    private static final ClientTestRunner INSTANCE = new ClientTestRunner();

    private ClientTestRunner() {
    }

    /** Hands the game's bus the client's ticks, once, from the kit's client constructor; a no-op unless asked for. */
    @ApiStatus.Internal
    public static void listen() {
        if (ENABLED) {
            NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> INSTANCE.tick(Minecraft.getInstance()));
        }
    }

    /** Whether this game runs the client tests. */
    public static boolean enabled() {
        return ENABLED;
    }

    /**
     * The suites a list of names separated by commas asks for, in order; none asks for every suite.
     *
     * @throws IllegalArgumentException when a name is no suite registered, so a mistyped one stops the run instead
     *                                  of running a smaller battery that reads as passed
     */
    public static Set<String> asked(final String list, final Set<String> registered) {
        final Set<String> suites = new LinkedHashSet<>();
        for (final String part : list.split(",")) {
            final String name = part.strip().toLowerCase(Locale.ROOT);
            if (name.isEmpty()) {
                continue;
            }
            if (!registered.contains(name)) {
                throw new IllegalArgumentException("there is no client test suite named \"" + name
                        + "\"; the suites are " + String.join(", ", registered));
            }
            suites.add(name);
        }
        return suites;
    }

    private void tick(final Minecraft mc) {
        stateTicks++;
        try {
            switch (state) {
                case BOOT -> boot(mc);
                case WAIT_LEVEL -> waitLevel(mc);
                case PREPARE -> prepare(mc);
                case NEXT_TEST -> nextTest(mc);
                case MOVE_TO_AREA -> moveToArea(mc);
                case RUNNING -> running();
                case FINISH -> finish(mc);
                case DONE -> {
                }
            }
        } catch (final RuntimeException e) {
            if ((state == State.RUNNING || state == State.MOVE_TO_AREA) && current != null) {
                // A test whose area never loads, or whose steps fail to build, fails under its own name.
                pendingMove = null;
                areaProbe = null;
                failCurrent(e);
            } else {
                LOGGER.error("{} the runner failed in its state {}", ClientTestReport.MARK, state, e);
                report.record(new ClientTestReport.Result("runner." + state, false, String.valueOf(e), List.of(),
                        stateTicks));
                enter(State.FINISH);
            }
        }
    }

    private void enter(final State next) {
        state = next;
        stateTicks = 0;
    }

    private void boot(final Minecraft mc) {
        /*
         * A shard's game folder is fresh, so the game would show the accessibility onboarding screen and would pause
         * whenever its window loses focus, and several shards' windows run side by side, so at most one of them is
         * focused. Both are settled before anything else.
         */
        if (mc.options.pauseOnLostFocus || mc.options.onboardAccessibility) {
            mc.options.pauseOnLostFocus = false;
            mc.options.onboardAccessibility = false;
            mc.options.save();
        }
        if (mc.screen instanceof AccessibilityOnboardingScreen onboarding) {
            onboarding.onClose();
            return;
        }
        if (!(mc.screen instanceof TitleScreen) || mc.getOverlay() != null) {
            return;
        }
        final Map<String, Integer> durations = ClientTestDurations.read(runDirectory(mc));
        final List<String> needles = Arrays.stream(ONLY.toLowerCase(Locale.ROOT).split(","))
                .map(String::trim).filter(needle -> !needle.isEmpty()).toList();
        Set<String> suites;
        try {
            suites = asked(SUITES, ClientTests.suites());
        } catch (final IllegalArgumentException wrongName) {
            // A mistyped suite fails the run below instead of running a smaller battery that reads as passed.
            LOGGER.error("{} {}", ClientTestReport.MARK, wrongName.getMessage());
            suites = null;
        }
        final Set<String> askedSuites = suites;
        // The names are matched before the tests are split, so the few asked for are shared out over the shards.
        final List<ClientTests.Planned> chosen = askedSuites == null ? List.of() : ClientTests.all().stream()
                .filter(test -> askedSuites.isEmpty() || askedSuites.contains(test.suite().name()))
                .filter(test -> needles.isEmpty()
                        || needles.stream().anyMatch(test.name().toLowerCase(Locale.ROOT)::contains))
                .toList();
        tests = ClientTestShards.shard(chosen, SHARD, SHARDS, durations, ClientTests.Planned::name);
        LOGGER.info("{} shard {}/{} split {}, expected to take {} ticks", ClientTestReport.MARK, SHARD, SHARDS,
                durations.isEmpty() ? "round-robin" : "by the ticks each test took last time",
                ClientTestShards.expectedTicks(tests, durations, ClientTests.Planned::name));
        /*
         * A name that matches nothing is a mistake, not an empty battery: whoever typed it meant to run something. With
         * more shards than tests asked for, a shard can be left with none, which is no mistake.
         */
        if (askedSuites == null || ((!needles.isEmpty() || !askedSuites.isEmpty()) && chosen.isEmpty())) {
            this.askedForNothing = true;
            LOGGER.error("{} no test is in the suites \"{}\" and named like \"{}\"", ClientTestReport.MARK, SUITES,
                    ONLY);
        }
        LOGGER.info("{} shard {}/{} runs {} tests", ClientTestReport.MARK, SHARD, SHARDS, tests.size());
        deleteOldWorld(mc);
        final GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        rules.getRule(GameRules.RULE_DO_IMMEDIATE_RESPAWN).set(true, null);
        final LevelSettings settings = new LevelSettings(WORLD_NAME, GameType.CREATIVE, false, Difficulty.PEACEFUL,
                true, rules, WorldDataConfiguration.DEFAULT);
        final WorldOptions options = new WorldOptions(0L, false, false);
        mc.createWorldOpenFlows().createFreshLevel(WORLD_NAME, settings, options,
                registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                        .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                mc.screen);
        enter(State.WAIT_LEVEL);
    }

    private void deleteOldWorld(final Minecraft mc) {
        final Path folder = mc.getLevelSource().getBaseDir().resolve(WORLD_NAME);
        if (!Files.exists(folder)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(folder)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (final IOException e) {
                    throw new IllegalStateException("could not delete " + path, e);
                }
            });
        } catch (final IOException e) {
            throw new IllegalStateException("could not clear the old test world", e);
        }
    }

    private void waitLevel(final Minecraft mc) {
        final MinecraftServer server = mc.getSingleplayerServer();
        if (mc.screen instanceof PauseScreen) {
            // The window lost focus before the option above took effect.
            mc.setScreen(null);
        }
        if (mc.level == null || mc.player == null || mc.screen != null || server == null || !server.isReady()) {
            return;
        }
        if (stateTicks >= SETTLE_TICKS) {
            enter(State.PREPARE);
        }
    }

    private void prepare(final Minecraft mc) {
        // Screens must not pause when a window loses focus, and a fixed GUI scale keeps points and pictures the same.
        mc.options.pauseOnLostFocus = false;
        mc.options.guiScale().set(GUI_SCALE);
        mc.resizeDisplay();
        final MinecraftServer server = serverOf(mc);
        pendingMove = server.submit(() -> {
            final ServerLevel level = server.overworld();
            level.setDayTime(NOON);
            spawn = level.getSharedSpawnPos();
            groundY = level.getHeight(Heightmap.Types.WORLD_SURFACE, spawn.getX(), spawn.getZ());
        });
        nextTest = 0;
        enter(State.NEXT_TEST);
    }

    private void nextTest(final Minecraft mc) {
        if (pendingMove != null && !pendingMove.isDone()) {
            return;
        }
        pendingMove = null;
        if (mc.screen != null) {
            mc.setScreen(null);
        }
        if (nextTest >= tests.size()) {
            enter(State.FINISH);
            return;
        }
        current = tests.get(nextTest++);
        // Areas lie along x by the test's number among every test; y keeps a GameTest's rule, y 2 on the ground.
        final BlockPos origin = new BlockPos(spawn.getX() + current.index() * AREA_SPACING, groundY - 2,
                spawn.getZ());
        helper = current.suite().helpers().create(mc, current, origin);
        stepIndex = 0;
        testTicks = 0;
        final MinecraftServer server = serverOf(mc);
        pendingMove = server.submit(() -> {
            /*
             * A window the last test left open is closed on the server too: closing its screen above told the server
             * nothing, and a test that waits for the server to close its own window would wait for that one.
             */
            final ServerPlayer player = helper.serverPlayer();
            if (player.containerMenu != player.inventoryMenu) {
                player.closeContainer();
            }
            helper.teleport(new BlockPos(3, 2, 8), Direction.NORTH);
        });
        enter(State.MOVE_TO_AREA);
    }

    private void moveToArea(final Minecraft mc) {
        if (!pendingMove.isDone()) {
            return;
        }
        pendingMove.join();
        final MinecraftServer server = mc.getSingleplayerServer();
        final BlockPos probe = helper.abs(new BlockPos(3, 2, 3));
        // The server's level is read on the server's thread; only the client's level is read here.
        if (areaProbe == null && server != null) {
            areaProbe = server.submit(() -> server.overworld().isLoaded(probe));
        }
        final boolean loaded = areaProbe != null && areaProbe.isDone() && areaProbe.join()
                && mc.level != null && mc.level.isLoaded(probe);
        if (areaProbe != null && areaProbe.isDone() && !loaded) {
            areaProbe = null;
        }
        if (!loaded || stateTicks < 10) {
            if (stateTicks > AREA_TICKS) {
                throw new ClientTestFailure("the test's area never loaded");
            }
            return;
        }
        areaProbe = null;
        pendingMove = null;
        // Every test starts from what a fresh game remembers: what a mod keeps between tests is put back first.
        ClientTests.resets().forEach(Runnable::run);
        /*
         * And with motion reduced, as a player can ask for: a window that grows in for a quarter of a second would
         * stand somewhere else at the moment a test clicks it. A test of the motion itself lets it move again.
         */
        MotionClock.setReduced(true);
        LOGGER.info("{} running {} at {}", ClientTestReport.MARK, current.name(), helper.origin());
        try {
            current.test().method().invoke(null, helper);
        } catch (final IllegalAccessException | InvocationTargetException e) {
            throw new ClientTestFailure("could not build the test's steps", e.getCause() != null ? e.getCause() : e);
        }
        stepIndex = 0;
        stepDelayLeft = helper.steps().isEmpty() ? 0 : helper.steps().getFirst().delay();
        testTicks = 0;
        enter(State.RUNNING);
    }

    private void running() {
        testTicks++;
        if (testTicks > current.test().timeoutTicks()) {
            throw new ClientTestFailure("test timed out after " + current.test().timeoutTicks() + " ticks at step "
                    + stepIndex + " (" + stepLabel() + ")");
        }
        final List<ClientTestHelper.Queued> steps = helper.steps();
        if (stepIndex >= steps.size()) {
            helper.runAfterTest();
            report.record(new ClientTestReport.Result(current.name(), true, "", helper.screenshotsTaken(),
                    testTicks));
            current = null;
            enter(State.NEXT_TEST);
            return;
        }
        if (stepDelayLeft > 0) {
            stepDelayLeft--;
            return;
        }
        if (steps.get(stepIndex).step().tick()) {
            stepIndex++;
            stepDelayLeft = stepIndex < steps.size() ? steps.get(stepIndex).delay() : 0;
        }
    }

    private String stepLabel() {
        final List<ClientTestHelper.Queued> steps = helper.steps();
        return stepIndex < steps.size() ? steps.get(stepIndex).label() : "end";
    }

    private void failCurrent(final RuntimeException e) {
        final String where = " [step " + stepIndex + " " + stepLabel() + "]";
        LOGGER.error("{} {} failed{}", ClientTestReport.MARK, current.name(), where, e);
        if (e.getMessage() != null && e.getMessage().contains("timed out")) {
            dumpServerThreads();
        }
        try {
            helper.screenshot("FAILED");
        } catch (final RuntimeException ignored) {
            // A screenshot is a courtesy; the failure itself is what matters.
        }
        // Whatever the test changed outside its own area is put back, so a failure leaks nothing into later tests.
        helper.runAfterTest();
        final Throwable cause = e instanceof ClientTestFailure && e.getCause() != null ? e.getCause() : e;
        report.record(new ClientTestReport.Result(current.name(), false,
                (e.getMessage() == null ? cause.toString() : e.getMessage()) + where, helper.screenshotsTaken(),
                testTicks));
        current = null;
        enter(State.NEXT_TEST);
    }

    /*
     * Ends the run, and says so with the one thing a build listens to: a failed test, or a name that matched nothing,
     * ends it with a code that fails whatever started it, straight out rather than through the usual shutdown, which
     * comes back saying everything was fine. The report is written already, and the world this leaves is the test
     * world the next run deletes anyway.
     */
    private void finish(final Minecraft mc) {
        final Path file = mc.gameDirectory.toPath().resolve("clienttests").resolve("report.txt");
        report.write(file, SHARD, SHARDS);
        ClientTestDurations.record(runDirectory(mc), report.results());
        enter(State.DONE);
        if (this.askedForNothing || report.failures() > 0) {
            LOGGER.error("{} this run failed: {} of {} tests failed{}", ClientTestReport.MARK, report.failures(),
                    report.total(), this.askedForNothing ? ", and what was asked for matched no test" : "");
            System.exit(1);
        }
        mc.stop();
    }

    private static MinecraftServer serverOf(final Minecraft mc) {
        final MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) {
            throw new IllegalStateException("the integrated server vanished");
        }
        return server;
    }

    /*
     * Where every shard's game folder is, and the durations the shards share. The game's folder is "." in a run, so it
     * is made whole before its parent is taken: the parent of "shard-0/." is the shard itself.
     */
    private static Path runDirectory(final Minecraft mc) {
        return mc.gameDirectory.toPath().toAbsolutePath().normalize().getParent();
    }

    /*
     * A wait that timed out may be the server's chunks held up, which shows nowhere else: the stacks of the server's
     * threads and of the game's workers are written to the log, for whoever reads the failure.
     */
    private static void dumpServerThreads() {
        final StringBuilder out = new StringBuilder(ClientTestReport.MARK).append(
                " the server's threads as the wait timed out:");
        for (final Map.Entry<Thread, StackTraceElement[]> each : Thread.getAllStackTraces().entrySet()) {
            final String name = each.getKey().getName();
            if (!name.startsWith("Server thread") && !name.startsWith("Worker-") && !name.startsWith("IO-Worker")) {
                continue;
            }
            out.append(System.lineSeparator()).append('"').append(name).append("\" ").append(each.getKey().getState());
            final StackTraceElement[] stack = each.getValue();
            for (int i = 0; i < Math.min(DUMPED_FRAMES, stack.length); i++) {
                out.append(System.lineSeparator()).append("    at ").append(stack[i]);
            }
        }
        LOGGER.error(out.toString());
    }

    /* Where the run stands: from the title screen to the last test, and done. */
    private enum State { BOOT, WAIT_LEVEL, PREPARE, NEXT_TEST, MOVE_TO_AREA, RUNNING, FINISH, DONE }
}
