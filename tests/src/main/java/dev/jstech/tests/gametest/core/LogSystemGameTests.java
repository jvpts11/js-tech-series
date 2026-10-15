/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.log.LogLevel;
import dev.jstech.core.log.LogSystem;
import dev.jstech.core.log.LogSystems;
import dev.jstech.tests.JsTests;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The log by system, with the game running: the Core's systems are there to be named, turning one up has it write what
 * it kept to itself and leaves the others as they were, and {@code /jstech log} does the same for an administrator.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class LogSystemGameTests {

    private static final String ARENA = "empty";
    /** An operator of the server's own maintenance level. */
    private static final int OPERATOR = 3;

    private LogSystemGameTests() {
    }

    @GameTest(template = ARENA)
    public static void setLevel_turnsOneSystemUpAndLeavesTheOthers(final GameTestHelper helper) {
        final LogSystem media = CoreLogs.MEDIA;
        final LogLevel mediaBefore = LogSystems.level(media);
        final LogLevel guideBefore = LogSystems.level(CoreLogs.GUIDE);
        helper.assertTrue("jscore/media".equals(media.logger().getName()),
                "the recordings write under jscore/media; got " + media.logger().getName());
        try {
            LogSystems.setLevel(media, LogLevel.DEBUG);
            helper.assertTrue(media.logger().isDebugEnabled() && LogSystems.level(media) == LogLevel.DEBUG,
                    "the recordings write their debugging once turned up");
            helper.assertTrue(LogSystems.level(CoreLogs.GUIDE) == guideBefore,
                    "the manuals write as they did; got " + LogSystems.level(CoreLogs.GUIDE));
            LogSystems.setLevel(media, LogLevel.WARN);
            helper.assertTrue(!media.logger().isInfoEnabled() && media.logger().isWarnEnabled(),
                    "turned down to warnings, the recordings keep what is only worth knowing to themselves");
        } finally {
            LogSystems.setLevel(media, mediaBefore);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void command_setsTheLevelOfTheSystemItNames(final GameTestHelper helper) {
        final LogSystem saves = CoreLogs.SAVES;
        final LogLevel before = LogSystems.level(saves);
        final MinecraftServer server = helper.getLevel().getServer();
        try {
            helper.assertTrue(run(server, "jstech log") == LogSystems.all().size(),
                    "the list names every system that writes to the log");
            run(server, "jstech log jscore:saves trace");
            helper.assertTrue(LogSystems.level(saves) == LogLevel.TRACE && saves.logger().isTraceEnabled(),
                    "the saves write every step once the command turns them up; at " + LogSystems.level(saves));
            helper.assertTrue(run(server, "jstech log jscore:saves") == 1,
                    "the command answers for the one system it names");
        } finally {
            LogSystems.setLevel(saves, before);
        }
        helper.succeed();
    }

    private static int run(final MinecraftServer server, final String command) {
        final CommandSourceStack source = server.createCommandSourceStack().withPermission(OPERATOR)
                .withSuppressedOutput();
        try {
            return server.getCommands().getDispatcher().execute(command, source);
        } catch (final CommandSyntaxException wrong) {
            throw new IllegalStateException("the command is registered and parses: " + wrong.getMessage(), wrong);
        }
    }
}
