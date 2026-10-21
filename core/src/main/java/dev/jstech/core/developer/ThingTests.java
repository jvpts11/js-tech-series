/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer;

import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.MultipleTestTracker;
import net.minecraft.gametest.framework.RetryOptions;
import net.minecraft.gametest.framework.StructureGridSpawner;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.ApiStatus;

/**
 * Runs, near a player, the GameTests that name a thing ({@link StudioTests}), laid out in rows as the game's own
 * {@code /test} lays them, and tells the player how many passed once the last is done. Only for a player who may use
 * the developer's tools that change the world ({@link DeveloperMode#mayUseWorldTools}).
 */
@TextHolder
public final class ThingTests {

    public static final TextKey NOT_ALLOWED = TextKey.of("commands.jscore.developer.not_allowed",
            "Only an operator uses the developer's tools that read or change the world, while the world's developer "
                    + "mode is on");
    /* The thing. */
    public static final TextKey NONE = TextKey.of("jscore.studio.tests.none",
            "No GameTest names %s, or the GameTests are not on in this run");
    /* How many, and the thing. */
    public static final TextKey RUNNING = TextKey.of("jscore.studio.tests.running", "Running %s GameTests of %s");
    /* The thing, how many passed and how many ran. */
    public static final TextKey PASSED = TextKey.of("jscore.studio.tests.passed",
            "The GameTests of %s: %s of %s passed");

    private static final String VERSION = "1";
    /** How many tests stand side by side before the next row, and how far ahead of the player the first stands. */
    private static final int TESTS_PER_ROW = 8;
    private static final int AHEAD = 3;

    private ThingTests() {
    }

    /** Hands the bus what this answers, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar(VERSION)
                .playToServer(RunThingTestsPayload.TYPE, RunThingTestsPayload.STREAM_CODEC, ThingTests::onRun));
    }

    /**
     * Runs the GameTests that name {@code thing} near {@code player}, and how many it started: none when the player may
     * not, or no test names it, each said to them.
     */
    public static int run(final ServerPlayer player, final ResourceLocation thing) {
        if (!DeveloperMode.mayUseWorldTools(player)) {
            player.sendSystemMessage(GameText.component(NOT_ALLOWED));
            return 0;
        }
        final Map<String, TestFunction> byName = new LinkedHashMap<>();
        for (final TestFunction test : GameTestRegistry.getAllTestFunctions()) {
            byName.put(test.testName(), test);
        }
        final List<String> names = StudioTests.naming(byName.keySet(), thing.getPath());
        if (names.isEmpty()) {
            player.sendSystemMessage(GameText.component(NONE.with(thing.toString())));
            return 0;
        }
        final ServerLevel level = player.serverLevel();
        final List<GameTestInfo> tests = names.stream().map(byName::get)
                .map(test -> new GameTestInfo(test, test.rotation(), level, RetryOptions.noRetries())).toList();
        GameTestRunner.clearMarkers(level);
        final GameTestRunner runner = GameTestRunner.Builder.fromInfo(tests, level)
                .newStructureSpawner(new StructureGridSpawner(firstPlace(player), TESTS_PER_ROW, false)).build();
        final MultipleTestTracker tracker = new MultipleTestTracker(runner.getTestInfos());
        tracker.addListener(new Teller(player, thing, tracker));
        player.sendSystemMessage(GameText.component(RUNNING.with(tests.size(), thing.toString())));
        runner.start();
        return tests.size();
    }

    private static void onRun(final RunThingTestsPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                run(player, payload.thing());
            }
        });
    }

    /* On the ground a few blocks ahead of the player, as the game's own /test places its first. */
    private static BlockPos firstPlace(final ServerPlayer player) {
        final BlockPos at = player.blockPosition();
        final int ground = player.serverLevel().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, at).getY();
        return new BlockPos(at.getX(), ground + 1, at.getZ() + AHEAD);
    }

    /** Tells the player how many passed, once the last test is done. */
    private record Teller(ServerPlayer player, ResourceLocation thing, MultipleTestTracker tracker)
            implements GameTestListener {

        @Override
        public void testStructureLoaded(final GameTestInfo test) {
        }

        @Override
        public void testPassed(final GameTestInfo test, final GameTestRunner runner) {
            tellWhenDone();
        }

        @Override
        public void testFailed(final GameTestInfo test, final GameTestRunner runner) {
            tellWhenDone();
        }

        @Override
        public void testAddedForRerun(final GameTestInfo old, final GameTestInfo again, final GameTestRunner runner) {
        }

        private void tellWhenDone() {
            if (this.tracker.isDone()) {
                final int total = this.tracker.getTotalCount();
                final int failed = this.tracker.getFailedRequiredCount() + this.tracker.getFailedOptionalCount();
                this.player.sendSystemMessage(GameText.component(PASSED.with(this.thing.toString(), total - failed,
                        total)));
            }
        }
    }
}
