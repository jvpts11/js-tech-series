/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.jstech.core.audio.media.MediaStore;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestMedia;
import java.io.IOException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The operator's commands for the server's recordings: how much it keeps, and clearing out what nothing has used for
 * a number of days, which leaves whatever was used within them.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MediaCommandGameTests {

    private static final String ARENA = "empty";
    /** An operator of the server's own maintenance level. */
    private static final int OPERATOR = 3;

    private MediaCommandGameTests() {
    }

    @GameTest(template = ARENA)
    public static void media_saysHowManyRecordingsTheServerKeeps(final GameTestHelper helper) {
        final MediaStore store = MediaStore.current().orElseThrow();
        try {
            store.put(TestMedia.wav(300, "counted " + System.nanoTime()), "wav");
        } catch (final IOException unexpected) {
            throw new IllegalStateException(unexpected);
        }
        final int said = run(helper.getLevel().getServer(), "jstech media");
        helper.assertTrue(said == store.size().count() && said > 0,
                "the command answers how many recordings the server keeps; said " + said);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void prune_leavesWhatWasUsedWithinTheDaysGiven(final GameTestHelper helper) {
        final MediaStore store = MediaStore.current().orElseThrow();
        final int before = store.size().count();
        final int pruned = run(helper.getLevel().getServer(), "jstech media prune 36500");
        helper.assertTrue(pruned == 0 && store.size().count() == before,
                "nothing on a server younger than a century is a century unused; took out " + pruned);
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
