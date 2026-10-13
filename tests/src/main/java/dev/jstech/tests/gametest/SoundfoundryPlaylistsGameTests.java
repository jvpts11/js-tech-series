/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.audio.SoundfoundryPlaylists;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The text of a playlist: a song's tags never add a line of their own, so they cannot name songs the player did not
 * add.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SoundfoundryPlaylistsGameTests {

    private static final String ARENA = "empty";

    private SoundfoundryPlaylistsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void shownAs_keepsLineBreaksOutOfTheListing(final GameTestHelper helper) {
        final String shown = SoundfoundryPlaylists.shownAs("x\nfoo\r\nbar", "artist");

        helper.assertTrue(shown.indexOf('\n') < 0 && shown.indexOf('\r') < 0, "no line break survives; got " + shown);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void refsOf_readsBackOnlyTheSongsWritten(final GameTestHelper helper) {
        final String content = SoundfoundryPlaylists.contentOf(List.of(new SoundfoundryPlaylists.Line("song:one", 60L,
                SoundfoundryPlaylists.shownAs("x\nsong:injected", ""))));

        helper.assertTrue(SoundfoundryPlaylists.refsOf(content).equals(List.of("song:one")),
                "a title with a line break adds no song; got " + SoundfoundryPlaylists.refsOf(content));
        helper.succeed();
    }
}
