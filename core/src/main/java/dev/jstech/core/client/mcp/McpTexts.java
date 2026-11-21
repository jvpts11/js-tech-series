/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the MCP server says on the player's screen: its pairing question, its mark, and its command. */
@TextHolder
public final class McpTexts {

    /* The pairing question. */
    public static final TextKey PAIR_TITLE = TextKey.of("gui.jscore.mcp.pair.title",
            "An agent asks to use your game");
    public static final TextKey PAIR_WHO = TextKey.of("gui.jscore.mcp.pair.who",
            "%s %s, on this computer, asks the MCP server of J's Core for a key of its own, good until the game "
                    + "closes.");
    public static final TextKey PAIR_WHO_SERVER = TextKey.of("gui.jscore.mcp.pair.who_server",
            "%s %s, on the server's computer, asks this server's MCP server for a key of its own, good until the "
                    + "server stops.");
    public static final TextKey TO_READ = TextKey.of("gui.jscore.mcp.pair.to_read", "To read");
    public static final TextKey READ_WHAT = TextKey.of("gui.jscore.mcp.pair.read_what",
            "where you are and look, the screens, pictures of the window, the blocks round you, the chat and the "
                    + "logs; what players wrote comes marked as theirs.");
    public static final TextKey TO_ACT = TextKey.of("gui.jscore.mcp.pair.to_act", "To act");
    public static final TextKey ACT_WHAT = TextKey.of("gui.jscore.mcp.pair.act_what",
            "move, click, place and break as you do, run the allowed commands, and write files in your mod's and "
                    + "your pack's folders only.");
    public static final TextKey NOTHING_ELSE = TextKey.of("gui.jscore.mcp.pair.nothing_else",
            "Nothing else on this computer is reached; the studio takes the key back at once.");
    public static final TextKey LET_READ = TextKey.of("gui.jscore.mcp.pair.let_read", "Let it read");
    public static final TextKey LET_ACT = TextKey.of("gui.jscore.mcp.pair.let_act", "Let it read and act");
    public static final TextKey REFUSE = TextKey.of("gui.jscore.mcp.pair.refuse", "Refuse");
    public static final TextKey TIMEOUT = TextKey.of("gui.jscore.mcp.pair.timeout",
            "Refused by itself in %s s with no answer. A client that asks again is asked again.");
    /* The mark on the screen. */
    public static final TextKey MARK = TextKey.of("gui.jscore.mcp.mark", "MCP: %s agents");
    public static final TextKey MARK_ONE = TextKey.of("gui.jscore.mcp.mark_one", "MCP: 1 agent");
    public static final TextKey ACTING = TextKey.of("gui.jscore.mcp.acting", "%s is acting: %s");
    public static final TextKey ACTING_HINT = TextKey.of("gui.jscore.mcp.acting_hint",
            "Any key takes the controls back    %s cuts every agent off");
    /* The command, and what the server says of itself. */
    public static final TextKey ON = TextKey.of("commands.jscore.mcp.on",
            "The MCP server listens at http://127.0.0.1:%s/mcp, on this computer only.");
    public static final TextKey OFF = TextKey.of("commands.jscore.mcp.off",
            "The MCP server stopped, and every key it gave is taken back.");
    public static final TextKey FAILED = TextKey.of("commands.jscore.mcp.failed",
            "The MCP server could not listen at port %s: %s");
    public static final TextKey CUT = TextKey.of("commands.jscore.mcp.cut", "%s agents cut off.");
    public static final TextKey IS_ON = TextKey.of("commands.jscore.mcp.is_on",
            "The MCP server is on at port %s, with %s clients let in.");
    public static final TextKey IS_OFF = TextKey.of("commands.jscore.mcp.is_off", "The MCP server is off.");
    public static final TextKey NEEDS_MODE = TextKey.of("commands.jscore.mcp.needs_mode",
            "Turn developer mode on first: the MCP server is one of its tools.");
    public static final TextKey SENT = TextKey.of("jscore.mcp.sent",
            "Sent to the server as the player's own command; what it answers comes in the chat.");
    public static final TextKey NO_WORLD = TextKey.of("jscore.mcp.no_world", "No world is open.");
    public static final TextKey NO_SCREEN = TextKey.of("jscore.mcp.no_screen", "No screen is open.");
    /* The block used. */
    public static final TextKey NOTHING_OPENED = TextKey.of("jscore.mcp.nothing_opened",
            "Using %s opened no screen.");
    /* The block, its place, how far it is from the eye and how far the player reaches, in blocks. */
    public static final TextKey OUT_OF_REACH = TextKey.of("jscore.mcp.out_of_reach",
            "%s at %s is %s blocks from the eye, past the reach of %s.");
    /* The block and its place. */
    public static final TextKey OFF_ITS_SHAPE = TextKey.of("jscore.mcp.off_its_shape",
            "The point looked at is off the shape of %s at %s, and the look meets nothing: look at another point.");
    /* The block. */
    public static final TextKey KEPT_FROM_BREAKING = TextKey.of("jscore.mcp.kept_from_breaking",
            "The game mode keeps the player from breaking %s here.");
    public static final TextKey UNBREAKABLE = TextKey.of("jscore.mcp.unbreakable", "%s cannot be broken.");
    /* What was looked for, a piece's name or a type of entity, and how far round the player, in blocks. */
    public static final TextKey NONE_NEAR = TextKey.of("jscore.mcp.none_near", "No %s is within %s blocks.");
    /* The piece's name. */
    /* Where the walk stopped, and why, in a word: wall, no_headroom, drop, shut_door, no_way, blocked. */
    public static final TextKey WALK_STOPPED = TextKey.of("jscore.mcp.walk_stopped", "The walk stopped at %s: %s.");
    public static final TextKey OUT_OF_SIGHT = TextKey.of("jscore.mcp.out_of_sight",
            "No point of the nearest %s can be seen from here: something is in the way.");
    public static final TextKey OWN_WORLD_ONLY = TextKey.of("jscore.mcp.own_world_only",
            "Only a world of the player's own, which this game runs, is saved and opened again.");
    /* What the tests were asked by. */
    public static final TextKey NO_TESTS = TextKey.of("jscore.mcp.no_tests",
            "No GameTest's name holds %s, or the GameTests are not on in this run.");
    /* The test. */
    public static final TextKey NO_RECORDED_TEST = TextKey.of("jscore.mcp.no_recorded_test",
            "There is no client test %s kept as a file.");
    public static final TextKey NOT_RECORDING = TextKey.of("jscore.mcp.not_recording",
            "Nothing is being recorded.");

    private McpTexts() {
    }
}
