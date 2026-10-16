/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.jstech.core.command.SeriesCommands;
import dev.jstech.core.world.event.WorldEventBalance;
import dev.jstech.core.world.event.WorldEventStage;
import dev.jstech.core.world.event.WorldEvents;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestWorldChanges;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The world's events: one planned is warned of, begins and ends, each step heard on the game's bus, one of a kind at a
 * time; one round a place covers only so far; an operator lists, starts and stops them; and the world's settings say
 * how often each happens and which never do.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class WorldEventGameTests {

    private static final String ARENA = "empty";
    private static final String STORM = "jstests:test_storm";
    private static final String GALE = "jstests:test_gale";

    private WorldEventGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void event_isWarnedOfBeginsAndEnds(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final List<String> heard = new CopyOnWriteArrayList<>();
        final Consumer<WorldEventStage.Warned> warned = stage -> hear(heard, stage, "warned");
        final AtomicBoolean consequences = new AtomicBoolean();
        final Consumer<WorldEventStage.Started> started = stage -> {
            hear(heard, stage, "started");
            consequences.set(stage.consequences());
        };
        final Consumer<WorldEventStage.Ended> ended = stage -> hear(heard, stage, "ended");
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, WorldEventStage.Warned.class, warned);
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, WorldEventStage.Started.class, started);
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, WorldEventStage.Ended.class, ended);
        WorldEvents.stop(level, TestWorldChanges.STORM);
        helper.assertTrue(WorldEvents.plan(level, TestWorldChanges.STORM, 40, Optional.empty()),
                "the storm is planned");
        helper.assertTrue(!WorldEvents.plan(level, TestWorldChanges.STORM, 40, Optional.empty()),
                "one of a kind at a time");
        helper.assertTrue(WorldEvents.planned(level).stream().anyMatch(event -> event.type()
                .equals(TestWorldChanges.STORM)) && !WorldEvents.activeAt(level, TestWorldChanges.STORM,
                BlockPos.ZERO), "planned, not under way yet");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(heard.contains("started"), "the storm has not begun"))
                .thenExecute(() -> {
                    helper.assertTrue(heard.indexOf("warned") == 0 && heard.indexOf("started") == 1,
                            "warned of first, then begun: " + heard);
                    helper.assertTrue(WorldEvents.activeAt(level, TestWorldChanges.STORM, BlockPos.ZERO.east(5000)),
                            "a storm over the whole dimension is under way everywhere in it");
                    helper.assertTrue(consequences.get(), "and may change the world, as the settings allow");
                    helper.assertTrue(WorldEvents.stop(level, TestWorldChanges.STORM), "it is stopped");
                    helper.assertTrue(heard.contains("ended") && WorldEvents.planned(level).stream()
                            .noneMatch(event -> event.type().equals(TestWorldChanges.STORM)), "and ends");
                    helper.assertTrue(!WorldEvents.stop(level, TestWorldChanges.STORM), "once");
                    NeoForge.EVENT_BUS.unregister(warned);
                    NeoForge.EVENT_BUS.unregister(started);
                    NeoForge.EVENT_BUS.unregister(ended);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void localEvent_coversOnlySoFarRoundItsPlace(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        WorldEvents.stop(level, TestWorldChanges.QUAKE);
        final BlockPos centre = helper.absolutePos(BlockPos.ZERO);
        try {
            helper.assertTrue(!WorldEvents.plan(level, TestWorldChanges.QUAKE, 0, Optional.empty())
                    || !level.players().isEmpty(), "a quake with no place needs a player to happen round");
            WorldEvents.stop(level, TestWorldChanges.QUAKE);
            helper.assertTrue(WorldEvents.start(level, TestWorldChanges.QUAKE, 0, Optional.of(centre)),
                    "a quake starts round a place");
            helper.assertTrue(WorldEvents.activeAt(level, TestWorldChanges.QUAKE, centre.east(
                    TestWorldChanges.QUAKE_RADIUS - 2)), "it covers what is near");
            helper.assertTrue(!WorldEvents.activeAt(level, TestWorldChanges.QUAKE, centre.east(
                    TestWorldChanges.QUAKE_RADIUS + 2)), "and nothing beyond its reach");
        } finally {
            WorldEvents.stop(level, TestWorldChanges.QUAKE);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void commands_listStartAndStopEvents(final GameTestHelper helper) throws CommandSyntaxException {
        final ServerLevel level = helper.getLevel();
        WorldEvents.stop(level, TestWorldChanges.GALE);
        try {
            helper.assertTrue(run(helper, "jstech event start " + GALE + " 100") == 1, "an operator starts it");
            helper.assertTrue(WorldEvents.activeAt(level, TestWorldChanges.GALE, BlockPos.ZERO), "and it is on");
            helper.assertTrue(run(helper, "jstech event list") >= 1, "the list shows it");
            helper.assertTrue(run(helper, "jstech event stop " + GALE) == 1, "an operator stops it");
            helper.assertTrue(fails(helper, "jstech event stop " + GALE), "and cannot stop it twice");
            helper.assertTrue(fails(helper, "jstech event start jstests:nothing"), "nor start what is not declared");
            helper.assertTrue(run(helper, "jstech event start " + GALE) == 1
                    && fails(helper, "jstech event delay " + GALE + " 100"), "a gale under way cannot be put off");
            WorldEvents.stop(level, TestWorldChanges.GALE);
            helper.assertTrue(run(helper, "jstech event plan " + GALE + " 2400") == 1, "and plans one ahead");
            final long before = WorldEvents.planned(level).stream().filter(event -> event.type()
                    .equals(TestWorldChanges.GALE)).findFirst().orElseThrow().startsAt();
            helper.assertTrue(run(helper, "jstech event delay " + GALE + " 1200") == 1
                    && WorldEvents.planned(level).stream().anyMatch(event -> event.type()
                    .equals(TestWorldChanges.GALE) && event.startsAt() == before + 1200), "and puts it off");
        } finally {
            WorldEvents.stop(level, TestWorldChanges.GALE);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void settings_sayHowOftenAndWhichNever(final GameTestHelper helper) {
        final boolean enabled = WorldEventBalance.enabled();
        try {
            WorldEventBalance.setFrequency(Map.of(STORM, 0.5, "not an id!", 3.0));
            WorldEventBalance.setDisabled(List.of(STORM));
            WorldEventBalance.setHarmless(List.of(GALE));
            helper.assertTrue(WorldEventBalance.harmless(TestWorldChanges.GALE)
                    && !WorldEventBalance.harmless(TestWorldChanges.STORM), "a named event leaves the world alone");
            helper.assertTrue(WorldEventBalance.frequency(TestWorldChanges.STORM) == 0.5
                    && WorldEventBalance.frequency(TestWorldChanges.QUAKE) == 1.0,
                    "a named event happens as often as said, the rest as often as their own");
            helper.assertTrue(WorldEventBalance.disabled(TestWorldChanges.STORM)
                    && !WorldEventBalance.disabled(TestWorldChanges.QUAKE), "and a named one never by itself");
            WorldEventBalance.setEnabled(false);
            helper.assertTrue(!WorldEventBalance.enabled(), "events can be switched off");
        } finally {
            WorldEventBalance.setFrequency(Map.of());
            WorldEventBalance.setDisabled(List.of());
            WorldEventBalance.setHarmless(List.of());
            WorldEventBalance.setEnabled(enabled);
        }
        helper.assertTrue(WorldEvents.all(helper.getLevel()).containsKey(ResourceLocation.parse(STORM)),
                "the test storm is declared");
        helper.assertTrue(WorldEvents.clock(940).equals("0:47") && WorldEvents.clock(20 * 3725).equals("1:02:05")
                && WorldEvents.clock(3).equals("0:01") && WorldEvents.clock(-5).equals("0:00"),
                "a wait reads as a clock shows it, a part of a second counted whole");
        helper.succeed();
    }

    private static void hear(final List<String> heard, final WorldEventStage stage, final String what) {
        if (stage.event().type().equals(TestWorldChanges.STORM)) {
            heard.add(what);
        }
    }

    private static int run(final GameTestHelper helper, final String command) throws CommandSyntaxException {
        final CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack()
                .withPermission(SeriesCommands.GAME_MASTERS).withSuppressedOutput();
        return helper.getLevel().getServer().getCommands().getDispatcher().execute(command, source);
    }

    private static boolean fails(final GameTestHelper helper, final String command) {
        try {
            run(helper, command);
            return false;
        } catch (final CommandSyntaxException refused) {
            return true;
        }
    }
}
